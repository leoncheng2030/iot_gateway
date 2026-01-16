package vip.xiaonuo.iot.core.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 异常检测器
 * 支持多种异常检测算法
 * 
 * @author wqs
 * @date 2026/01/15
 */
@Slf4j
@Component
public class AnomalyDetector {
    
    @Resource
    private ApplicationEventPublisher applicationEventPublisher;
    
    /** 历史数据缓存 <deviceId:propertyKey, List<Double>> */
    private final Map<String, LinkedList<Double>> historyCache = new ConcurrentHashMap<>();
    
    /** 最大历史数据点数量 */
    private static final int MAX_HISTORY_SIZE = 1000;
    
    /** 最小数据点数量（用于统计计算） */
    private static final int MIN_DATA_POINTS = 10;
    
    /** 默认使用3σ原则 */
    private static final double DEFAULT_SIGMA = 3.0;
    
    /**
     * 检测异常并记录历史数据
     * 
     * @param deviceId 设备ID
     * @param propertyKey 属性键
     * @param currentValue 当前值
     * @return 异常检测结果
     */
    public AnomalyResult detect(String deviceId, String propertyKey, double currentValue) {
        String key = buildKey(deviceId, propertyKey);
        
        // 获取或创建历史数据列表
        LinkedList<Double> history = historyCache.computeIfAbsent(key, k -> new LinkedList<>());
        
        // 执行异常检测
        AnomalyResult result = detectAnomaly(history, currentValue);
        result.setDeviceId(deviceId);
        result.setPropertyKey(propertyKey);
        result.setValue(currentValue);
        result.setTimestamp(System.currentTimeMillis());
        
        // 添加当前值到历史记录
        history.addLast(currentValue);
        
        // 限制历史数据大小
        while (history.size() > MAX_HISTORY_SIZE) {
            history.removeFirst();
        }
        
        // 如果检测到异常，发布事件
        if (result.isAnomaly()) {
            publishAnomalyEvent(result);
        }
        
        return result;
    }
    
    /**
     * 基于3σ原则检测异常
     * 
     * @param values 历史数据点
     * @param currentValue 当前值
     * @return 异常检测结果
     */
    private AnomalyResult detectAnomaly(List<Double> values, double currentValue) {
        if (values.size() < MIN_DATA_POINTS) {
            return AnomalyResult.builder()
                .anomaly(false)
                .reason("数据点不足，需要至少" + MIN_DATA_POINTS + "个历史数据点")
                .build();
        }
        
        // 计算均值
        double mean = values.stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0);
        
        // 计算标准差
        double variance = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average()
            .orElse(0);
        double stdDev = Math.sqrt(variance);
        
        // 3σ原则：超出3倍标准差为异常
        double lowerBound = mean - DEFAULT_SIGMA * stdDev;
        double upperBound = mean + DEFAULT_SIGMA * stdDev;
        
        boolean isAnomaly = currentValue < lowerBound || currentValue > upperBound;
        
        AnomalyResult.AnomalyResultBuilder builder = AnomalyResult.builder()
            .anomaly(isAnomaly)
            .mean(mean)
            .stdDev(stdDev)
            .lowerBound(lowerBound)
            .upperBound(upperBound)
            .algorithm("3-SIGMA");
        
        if (isAnomaly) {
            String direction = currentValue < lowerBound ? "低于下界" : "高于上界";
            builder.reason(String.format("当前值%.2f%s(边界: [%.2f, %.2f])", 
                currentValue, direction, lowerBound, upperBound));
            
            log.warn("检测到异常值: value={}, mean={}, stdDev={}, bounds=[{}, {}]",
                String.format("%.2f", currentValue),
                String.format("%.2f", mean),
                String.format("%.2f", stdDev),
                String.format("%.2f", lowerBound),
                String.format("%.2f", upperBound));
        }
        
        return builder.build();
    }
    
    /**
     * 基于IQR的异常检测（更鲁棒，抗异常值干扰）
     * 
     * @param deviceId 设备ID
     * @param propertyKey 属性键
     * @param currentValue 当前值
     * @return 异常检测结果
     */
    public AnomalyResult detectByIQR(String deviceId, String propertyKey, double currentValue) {
        String key = buildKey(deviceId, propertyKey);
        LinkedList<Double> history = historyCache.get(key);
        
        if (history == null || history.size() < MIN_DATA_POINTS) {
            return AnomalyResult.builder()
                .anomaly(false)
                .deviceId(deviceId)
                .propertyKey(propertyKey)
                .value(currentValue)
                .timestamp(System.currentTimeMillis())
                .reason("数据点不足")
                .build();
        }
        
        List<Double> sorted = history.stream().sorted().toList();
        int size = sorted.size();
        
        // 计算四分位数
        double q1 = sorted.get(size / 4);
        double q3 = sorted.get(size * 3 / 4);
        double iqr = q3 - q1;
        
        // IQR异常边界
        double lowerBound = q1 - 1.5 * iqr;
        double upperBound = q3 + 1.5 * iqr;
        
        boolean isAnomaly = currentValue < lowerBound || currentValue > upperBound;
        
        AnomalyResult result = AnomalyResult.builder()
            .anomaly(isAnomaly)
            .deviceId(deviceId)
            .propertyKey(propertyKey)
            .value(currentValue)
            .timestamp(System.currentTimeMillis())
            .lowerBound(lowerBound)
            .upperBound(upperBound)
            .algorithm("IQR")
            .reason(isAnomaly ? String.format("IQR异常: 值%.2f超出边界[%.2f, %.2f]", 
                currentValue, lowerBound, upperBound) : null)
            .build();
        
        if (isAnomaly) {
            publishAnomalyEvent(result);
        }
        
        return result;
    }
    
    /**
     * Z-Score异常检测
     * 
     * @param deviceId 设备ID
     * @param propertyKey 属性键
     * @param currentValue 当前值
     * @param threshold Z-Score阈值（默认3）
     * @return 异常检测结果
     */
    public AnomalyResult detectByZScore(String deviceId, String propertyKey, 
                                        double currentValue, double threshold) {
        String key = buildKey(deviceId, propertyKey);
        LinkedList<Double> history = historyCache.get(key);
        
        if (history == null || history.size() < MIN_DATA_POINTS) {
            return AnomalyResult.builder()
                .anomaly(false)
                .deviceId(deviceId)
                .propertyKey(propertyKey)
                .value(currentValue)
                .timestamp(System.currentTimeMillis())
                .reason("数据点不足")
                .build();
        }
        
        double mean = history.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = history.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average()
            .orElse(0);
        double stdDev = Math.sqrt(variance);
        
        // 防止除以零
        if (stdDev == 0) {
            return AnomalyResult.builder()
                .anomaly(currentValue != mean)
                .deviceId(deviceId)
                .propertyKey(propertyKey)
                .value(currentValue)
                .timestamp(System.currentTimeMillis())
                .algorithm("Z-SCORE")
                .reason("标准差为0")
                .build();
        }
        
        double zScore = Math.abs((currentValue - mean) / stdDev);
        boolean isAnomaly = zScore > threshold;
        
        return AnomalyResult.builder()
            .anomaly(isAnomaly)
            .deviceId(deviceId)
            .propertyKey(propertyKey)
            .value(currentValue)
            .timestamp(System.currentTimeMillis())
            .mean(mean)
            .stdDev(stdDev)
            .algorithm("Z-SCORE")
            .reason(isAnomaly ? String.format("Z-Score=%.2f 超过阈值%.2f", zScore, threshold) : null)
            .build();
    }
    
    /**
     * 发布异常事件
     */
    private void publishAnomalyEvent(AnomalyResult result) {
        try {
            applicationEventPublisher.publishEvent(new AnomalyEvent(this, result));
        } catch (Exception e) {
            log.warn("发布异常事件失败: {}", e.getMessage());
        }
    }
    
    /**
     * 清理设备历史数据
     */
    public void clearHistory(String deviceId) {
        historyCache.entrySet().removeIf(entry -> entry.getKey().startsWith(deviceId + ":"));
        log.debug("清理设备历史数据: {}", deviceId);
    }
    
    /**
     * 清理所有历史数据
     */
    public void clearAllHistory() {
        historyCache.clear();
        log.info("清理所有历史数据");
    }
    
    /**
     * 获取设备属性的历史数据统计
     */
    public Optional<HistoryStats> getHistoryStats(String deviceId, String propertyKey) {
        String key = buildKey(deviceId, propertyKey);
        LinkedList<Double> history = historyCache.get(key);
        
        if (history == null || history.isEmpty()) {
            return Optional.empty();
        }
        
        DoubleSummaryStatistics stats = history.stream()
            .mapToDouble(Double::doubleValue)
            .summaryStatistics();
        
        return Optional.of(new HistoryStats(
            stats.getCount(),
            stats.getAverage(),
            stats.getMin(),
            stats.getMax(),
            stats.getSum()
        ));
    }
    
    private String buildKey(String deviceId, String propertyKey) {
        return deviceId + ":" + propertyKey;
    }
    
    /**
     * 异常检测结果
     */
    @Data
    @AllArgsConstructor
    @lombok.Builder
    public static class AnomalyResult {
        /** 是否异常 */
        private boolean anomaly;
        /** 设备ID */
        private String deviceId;
        /** 属性键 */
        private String propertyKey;
        /** 当前值 */
        private double value;
        /** 时间戳 */
        private long timestamp;
        /** 均值 */
        private double mean;
        /** 标准差 */
        private double stdDev;
        /** 下界 */
        private double lowerBound;
        /** 上界 */
        private double upperBound;
        /** 使用的算法 */
        private String algorithm;
        /** 异常原因 */
        private String reason;
    }
    
    /**
     * 历史数据统计
     */
    @Data
    @AllArgsConstructor
    public static class HistoryStats {
        private long count;
        private double average;
        private double min;
        private double max;
        private double sum;
    }
}
