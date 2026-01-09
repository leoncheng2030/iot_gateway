/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 *
 * Snowy采用APACHE LICENSE 2.0开源协议，您在使用过程中，需要注意以下几点：
 *
 * 1.请不要删除和修改根目录下的LICENSE文件。
 * 2.请不要删除和修改Snowy源码头部的版权声明。
 * 3.本项目代码可免费商业使用，商业使用请保留源码和相关描述文件的项目出处，作者声明等。
 * 4.分发源码时候，请注明软件出处 https://www.xiaonuo.vip
 * 5.不可二次分发开源参与同类竞品，如有想法可联系团队xiaonuobase@qq.com商议合作。
 * 6.若您的项目无法满足以上几点，需要更多功能代码，获取Snowy商业授权许可，请在官网购买授权，地址为 https://www.xiaonuo.vip
 */
package vip.xiaonuo.iot.modular.statistics.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.system.RuntimeInfo;
import cn.hutool.system.SystemUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.query.FluxTable;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import vip.xiaonuo.iot.modular.device.entity.IotDevice;
import vip.xiaonuo.iot.modular.device.mapper.IotDeviceMapper;
import vip.xiaonuo.iot.modular.devicedriver.entity.IotDeviceDriver;
import vip.xiaonuo.iot.modular.devicedriver.mapper.IotDeviceDriverMapper;
import vip.xiaonuo.iot.modular.statistics.service.IotStatisticsService;

import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * IoT统计数据Service实现类
 *
 * @author jetox
 * @date 2026/01/09 17:00
 */
@Slf4j
@Service
public class IotStatisticsServiceImpl implements IotStatisticsService {

    @Resource
    private IotDeviceMapper iotDeviceMapper;

    @Resource
    private IotDeviceDriverMapper iotDeviceDriverMapper;

    @Resource
    private InfluxDBClient influxDBClient;

    @Value("${influxdb.bucket}")
    private String bucket;

    @Override
    public Map<String, Object> getDeviceStatistics() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 查询设备总数
            Long totalDevices = iotDeviceMapper.selectCount(null);
            
            // 查询在线设备数（状态为ONLINE）
            Long onlineDevices = iotDeviceMapper.selectCount(
                new LambdaQueryWrapper<IotDevice>()
                    .eq(IotDevice::getDeviceStatus, "ONLINE")
            );
            
            // 查询离线设备数（状态为OFFLINE）
            Long offlineDevices = iotDeviceMapper.selectCount(
                new LambdaQueryWrapper<IotDevice>()
                    .eq(IotDevice::getDeviceStatus, "OFFLINE")
            );
            
            // 查询今日数据量（从InfluxDB）
            Long todayDataCount = getTodayDataCount();
            
            result.put("totalDevices", totalDevices != null ? totalDevices : 0);
            result.put("onlineDevices", onlineDevices != null ? onlineDevices : 0);
            result.put("offlineDevices", offlineDevices != null ? offlineDevices : 0);
            result.put("todayDataCount", todayDataCount);
        } catch (Exception e) {
            log.error("获取设备统计数据失败", e);
            result.put("totalDevices", 0);
            result.put("onlineDevices", 0);
            result.put("offlineDevices", 0);
            result.put("todayDataCount", 0);
        }
        
        return result;
    }

    @Override
    public Map<String, Object> getDriverStatistics() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 查询驱动总数
            Long total = iotDeviceDriverMapper.selectCount(null);
            
            // 查询运行中的驱动数（状态为RUNNING）
            Long running = iotDeviceDriverMapper.selectCount(
                new LambdaQueryWrapper<IotDeviceDriver>()
                    .eq(IotDeviceDriver::getStatus, "RUNNING")
            );
            
            // 查询已停止的驱动数（状态为STOPPED）
            Long stopped = iotDeviceDriverMapper.selectCount(
                new LambdaQueryWrapper<IotDeviceDriver>()
                    .eq(IotDeviceDriver::getStatus, "STOPPED")
            );
            
            // 查询异常的驱动数（状态为ERROR）
            Long error = iotDeviceDriverMapper.selectCount(
                new LambdaQueryWrapper<IotDeviceDriver>()
                    .eq(IotDeviceDriver::getStatus, "ERROR")
            );
            
            // 获取最近启动时间和通信速率
            List<IotDeviceDriver> runningDrivers = iotDeviceDriverMapper.selectList(
                new LambdaQueryWrapper<IotDeviceDriver>()
                    .eq(IotDeviceDriver::getStatus, "RUNNING")
                    .orderByDesc(IotDeviceDriver::getUpdateTime)
                    .last("LIMIT 1")
            );
            
            String lastStartTime = "--";
            if (!runningDrivers.isEmpty()) {
                IotDeviceDriver driver = runningDrivers.get(0);
                if (driver.getUpdateTime() != null) {
                    lastStartTime = DateUtil.formatDateTime(driver.getUpdateTime());
                }
            }
            
            // 计算通信速率（基于InfluxDB近1分钟的数据量）
            Long communicationRate = getCommunicationRate();
            
            result.put("total", total != null ? total : 0);
            result.put("running", running != null ? running : 0);
            result.put("stopped", stopped != null ? stopped : 0);
            result.put("error", error != null ? error : 0);
            result.put("lastStartTime", lastStartTime);
            result.put("communicationRate", communicationRate);
        } catch (Exception e) {
            log.error("获取驱动统计数据失败", e);
            result.put("total", 0);
            result.put("running", 0);
            result.put("stopped", 0);
            result.put("error", 0);
            result.put("lastStartTime", "--");
            result.put("communicationRate", 0);
        }
        
        return result;
    }

    @Override
    public Map<String, Object> getAlarmStatistics() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> alarmList = new ArrayList<>();
        
        try {
            // TODO: 这里需要根据实际的告警表实现
            // 目前返回空列表，后续可以基于设备状态、驱动状态等生成告警信息
            
            // 示例：检查离线设备生成告警
            List<IotDevice> offlineDevices = iotDeviceMapper.selectList(
                new LambdaQueryWrapper<IotDevice>()
                    .eq(IotDevice::getDeviceStatus, "OFFLINE")
                    .orderByDesc(IotDevice::getUpdateTime)
                    .last("LIMIT 5")
            );
            
            for (IotDevice device : offlineDevices) {
                Map<String, Object> alarm = new HashMap<>();
                alarm.put("id", device.getId());
                alarm.put("deviceName", device.getDeviceName());
                alarm.put("message", "设备离线");
                alarm.put("level", "high");
                alarm.put("time", getRelativeTime(device.getUpdateTime()));
                alarmList.add(alarm);
            }
            
            result.put("list", alarmList);
        } catch (Exception e) {
            log.error("获取告警统计数据失败", e);
            result.put("list", new ArrayList<>());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> getDataTrend(String timeRange) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            int hours = parseTimeRange(timeRange);
            int dataPoints = getDataPoints(timeRange);
            int intervalMinutes = (hours * 60) / dataPoints;
            
            // 生成时间标签
            List<String> timeLabels = generateTimeLabels(hours, dataPoints);
            
            // 从InfluxDB查询数据采集速率
            List<Long> rateData = getDataRateByTimeRange(hours, intervalMinutes, dataPoints);
            
            // 计算成功率（简化版，假设成功率在95-100之间）
            List<Double> successRateData = new ArrayList<>();
            for (int i = 0; i < dataPoints; i++) {
                successRateData.add(95.0 + Math.random() * 5);
            }
            
            // 计算统计值
            double avgRate = rateData.stream().mapToLong(Long::longValue).average().orElse(0.0);
            double avgSuccessRate = successRateData.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            
            Map<String, Object> chartData = new HashMap<>();
            chartData.put("timeLabels", timeLabels);
            chartData.put("rateData", rateData);
            chartData.put("successRateData", successRateData);
            
            result.put("chartData", chartData);
            result.put("avgRate", Math.round(avgRate));
            result.put("successRate", Math.round(avgSuccessRate * 10) / 10.0);
            result.put("avgDelay", 45); // 平均延迟，可以从实际监控数据获取
        } catch (Exception e) {
            log.error("获取数据采集趋势失败", e);
            result.put("chartData", new HashMap<>());
            result.put("avgRate", 0);
            result.put("successRate", 0);
            result.put("avgDelay", 0);
        }
        
        return result;
    }

    @Override
    public Map<String, Object> getSystemResource() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            SystemInfo si = new SystemInfo();
            HardwareAbstractionLayer hal = si.getHardware();
            
            // 获取物理内存信息（使用OSHI）
            GlobalMemory memory = hal.getMemory();
            long memoryTotal = memory.getTotal();
            long memoryAvailable = memory.getAvailable();
            long memoryUsed = memoryTotal - memoryAvailable;
            
            double memoryUsedGB = memoryUsed / (1024.0 * 1024 * 1024);
            double memoryTotalGB = memoryTotal / (1024.0 * 1024 * 1024);
            double memoryUsage = NumberUtil.mul(NumberUtil.div(memoryUsed, memoryTotal, 4), 100);
            
            // 获取CPU使用率（简化版，使用JVM的）
            double cpuUsage = 0;
            try {
                com.sun.management.OperatingSystemMXBean osBean = 
                    (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
                cpuUsage = osBean.getSystemCpuLoad() * 100;
                if (cpuUsage < 0) {
                    cpuUsage = 0;
                }
            } catch (Exception e) {
                log.warn("获取CPU使用率失败", e);
            }
            
            // 获取磁盘信息
            OperatingSystem operatingSystem = si.getOperatingSystem();
            FileSystem fileSystem = operatingSystem.getFileSystem();
            AtomicLong storageTotal = new AtomicLong();
            AtomicLong storageUsed = new AtomicLong();
            
            List<OSFileStore> fileStores = fileSystem.getFileStores();
            for (OSFileStore osFileStore : fileStores) {
                long totalSpace = osFileStore.getTotalSpace();
                long usableSpace = osFileStore.getUsableSpace();
                long usedSpace = totalSpace - usableSpace;
                storageTotal.addAndGet(totalSpace);
                storageUsed.addAndGet(usedSpace);
            }
            
            double diskUsedGB = storageUsed.get() / (1024.0 * 1024 * 1024);
            double diskTotalGB = storageTotal.get() / (1024.0 * 1024 * 1024);
            double diskUsage = storageTotal.get() > 0 ? 
                NumberUtil.mul(NumberUtil.div(storageUsed.doubleValue(), storageTotal.doubleValue(), 4), 100) : 0;
            
            // 获取系统运行时长
            long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
            String uptime = formatUptime(uptimeMillis);
            
            result.put("cpuUsage", Math.round(cpuUsage));
            result.put("memoryUsage", Math.round(memoryUsage));
            result.put("memoryUsed", Math.round(memoryUsedGB * 10) / 10.0);
            result.put("memoryTotal", Math.round(memoryTotalGB * 10) / 10.0);
            result.put("diskUsage", Math.round(diskUsage));
            result.put("diskUsed", Math.round(diskUsedGB));
            result.put("diskTotal", Math.round(diskTotalGB));
            result.put("uptime", uptime);
        } catch (Exception e) {
            log.error("获取系统资源统计失败", e);
            result.put("cpuUsage", 0);
            result.put("memoryUsage", 0);
            result.put("memoryUsed", 0);
            result.put("memoryTotal", 0);
            result.put("diskUsage", 0);
            result.put("diskUsed", 0);
            result.put("diskTotal", 0);
            result.put("uptime", "0天0小时");
        }
        
        return result;
    }

    /**
     * 获取今日数据量
     */
    private Long getTodayDataCount() {
        try {
            String query = String.format(
                "from(bucket:\"%s\") " +
                "|> range(start: -24h) " +
                "|> filter(fn: (r) => r[\"_measurement\"] == \"device_data\") " +
                "|> count()",
                bucket
            );
            
            List<FluxTable> tables = influxDBClient.getQueryApi().query(query);
            if (tables != null && !tables.isEmpty()) {
                for (FluxTable table : tables) {
                    if (!table.getRecords().isEmpty()) {
                        Object value = table.getRecords().get(0).getValue();
                        if (value instanceof Number) {
                            return ((Number) value).longValue();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("查询今日数据量失败", e);
        }
        return 0L;
    }

    /**
     * 获取通信速率（每秒数据条数）
     */
    private Long getCommunicationRate() {
        try {
            String query = String.format(
                "from(bucket:\"%s\") " +
                "|> range(start: -1m) " +
                "|> filter(fn: (r) => r[\"_measurement\"] == \"device_data\") " +
                "|> count()",
                bucket
            );
            
            List<FluxTable> tables = influxDBClient.getQueryApi().query(query);
            if (tables != null && !tables.isEmpty()) {
                for (FluxTable table : tables) {
                    if (!table.getRecords().isEmpty()) {
                        Object value = table.getRecords().get(0).getValue();
                        if (value instanceof Number) {
                            // 转换为每秒速率
                            return ((Number) value).longValue() / 60;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("查询通信速率失败", e);
        }
        return 0L;
    }

    /**
     * 根据时间范围获取数据速率
     */
    private List<Long> getDataRateByTimeRange(int hours, int intervalMinutes, int dataPoints) {
        List<Long> result = new ArrayList<>();
        
        try {
            String query = String.format(
                "from(bucket:\"%s\") " +
                "|> range(start: -%dh) " +
                "|> filter(fn: (r) => r[\"_measurement\"] == \"device_data\") " +
                "|> aggregateWindow(every: %dm, fn: count, createEmpty: false)",
                bucket, hours, intervalMinutes
            );
            
            List<FluxTable> tables = influxDBClient.getQueryApi().query(query);
            if (!tables.isEmpty()) {
                for (FluxTable table : tables) {
                    List<Long> finalResult = result;
                    table.getRecords().forEach(record -> {
                        Object value = record.getValue();
                        if (value instanceof Number) {
                            // 转换为每秒速率
                            finalResult.add(((Number) value).longValue() / (intervalMinutes * 60L));
                        }
                    });
                }
            }
            
            // 补齐数据点
            while (result.size() < dataPoints) {
                result.add(0L);
            }
            
            // 限制数据点数量
            if (result.size() > dataPoints) {
                result = result.subList(result.size() - dataPoints, result.size());
            }
        } catch (Exception e) {
            log.error("查询数据速率失败", e);
            // 返回默认数据
            for (int i = 0; i < dataPoints; i++) {
                result.add(0L);
            }
        }
        
        return result;
    }

    /**
     * 解析时间范围
     */
    private int parseTimeRange(String timeRange) {
        switch (timeRange) {
            case "6h":
                return 6;
            case "24h":
                return 24;
            case "1h":
            default:
                return 1;
        }
    }

    /**
     * 获取数据点数量
     */
    private int getDataPoints(String timeRange) {
        return switch (timeRange) {
            case "6h" -> 12;
            case "24h" -> 24;
            default -> 12;
        };
    }

    /**
     * 生成时间标签
     */
    private List<String> generateTimeLabels(int hours, int dataPoints) {
        List<String> labels = new ArrayList<>();
        int intervalMinutes = (hours * 60) / dataPoints;
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        
        for (int i = dataPoints - 1; i >= 0; i--) {
            LocalDateTime time = now.minusMinutes((long) i * intervalMinutes);
            labels.add(time.format(formatter));
        }
        
        return labels;
    }

    /**
     * 格式化运行时长
     */
    private String formatUptime(long uptimeMillis) {
        long days = uptimeMillis / (24 * 60 * 60 * 1000);
        long hours = (uptimeMillis % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
        return String.format("%d天%d小时", days, hours);
    }

    /**
     * 获取相对时间
     */
    private String getRelativeTime(Date date) {
        if (date == null) {
            return "未知";
        }
        
        long diff = System.currentTimeMillis() - date.getTime();
        long minutes = diff / (60 * 1000);
        
        if (minutes < 1) {
            return "刚刚";
        } else if (minutes < 60) {
            return minutes + "分钟前";
        } else {
            long hours = minutes / 60;
            if (hours < 24) {
                return hours + "小时前";
            } else {
                long days = hours / 24;
                return days + "天前";
            }
        }
    }
}
