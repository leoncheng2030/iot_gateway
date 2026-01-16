package vip.xiaonuo.iot.core.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import java.util.*;
import java.util.concurrent.*;

/**
 * 数据窗口聚合器
 * 支持滑动窗口和滚动窗口统计
 * 
 * @author wqs
 * @date 2026/01/15
 */
@Slf4j
@Component
public class WindowAggregator {
    
    @Resource
    private ApplicationEventPublisher applicationEventPublisher;
    
    /** 设备数据窗口缓存 <deviceId, <propertyKey, WindowData>> */
    private final Map<String, Map<String, WindowData>> windowCache = new ConcurrentHashMap<>();
    
    /** 定时调度器 */
    private ScheduledExecutorService scheduler;
    
    /** 窗口大小（秒） */
    private static final int WINDOW_SIZE = 60;
    
    /** 计算间隔（秒） */
    private static final int CALCULATE_INTERVAL = 10;
    
    @PostConstruct
    public void init() {
        scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread thread = new Thread(r, "window-aggregator");
            thread.setDaemon(true);
            return thread;
        });
        
        // 每10秒计算一次窗口统计
        scheduler.scheduleAtFixedRate(
            this::calculateWindows, 
            CALCULATE_INTERVAL, 
            CALCULATE_INTERVAL, 
            TimeUnit.SECONDS
        );
        
        log.info("窗口聚合器初始化完成，窗口大小: {}秒，计算间隔: {}秒", WINDOW_SIZE, CALCULATE_INTERVAL);
    }
    
    /**
     * 添加数据点
     * 
     * @param deviceId 设备ID
     * @param propertyKey 属性键
     * @param value 属性值
     * @param timestamp 时间戳
     */
    public void addDataPoint(String deviceId, String propertyKey, double value, long timestamp) {
        windowCache.computeIfAbsent(deviceId, k -> new ConcurrentHashMap<>())
                   .computeIfAbsent(propertyKey, k -> new WindowData())
                   .addPoint(value, timestamp);
    }
    
    /**
     * 获取设备属性的当前窗口统计
     * 
     * @param deviceId 设备ID
     * @param propertyKey 属性键
     * @return 窗口统计结果
     */
    public Optional<WindowStats> getCurrentStats(String deviceId, String propertyKey) {
        Map<String, WindowData> properties = windowCache.get(deviceId);
        if (properties == null) {
            return Optional.empty();
        }
        
        WindowData windowData = properties.get(propertyKey);
        if (windowData == null) {
            return Optional.empty();
        }
        
        long now = System.currentTimeMillis();
        long windowStart = now - WINDOW_SIZE * 1000L;
        
        return Optional.of(windowData.calculateStats(windowStart, now));
    }
    
    /**
     * 计算所有窗口统计
     */
    private void calculateWindows() {
        try {
            long now = System.currentTimeMillis();
            long windowStart = now - WINDOW_SIZE * 1000L;
            
            windowCache.forEach((deviceId, properties) -> {
                properties.forEach((propertyKey, windowData) -> {
                    WindowStats stats = windowData.calculateStats(windowStart, now);
                    
                    if (stats.getCount() > 0) {
                        log.debug("设备[{}]属性[{}]窗口统计: avg={}, max={}, min={}, count={}", 
                            deviceId, propertyKey, 
                            String.format("%.2f", stats.getAvg()),
                            String.format("%.2f", stats.getMax()),
                            String.format("%.2f", stats.getMin()),
                            stats.getCount());
                        
                        // 发布统计结果事件
                        publishWindowStats(deviceId, propertyKey, stats);
                    }
                    
                    // 清理过期数据
                    windowData.cleanup(windowStart);
                });
            });
        } catch (Exception e) {
            log.error("计算窗口统计失败", e);
        }
    }
    
    /**
     * 发布窗口统计事件
     */
    private void publishWindowStats(String deviceId, String propertyKey, WindowStats stats) {
        try {
            WindowStatsEvent event = new WindowStatsEvent(this, deviceId, propertyKey, stats);
            applicationEventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.warn("发布窗口统计事件失败: {}", e.getMessage());
        }
    }
    
    /**
     * 清理设备数据
     * 
     * @param deviceId 设备ID
     */
    public void clearDevice(String deviceId) {
        windowCache.remove(deviceId);
        log.debug("清理设备窗口数据: {}", deviceId);
    }
    
    /**
     * 获取缓存统计信息
     */
    public CacheStats getCacheStats() {
        int deviceCount = windowCache.size();
        int propertyCount = windowCache.values().stream()
            .mapToInt(Map::size)
            .sum();
        int dataPointCount = windowCache.values().stream()
            .flatMap(m -> m.values().stream())
            .mapToInt(WindowData::size)
            .sum();
        
        return new CacheStats(deviceCount, propertyCount, dataPointCount);
    }
    
    @PreDestroy
    public void shutdown() {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        windowCache.clear();
        log.info("窗口聚合器已关闭");
    }
    
    /**
     * 窗口数据
     */
    private static class WindowData {
        private final List<DataPoint> points = new CopyOnWriteArrayList<>();
        
        void addPoint(double value, long timestamp) {
            points.add(new DataPoint(value, timestamp));
        }
        
        WindowStats calculateStats(long startTime, long endTime) {
            List<Double> values = points.stream()
                .filter(p -> p.timestamp >= startTime && p.timestamp <= endTime)
                .map(p -> p.value)
                .toList();
            
            if (values.isEmpty()) {
                return new WindowStats(0, 0, 0, 0, 0);
            }
            
            double sum = values.stream().mapToDouble(Double::doubleValue).sum();
            double avg = sum / values.size();
            double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(0);
            double min = values.stream().mapToDouble(Double::doubleValue).min().orElse(0);
            
            return new WindowStats(avg, max, min, sum, values.size());
        }
        
        void cleanup(long beforeTime) {
            points.removeIf(p -> p.timestamp < beforeTime);
        }
        
        int size() {
            return points.size();
        }
    }
    
    /**
     * 数据点
     */
    private record DataPoint(double value, long timestamp) {}
    
    /**
     * 窗口统计结果
     */
    @Data
    @AllArgsConstructor
    public static class WindowStats {
        /** 平均值 */
        private final double avg;
        /** 最大值 */
        private final double max;
        /** 最小值 */
        private final double min;
        /** 总和 */
        private final double sum;
        /** 数据点数量 */
        private final long count;
    }
    
    /**
     * 缓存统计信息
     */
    @Data
    @AllArgsConstructor
    public static class CacheStats {
        /** 设备数量 */
        private final int deviceCount;
        /** 属性数量 */
        private final int propertyCount;
        /** 数据点数量 */
        private final int dataPointCount;
    }
}
