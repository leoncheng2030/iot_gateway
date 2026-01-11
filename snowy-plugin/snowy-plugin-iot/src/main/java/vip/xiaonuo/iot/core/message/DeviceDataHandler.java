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
package vip.xiaonuo.iot.core.message;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vip.xiaonuo.dev.api.DevSseApi;
import vip.xiaonuo.iot.core.util.DriverConfigUtil;
import vip.xiaonuo.iot.modular.device.entity.IotDevice;
import vip.xiaonuo.iot.modular.devicedata.entity.IotDeviceData;
import vip.xiaonuo.iot.modular.devicedata.service.IotDeviceDataService;
import vip.xiaonuo.iot.modular.devicedriverrel.entity.IotDeviceDriverRel;
import vip.xiaonuo.iot.modular.devicedriverrel.service.IotDeviceDriverRelService;
import vip.xiaonuo.iot.modular.deviceshadow.entity.IotDeviceShadow;
import vip.xiaonuo.iot.modular.deviceshadow.service.IotDeviceShadowService;
import vip.xiaonuo.iot.core.storage.InfluxDBService;
import vip.xiaonuo.iot.core.mq.MessageProducer;
import vip.xiaonuo.iot.modular.northbound.service.NorthboundPushService;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 设备数据处理器
 *
 * @author yubaoshan
 * @date 2024/12/11 07:40
 **/
@Slf4j
@Component
public class DeviceDataHandler {

    @Resource
    private IotDeviceDataService iotDeviceDataService;

    @Resource
    private IotDeviceShadowService iotDeviceShadowService;

    @Resource
    private IotDeviceDriverRelService iotDeviceDriverRelService;

    @Resource
    private DevSseApi devSseApi;

    @Resource
    private InfluxDBService influxDBService;

    @Resource
    private MessageProducer messageProducer;

    @Resource
    private NorthboundPushService northboundPushService;

    /**
     * 设备数据批量写入队列
     */
    private final BlockingQueue<IotDeviceData> deviceDataQueue = new LinkedBlockingQueue<>(1000);

    /**
     * 设备影子更新队列(deviceId -> data)
     */
    private final ConcurrentHashMap<String, JSONObject> shadowUpdateMap = new ConcurrentHashMap<>(500);
    
    /**
     * 设备影子缓存(deviceId -> lastReportedData) 用于数据变化检测
     */
    private final ConcurrentHashMap<String, JSONObject> shadowCache = new ConcurrentHashMap<>(500);

    /**
     * 批量写入定时器
     */
    private ScheduledExecutorService batchWriteExecutor;

    /**
     * SSE推送线程池
     * 核心线程数：4（根据CPU核心数调整）
     * 最大线程数：20（支持高并发场景）
     * 队列容量：1000（缓冲待推送消息）
     */
    private ExecutorService sseExecutor;

    @PostConstruct
    public void init() {
        sseExecutor = new ThreadPoolExecutor(
            4,  // 核心线程数
            20, // 最大线程数
            60L, TimeUnit.SECONDS, // 空闲线程存活时间
            new LinkedBlockingQueue<>(1000), // 任务队列
            new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, "sse-push-" + counter.getAndIncrement());
                    thread.setDaemon(true); // 设置为守护线程
                    return thread;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：调用者线程执行
        );

        // 初始化批量写入定时任务
        batchWriteExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "mysql-batch-writer");
            thread.setDaemon(true);
            return thread;
        });

        // 每秒执行一次批量写入
        batchWriteExecutor.scheduleAtFixedRate(
            this::flushBatchData,
            1,
            1,
            TimeUnit.SECONDS
        );

        log.info("设备数据批量写入服务已启动 - MySQL批量写入间隔: 1秒");
    }

    @PreDestroy
    public void destroy() {
        // 关闭SSE线程池
        if (sseExecutor != null) {
            sseExecutor.shutdown();
            try {
                if (!sseExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    sseExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                sseExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // 关闭批量写入线程池前先刷新数据
        if (batchWriteExecutor != null) {
            flushBatchData();
            batchWriteExecutor.shutdown();
            try {
                if (!batchWriteExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    batchWriteExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                batchWriteExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        log.info("设备数据批量写入服务已停止");
    }

    /**
     * 批量刷新数据到MySQL
     */
    private void flushBatchData() {
        // 1. 批量写入设备数据
        List<IotDeviceData> dataList = new ArrayList<>(100);
        deviceDataQueue.drainTo(dataList, 100);
        if (!dataList.isEmpty()) {
            try {
                iotDeviceDataService.saveBatch(dataList);
                log.debug("批量写入设备数据成功 - 数量: {}", dataList.size());
            } catch (Exception e) {
                log.error("批量写入设备数据失败 - 数量: {}", dataList.size(), e);
            }
        }

        // 2. 批量更新设备影子
        if (!shadowUpdateMap.isEmpty()) {
            // 取出所有待更新的影子数据
            Map<String, JSONObject> updates = new HashMap<>(shadowUpdateMap);
            shadowUpdateMap.clear();

            if (!updates.isEmpty()) {
                try {
                    // 批量查询现有影子
                    LambdaQueryWrapper<IotDeviceShadow> queryWrapper = new LambdaQueryWrapper<>();
                    queryWrapper.in(IotDeviceShadow::getDeviceId, updates.keySet());
                    List<IotDeviceShadow> existingShadows = iotDeviceShadowService.list(queryWrapper);
                    
                    Map<String, IotDeviceShadow> existingMap = existingShadows.stream()
                        .collect(Collectors.toMap(IotDeviceShadow::getDeviceId, s -> s));

                    List<IotDeviceShadow> toInsert = new ArrayList<>();
                    List<IotDeviceShadow> toUpdate = new ArrayList<>();

                    // 处理每个设备的影子更新
                    for (Map.Entry<String, JSONObject> entry : updates.entrySet()) {
                        String deviceId = entry.getKey();
                        JSONObject newData = entry.getValue();

                        IotDeviceShadow shadow = existingMap.get(deviceId);
                        JSONObject reportedData = JSONUtil.createObj();

                        if (shadow == null) {
                            // 新增
                            shadow = new IotDeviceShadow();
                            shadow.setDeviceId(deviceId);
                            shadow.setVersion(1L);
                            reportedData.putAll(newData);
                            shadow.setReported(reportedData.toString());
                            shadow.setUpdateTime(DateUtil.date());
                            toInsert.add(shadow);
                        } else {
                            // 更新：合并数据
                            shadow.setVersion(shadow.getVersion() + 1);
                            if (StrUtil.isNotBlank(shadow.getReported())) {
                                try {
                                    JSONObject existingData = JSONUtil.parseObj(shadow.getReported());
                                    reportedData.putAll(existingData);
                                } catch (Exception e) {
                                    log.warn("解析现有影子数据失败 - DeviceId: {}", deviceId, e);
                                }
                            }
                            reportedData.putAll(newData);
                            shadow.setReported(reportedData.toString());
                            shadow.setUpdateTime(DateUtil.date());
                            toUpdate.add(shadow);
                        }

                        // 推送设备影子变化到前端
                        pushDeviceShadowToFrontend(deviceId, shadow);
                    }

                    // 批量保存
                    if (!toInsert.isEmpty()) {
                        iotDeviceShadowService.saveBatch(toInsert);
                    }
                    if (!toUpdate.isEmpty()) {
                        iotDeviceShadowService.updateBatchById(toUpdate);
                    }

                    if (toInsert.size() > 0 || toUpdate.size() > 0) {
                        log.debug("批量更新设备影子成功 - 新增: {}, 更新: {}", toInsert.size(), toUpdate.size());
                    }
                } catch (Exception e) {
                    log.error("批量更新设备影子失败 - 数量: {}", updates.size(), e);
                }
            }
        }
    }

    /**
     * 处理属性数据
     */
    public void handlePropertyData(IotDevice device, JSONObject data) {
        try {
            // 检测数据是否变化
            boolean dataChanged = isDataChanged(device.getId(), data);
            
            // 1. 发送到RabbitMQ消息队列（异步解耦）
            messageProducer.sendDeviceData(device.getDeviceKey(), data);
            
            // 2. 推送到前端（实时性）
            pushDeviceDataToFrontend(device, data);
            
            // 3. 北向推送到外部系统（仅在数据变化时推送）
            if (dataChanged) {
                northboundPushService.pushDeviceData(device, data);
                log.debug("设备数据变化，触发北向推送 - DeviceId: {}", device.getId());
            } else {
                log.debug("设备数据未变化，跳过北向推送 - DeviceId: {}", device.getId());
            }
            
            // 4. 异步写入InfluxDB和更新影子
            sseExecutor.submit(() -> {
                try {
                    // 写入InfluxDB（时序数据）
                    influxDBService.writeDeviceData(device, data);

                    // 更新设备影子
                    updateDeviceShadow(device.getId(), data);
                } catch (Exception e) {
                    log.error("异步任务异常 - DeviceId: {}", device.getId(), e);
                }
            });
            
        } catch (Exception e) {
            log.error("处理属性数据失败 - DeviceId: {}", device.getId(), e);
        }
    }

    /**
     * 处理事件数据
     */
    public void handleEventData(IotDevice device, JSONObject data) {
        try {
            String eventType = data.getStr("eventType");
            String eventData = data.getStr("data");

            // 发送事件到RabbitMQ（可用于告警处理）
            if ("alarm".equalsIgnoreCase(eventType)) {
                // 告警事件使用高优先级队列
                messageProducer.sendAlarm(eventType, data, 8);
            }

            // 构建设备数据对象
            IotDeviceData deviceData = new IotDeviceData();
            deviceData.setDeviceId(device.getId());
            deviceData.setDataType("EVENT");
            deviceData.setDataKey(eventType);
            deviceData.setDataValue(eventData);
            deviceData.setDataTime(DateUtil.date());
            deviceData.setCreateTime(DateUtil.date());

            // 添加到批量写入队列
            boolean success = deviceDataQueue.offer(deviceData);
            if (!success) {
                log.warn("设备事件数据队列已满,触发立即刷新");
                flushBatchData();
                deviceDataQueue.offer(deviceData);
            }

            // 推送设备事件到前端
            pushDeviceEventToFrontend(device, eventType, eventData);
            
            // 北向推送设备事件
            northboundPushService.pushDeviceEvent(device, data);
        } catch (Exception e) {
            log.error("处理设备事件失败", e);
        }
    }

    /**
     * 处理指令响应
     */
    public void handleCommandResponse(IotDevice device, JSONObject data) {
        try {
            String commandId = data.getStr("commandId");
            String result = data.getStr("result"); // success/failed
            String message = data.getStr("message");
            JSONObject params = data.getJSONObject("params");

            // 构建指令执行记录
            IotDeviceData deviceData = new IotDeviceData();
            deviceData.setDeviceId(device.getId());
            deviceData.setDataType("COMMAND_RESPONSE");
            deviceData.setDataKey(commandId);
            deviceData.setDataValue(JSONUtil.toJsonStr(data));
            deviceData.setDataTime(DateUtil.date());
            deviceData.setCreateTime(DateUtil.date());

            // 添加到批量写入队列
            boolean success = deviceDataQueue.offer(deviceData);
            if (!success) {
                log.warn("指令响应数据队列已满,触发立即刷新");
                flushBatchData();
                deviceDataQueue.offer(deviceData);
            }

            // 推送指令响应到前端
            pushCommandResponseToFrontend(device, commandId, result, message, params);
                
        } catch (Exception e) {
            log.error("处理指令响应失败", e);
        }
    }

    /**
     * 推送指令响应到前端
     */
    private void pushCommandResponseToFrontend(IotDevice device, String commandId, 
                                                String result, String message, JSONObject params) {
        try {
            JSONObject pushMessage = JSONUtil.createObj()
                .set("type", "commandResponse")
                .set("deviceId", device.getId())
                .set("deviceKey", device.getDeviceKey())
                .set("deviceName", device.getDeviceName())
                .set("commandId", commandId)
                .set("result", result)
                .set("message", message)
                .set("params", params)
                .set("timestamp", System.currentTimeMillis());

            // 使用线程池异步推送
            sseExecutor.submit(() -> {
                try {
                    devSseApi.sendMessageToAllClient(pushMessage.toString());
                } catch (Exception e) {
                    // 静默处理
                }
            });
        } catch (RejectedExecutionException e) {
            // 静默处理
        } catch (Exception e) {
            // 静默处理
        }
    }

    /**
     * 更新设备影子(批量优化版)
     * 数据先放入Map缓存,由定时任务批量更新
     */
    private void updateDeviceShadow(String deviceId, JSONObject data) {
        try {
            // 合并到影子更新Map中
            shadowUpdateMap.compute(deviceId, (key, existingData) -> {
                if (existingData == null) {
                    return data;
                } else {
                    // 合并新数据
                    existingData.putAll(data);
                    return existingData;
                }
            });
            
            // 更新缓存用于变化检测
            shadowCache.compute(deviceId, (key, existingData) -> {
                if (existingData == null) {
                    return JSONUtil.parseObj(data.toString()); // 深拷贝
                } else {
                    existingData.putAll(data);
                    return existingData;
                }
            });
        } catch (Exception e) {
            log.error("添加设备影子更新失败 - DeviceId: {}", deviceId, e);
        }
    }
    
    /**
     * 检测设备数据是否变化
     * @param deviceId 设备ID
     * @param newData 新数据
     * @return true-数据变化, false-数据未变化
     */
    private boolean isDataChanged(String deviceId, JSONObject newData) {
        try {
            JSONObject cachedData = shadowCache.get(deviceId);
            
            // 第一次上报或无缓存，认为有变化
            if (cachedData == null) {
                return true;
            }
            
            // 对比每个属性值
            for (String key : newData.keySet()) {
                Object newValue = newData.get(key);
                Object cachedValue = cachedData.get(key);
                
                // 属性值不同，认为有变化
                if (!ObjectUtil.equal(newValue, cachedValue)) {
                    return true;
                }
            }
            
            // 所有属性值都相同，无变化
            return false;
        } catch (Exception e) {
            log.warn("数据变化检测异常，默认认为有变化 - DeviceId: {}", deviceId, e);
            return true; // 发生异常时默认有变化，保证数据不丢失
        }
    }

    /**
     * 推送设备数据变化到前端
     */
    private void pushDeviceDataToFrontend(IotDevice device, JSONObject data) {
        try {
            JSONObject message = JSONUtil.createObj()
                .set("type", "deviceData")
                .set("deviceId", device.getId())
                .set("deviceKey", device.getDeviceKey())
                .set("deviceName", device.getDeviceName())
                .set("data", data)
                .set("timestamp", System.currentTimeMillis());

            // 使用线程池异步推送，避免阻塞设备数据处理流程
            sseExecutor.submit(() -> {
                try {
                    devSseApi.sendMessageToAllClient(message.toString());
                } catch (Exception e) {
                    // 静默处理
                }
            });
        } catch (RejectedExecutionException e) {
            // 静默处理
        } catch (Exception e) {
            // 静默处理
        }
    }

    /**
     * 推送设备事件到前端
     */
    private void pushDeviceEventToFrontend(IotDevice device, String eventType, String eventData) {
        try {
            JSONObject message = JSONUtil.createObj()
                .set("type", "deviceEvent")
                .set("deviceId", device.getId())
                .set("deviceKey", device.getDeviceKey())
                .set("deviceName", device.getDeviceName())
                .set("eventType", eventType)
                .set("eventData", eventData)
                .set("timestamp", System.currentTimeMillis());

            // 使用线程池异步推送
            sseExecutor.submit(() -> {
                try {
                    devSseApi.sendMessageToAllClient(message.toString());
                } catch (Exception e) {
                    // 静默处理
                }
            });
        } catch (RejectedExecutionException e) {
            // 静默处理
        } catch (Exception e) {
            // 静默处理
        }
    }

    /**
     * 推送设备影子变化到前端
     */
    private void pushDeviceShadowToFrontend(String deviceId, IotDeviceShadow shadow) {
        try {
            JSONObject message = JSONUtil.createObj()
                .set("type", "deviceShadow")
                .set("deviceId", deviceId)
                .set("reported", shadow.getReported())
                .set("desired", shadow.getDesired())
                .set("version", shadow.getVersion())
                .set("timestamp", System.currentTimeMillis());

            // 使用线程池异步推送
            sseExecutor.submit(() -> {
                try {
                    devSseApi.sendMessageToAllClient(message.toString());
                } catch (Exception e) {
                    log.error("SSE消息发送失败 - DeviceId: {}", deviceId, e);
                }
            });
        } catch (RejectedExecutionException e) {
            log.error("线程池拒绝推送影子 - DeviceId: {}", deviceId, e);
        } catch (Exception e) {
            log.error("推送影子到前端失败 - DeviceId: {}", deviceId, e);
        }
    }

    /**
     * 推送设备状态变化到前端
     * 供外部调用，复用SSE线程池
     */
    public void pushDeviceStatus(IotDevice device, String status) {
        try {
            // 查询设备驱动关联，用于获取IP地址
            IotDeviceDriverRel driverRel = null;
            if (device.getId() != null) {
                List<IotDeviceDriverRel> relList = iotDeviceDriverRelService.list(
                    new LambdaQueryWrapper<IotDeviceDriverRel>()
                        .eq(IotDeviceDriverRel::getDeviceId, device.getId())
                        .last("LIMIT 1")
                );
                if (!relList.isEmpty()) {
                    driverRel = relList.get(0);
                }
            }
            
            String ipAddress = DriverConfigUtil.getIpAddress(driverRel, device);
            
            JSONObject message = JSONUtil.createObj()
                .set("type", "deviceStatus")
                .set("deviceId", device.getId())
                .set("deviceKey", device.getDeviceKey())
                .set("deviceName", device.getDeviceName())
                .set("status", status)
                .set("ipAddress", ipAddress)
                .set("timestamp", System.currentTimeMillis());

            // 设备离线时发送告警
            if ("OFFLINE".equals(status)) {
                JSONObject alarmData = JSONUtil.createObj()
                    .set("deviceId", device.getId())
                    .set("deviceKey", device.getDeviceKey())
                    .set("deviceName", device.getDeviceName())
                    .set("ipAddress", ipAddress)
                    .set("message", "设备离线")
                    .set("timestamp", System.currentTimeMillis());
                messageProducer.sendAlarm("DEVICE_OFFLINE", alarmData, 5);
            }
            
            // 北向推送设备状态
            northboundPushService.pushDeviceStatus(device, status);

            // 使用线程池异步推送
            sseExecutor.submit(() -> {
                try {
                    devSseApi.sendMessageToAllClient(message.toString());
                } catch (Exception e) {
                    // 静默处理
                }
            });
        } catch (RejectedExecutionException e) {
            // 静默处理
        } catch (Exception e) {
            // 静默处理
        }
    }
}
