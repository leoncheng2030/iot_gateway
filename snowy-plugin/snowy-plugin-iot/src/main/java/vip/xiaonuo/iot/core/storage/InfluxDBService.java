package vip.xiaonuo.iot.core.storage;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vip.xiaonuo.iot.modular.device.entity.IotDevice;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * InfluxDB数据存储服务(批量写入优化)
 *
 * @author jetox
 * @date 2025/12/12
 */
@Slf4j
@Service
public class InfluxDBService {

    @Resource
    private InfluxDBClient influxDBClient;

    @Value("${influxdb.bucket}")
    private String bucket;

    /**
     * 批量写入缓冲队列
     */
    private final BlockingQueue<Point> writeQueue = new LinkedBlockingQueue<>(5000);

    /**
     * 批量写入线程池
     */
    private ScheduledExecutorService scheduledExecutor;

    /**
     * 批量写入大小(每批次100条)
     */
    private static final int BATCH_SIZE = 100;

    /**
     * 批量写入间隔(每秒执行一次)
     */
    private static final int FLUSH_INTERVAL_SECONDS = 1;

    @PostConstruct
    public void init() {
        // 初始化定时任务,定期批量写入
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "influxdb-batch-writer");
            thread.setDaemon(true);
            return thread;
        });

        // 每秒执行一次批量写入
        scheduledExecutor.scheduleAtFixedRate(
            this::flushBatch,
            FLUSH_INTERVAL_SECONDS,
            FLUSH_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );

        log.info("InfluxDB批量写入服务已启动 - 批次大小: {}, 写入间隔: {}秒", BATCH_SIZE, FLUSH_INTERVAL_SECONDS);
    }

    @PreDestroy
    public void destroy() {
        if (scheduledExecutor != null) {
            // 关闭前先刷新剩余数据
            flushBatch();
            scheduledExecutor.shutdown();
            try {
                if (!scheduledExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduledExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduledExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        log.info("InfluxDB批量写入服务已停止");
    }

    /**
     * 批量刷新数据到InfluxDB
     */
    private void flushBatch() {
        List<Point> points = new ArrayList<>(BATCH_SIZE);
        writeQueue.drainTo(points, BATCH_SIZE);

        if (points.isEmpty()) {
            return;
        }

        try {
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            writeApi.writePoints(points);
            log.debug("批量写入InfluxDB成功 - 数据量: {}", points.size());
        } catch (Exception e) {
            log.error("批量写入InfluxDB失败 - 数据量: {}", points.size(), e);
        }
    }

    /**
     * 写入设备数据(批量优化版)
     * 数据先写入队列,由定时任务批量写入InfluxDB
     */
    public void writeDeviceData(IotDevice device, JSONObject data) {
        try {
            Point point = Point.measurement("device_data")
                    .addTag("deviceKey", device.getDeviceKey())
                    .addTag("deviceId", device.getId())
                    .addTag("deviceName", device.getDeviceName())
                    .addTag("productId", device.getProductId())
                    .time(Instant.now(), WritePrecision.MS);

            // 添加所有属性字段
            data.forEach((key, value) -> {
                if (value instanceof Number) {
                    point.addField(key, ((Number) value).doubleValue());
                } else if (value instanceof Boolean) {
                    // 将布尔值转换为整数存储(0或1),避免聚合查询时出错
                    point.addField(key, (Boolean) value ? 1 : 0);
                } else {
                    point.addField(key, value.toString());
                }
            });

            // 添加到批量写入队列
            boolean success = writeQueue.offer(point);
            if (!success) {
                // 队列已满,立即刷新
                log.warn("InfluxDB写入队列已满,触发立即刷新");
                flushBatch();
                // 重试一次
                writeQueue.offer(point);
            }
        } catch (Exception e) {
            log.error("添加数据到InfluxDB队列失败", e);
        }
    }

    /**
     * 查询设备图表数据（按时间分组）
     */
    public List<JSONObject> getChartData(String deviceId, int hours) {
        try {
            String query = String.format(
                "from(bucket:\"%s\") " +
                "|> range(start: -%dh) " +
                "|> filter(fn: (r) => r[\"_measurement\"] == \"device_data\") " +
                "|> filter(fn: (r) => r[\"deviceId\"] == \"%s\") " +
                "|> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\") " +
                "|> sort(columns: [\"_time\"], desc: false)",
                bucket, hours, deviceId
            );

            List<FluxTable> tables = influxDBClient.getQueryApi().query(query);
            List<JSONObject> chartDataList = new ArrayList<>();

            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    JSONObject timeData = JSONUtil.createObj();
                    Instant time = record.getTime();
                    if (time != null) {
                        timeData.set("time", time.toString());
                        timeData.set("timestamp", time.toEpochMilli());
                    }

                    // 提取所有字段值
                    JSONObject properties = JSONUtil.createObj();
                    record.getValues().forEach((key, value) -> {
                        if (!key.startsWith("_") && !key.equals("result") && 
                            !key.equals("table") && !key.equals("deviceId") && 
                            !key.equals("deviceKey") && !key.equals("deviceName") && 
                            !key.equals("productId")) {
                            if (value != null) {
                                properties.set(key, value);
                            }
                        }
                    });

                    timeData.set("properties", properties);
                    chartDataList.add(timeData);
                }
            }

            return chartDataList;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * 分页查询设备数据
     */
    public Map<String, Object> queryDeviceDataPage(String deviceId, int pageNum, int pageSize) {
        try {
            // 计算偏移量
            int offset = (pageNum - 1) * pageSize;
            
            String query = String.format(
                "from(bucket:\"%s\") " +
                "|> range(start: -30d) " +
                "|> filter(fn: (r) => r[\"_measurement\"] == \"device_data\") " +
                "|> filter(fn: (r) => r[\"deviceId\"] == \"%s\") " +
                "|> sort(columns: [\"_time\"], desc: true) " +
                "|> limit(n: %d, offset: %d)",
                bucket, deviceId, pageSize, offset
            );

            List<FluxTable> tables = influxDBClient.getQueryApi().query(query);
            List<Map<String, Object>> records = new ArrayList<>();

            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("time", record.getTime());
                    data.put("field", record.getField());
                    data.put("value", record.getValue());
                    data.put("deviceId", record.getValueByKey("deviceId"));
                    data.put("deviceKey", record.getValueByKey("deviceKey"));
                    records.add(data);
                }
            }

            // 构造分页结果
            Map<String, Object> result = new HashMap<>();
            result.put("records", records);
            result.put("current", pageNum);
            result.put("size", pageSize);
            result.put("total", records.size()); // 注意：InfluxDB不提供总数，这里返回当前页数量
            
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("records", new ArrayList<>());
            result.put("current", pageNum);
            result.put("size", pageSize);
            result.put("total", 0);
            return result;
        }
    }
}
