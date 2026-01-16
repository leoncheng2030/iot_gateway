package vip.xiaonuo.iot.core.storage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;

/**
 * 时序数据分层存储服务
 * 
 * 架构设计：
 * - 热数据（最近24小时）：内存存储（高性能）
 * - 温数据（7天内）：内存存储（可扩展为文件存储）
 * - 冷数据（>7天）：MySQL（归档，低成本存储）
 * 
 * @author wqs
 * @date 2026/01/15
 */
@Slf4j
@Service
public class TimeSeriesStorageService {
    
    @Value("${iot.storage.enabled:true}")
    private boolean enabled;
    
    @Value("${iot.storage.data-dir:data}")
    private String dataDir;
    
    @Value("${iot.storage.hot-data-entries:100000}")
    private long hotDataEntries;
    
    @Value("${iot.storage.hot-threshold-hours:24}")
    private int hotThresholdHours;
    
    @Value("${iot.storage.warm-threshold-days:7}")
    private int warmThresholdDays;
    
    /** 热数据存储（内存） */
    private ConcurrentMap<String, byte[]> hotDataMap;
    
    /** 温数据存储（内存） */
    private ConcurrentMap<String, byte[]> warmDataMap;
    
    /** 定时任务调度器 */
    private ScheduledExecutorService scheduler;
    
    /** 写入缓冲队列 */
    private final BlockingQueue<DataPointWrapper> writeBuffer = new LinkedBlockingQueue<>(100000);
    
    /** 批量写入线程 */
    private ExecutorService writeExecutor;
    
    /** 运行状态 */
    private volatile boolean running = false;
    
    @PostConstruct
    public void init() {
        if (!enabled) {
            log.info("时序数据分层存储服务已禁用");
            // 即使禁用也初始化空Map，避免NPE
            hotDataMap = new ConcurrentHashMap<>();
            warmDataMap = new ConcurrentHashMap<>();
            return;
        }
        
        log.info("初始化时序数据分层存储服务...");
        
        try {
            // 确保数据目录存在
            File dataDirectory = new File(dataDir);
            if (!dataDirectory.exists()) {
                dataDirectory.mkdirs();
            }
            
            // 使用ConcurrentHashMap作为存储
            hotDataMap = new ConcurrentHashMap<>((int) Math.min(hotDataEntries, 100000));
            warmDataMap = new ConcurrentHashMap<>();
            
            // 启动调度器
            scheduler = Executors.newScheduledThreadPool(1, r -> {
                Thread thread = new Thread(r, "ts-storage-scheduler");
                thread.setDaemon(true);
                return thread;
            });
            
            // 启动写入线程
            writeExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r, "ts-storage-writer");
                thread.setDaemon(true);
                return thread;
            });
            
            running = true;
            
            // 启动批量写入任务
            writeExecutor.submit(this::batchWriteTask);
            
            // 启动数据分层任务（每10分钟执行一次）
            scheduler.scheduleAtFixedRate(this::tieringData, 10, 10, TimeUnit.MINUTES);
            
            log.info("时序数据存储服务初始化完成: dataDir={}", dataDir);
            
        } catch (Exception e) {
            log.error("时序数据存储服务初始化失败，使用降级模式", e);
            // 确保降级模式可用
            if (hotDataMap == null) {
                hotDataMap = new ConcurrentHashMap<>();
            }
            if (warmDataMap == null) {
                warmDataMap = new ConcurrentHashMap<>();
            }
        }
    }
    
    /**
     * 写入数据点（异步批量写入）
     */
    public void write(String deviceId, String propertyKey, double value, long timestamp) {
        if (!enabled || hotDataMap == null) {
            return;
        }
        
        DataPointWrapper wrapper = new DataPointWrapper(deviceId, propertyKey, value, timestamp);
        
        if (!writeBuffer.offer(wrapper)) {
            log.warn("写入缓冲队列已满，丢弃数据点: deviceId={}, propertyKey={}", deviceId, propertyKey);
        }
    }
    
    /**
     * 同步写入数据点（直接写入，用于关键数据）
     */
    public void writeSync(String deviceId, String propertyKey, double value, long timestamp) {
        if (!enabled || hotDataMap == null) {
            return;
        }
        
        String key = buildKey(deviceId, propertyKey, timestamp);
        byte[] data = serializeDataPoint(new DeviceDataPoint(deviceId, propertyKey, value, timestamp));
        hotDataMap.put(key, data);
    }
    
    /**
     * 批量写入任务
     */
    private void batchWriteTask() {
        List<DataPointWrapper> batch = new ArrayList<>(1000);
        
        while (running) {
            try {
                // 从队列获取数据点
                DataPointWrapper wrapper = writeBuffer.poll(100, TimeUnit.MILLISECONDS);
                
                if (wrapper != null) {
                    batch.add(wrapper);
                    
                    // 批量获取更多数据点
                    writeBuffer.drainTo(batch, 999);
                    
                    // 批量写入
                    for (DataPointWrapper dp : batch) {
                        String key = buildKey(dp.deviceId, dp.propertyKey, dp.timestamp);
                        byte[] data = serializeDataPoint(
                            new DeviceDataPoint(dp.deviceId, dp.propertyKey, dp.value, dp.timestamp)
                        );
                        hotDataMap.put(key, data);
                    }
                    
                    batch.clear();
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("批量写入失败", e);
                batch.clear();
            }
        }
    }
    
    /**
     * 查询数据（自动选择存储层）
     */
    public List<DeviceDataPoint> query(String deviceId, String propertyKey, long startTime, long endTime) {
        List<DeviceDataPoint> result = new ArrayList<>();
        
        if (!enabled || hotDataMap == null) {
            return result;
        }
        
        long now = System.currentTimeMillis();
        long hotThreshold = now - hotThresholdHours * 3600 * 1000L;
        long warmThreshold = now - warmThresholdDays * 24 * 3600 * 1000L;
        
        // 1. 查询热数据层
        if (endTime >= hotThreshold) {
            result.addAll(queryDataMap(hotDataMap, deviceId, propertyKey, 
                Math.max(startTime, hotThreshold), endTime));
        }
        
        // 2. 查询温数据层
        if (startTime < hotThreshold && endTime >= warmThreshold) {
            result.addAll(queryDataMap(warmDataMap, deviceId, propertyKey,
                Math.max(startTime, warmThreshold), Math.min(endTime, hotThreshold)));
        }
        
        // 按时间排序
        result.sort(Comparator.comparingLong(DeviceDataPoint::getTimestamp));
        
        return result;
    }
    
    /**
     * 查询数据Map
     */
    private List<DeviceDataPoint> queryDataMap(ConcurrentMap<String, byte[]> dataMap,
                                               String deviceId, String propertyKey, 
                                               long startTime, long endTime) {
        List<DeviceDataPoint> result = new ArrayList<>();
        String keyPrefix = deviceId + ":" + propertyKey + ":";
        
        dataMap.forEach((key, data) -> {
            if (key.startsWith(keyPrefix)) {
                DeviceDataPoint dp = deserializeDataPoint(data);
                if (dp != null && dp.getTimestamp() >= startTime && dp.getTimestamp() <= endTime) {
                    result.add(dp);
                }
            }
        });
        
        return result;
    }
    
    /**
     * 获取设备最新数据点
     */
    public Optional<DeviceDataPoint> getLatest(String deviceId, String propertyKey) {
        if (!enabled || hotDataMap == null) {
            return Optional.empty();
        }
        
        String keyPrefix = deviceId + ":" + propertyKey + ":";
        DeviceDataPoint latest = null;
        
        for (Map.Entry<String, byte[]> entry : hotDataMap.entrySet()) {
            if (entry.getKey().startsWith(keyPrefix)) {
                DeviceDataPoint dp = deserializeDataPoint(entry.getValue());
                if (dp != null && (latest == null || dp.getTimestamp() > latest.getTimestamp())) {
                    latest = dp;
                }
            }
        }
        
        return Optional.ofNullable(latest);
    }
    
    /**
     * 数据分层任务（热 -> 温）
     */
    private void tieringData() {
        if (!enabled || hotDataMap == null || warmDataMap == null) {
            return;
        }
        
        try {
            long now = System.currentTimeMillis();
            long hotThreshold = now - hotThresholdHours * 3600 * 1000L;
            
            List<String> keysToMove = new ArrayList<>();
            
            // 找出需要移动的热数据
            hotDataMap.forEach((key, data) -> {
                DeviceDataPoint dp = deserializeDataPoint(data);
                if (dp != null && dp.getTimestamp() < hotThreshold) {
                    keysToMove.add(key);
                }
            });
            
            if (keysToMove.isEmpty()) {
                return;
            }
            
            // 移动到温数据层
            int moved = 0;
            for (String key : keysToMove) {
                byte[] data = hotDataMap.remove(key);
                if (data != null) {
                    warmDataMap.put(key, data);
                    moved++;
                }
            }
            
            if (moved > 0) {
                log.debug("数据分层完成：热->温 {}条", moved);
            }
            
        } catch (Exception e) {
            log.error("数据分层失败", e);
        }
    }
    
    /**
     * 清理过期温数据
     */
    public void cleanupWarmData() {
        if (!enabled || warmDataMap == null) {
            return;
        }
        
        try {
            long now = System.currentTimeMillis();
            long warmThreshold = now - warmThresholdDays * 24 * 3600 * 1000L;
            
            List<String> keysToRemove = new ArrayList<>();
            
            warmDataMap.forEach((key, data) -> {
                DeviceDataPoint dp = deserializeDataPoint(data);
                if (dp != null && dp.getTimestamp() < warmThreshold) {
                    keysToRemove.add(key);
                }
            });
            
            if (!keysToRemove.isEmpty()) {
                keysToRemove.forEach(warmDataMap::remove);
                log.info("清理过期温数据: {}条", keysToRemove.size());
            }
            
        } catch (Exception e) {
            log.error("清理温数据失败", e);
        }
    }
    
    /**
     * 获取存储统计信息
     */
    public StorageStats getStats() {
        return new StorageStats(
            hotDataMap != null ? hotDataMap.size() : 0,
            warmDataMap != null ? warmDataMap.size() : 0,
            writeBuffer.size(),
            hotThresholdHours,
            warmThresholdDays
        );
    }
    
    /**
     * 构建存储Key
     */
    private String buildKey(String deviceId, String propertyKey, long timestamp) {
        return deviceId + ":" + propertyKey + ":" + timestamp;
    }
    
    /**
     * 序列化数据点
     */
    private byte[] serializeDataPoint(DeviceDataPoint dp) {
        String str = dp.getDeviceId() + "|" + dp.getPropertyKey() + "|" + dp.getValue() + "|" + dp.getTimestamp();
        return str.getBytes();
    }
    
    /**
     * 反序列化数据点
     */
    private DeviceDataPoint deserializeDataPoint(byte[] data) {
        try {
            String str = new String(data);
            String[] parts = str.split("\\|");
            if (parts.length == 4) {
                return new DeviceDataPoint(
                    parts[0],
                    parts[1],
                    Double.parseDouble(parts[2]),
                    Long.parseLong(parts[3])
                );
            }
        } catch (Exception e) {
            log.warn("反序列化数据点失败", e);
        }
        return null;
    }
    
    @PreDestroy
    public void shutdown() {
        if (!enabled) {
            return;
        }
        
        log.info("关闭时序数据存储服务...");
        
        running = false;
        
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                scheduler.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }
        
        if (writeExecutor != null) {
            writeExecutor.shutdown();
            try {
                writeExecutor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                writeExecutor.shutdownNow();
            }
        }
        
        if (hotDataMap != null) {
            hotDataMap.clear();
        }
        
        if (warmDataMap != null) {
            warmDataMap.clear();
        }
        
        log.info("时序数据存储服务已关闭");
    }
    
    /**
     * 数据点实体
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DeviceDataPoint implements Serializable {
        private String deviceId;
        private String propertyKey;
        private double value;
        private long timestamp;
    }
    
    /**
     * 数据点包装器（用于批量写入）
     */
    @AllArgsConstructor
    private static class DataPointWrapper {
        String deviceId;
        String propertyKey;
        double value;
        long timestamp;
    }
    
    /**
     * 存储统计信息
     */
    @Data
    @AllArgsConstructor
    public static class StorageStats {
        private long hotDataCount;
        private long warmDataCount;
        private int writeBufferSize;
        private int hotThresholdHours;
        private int warmThresholdDays;
    }
}
