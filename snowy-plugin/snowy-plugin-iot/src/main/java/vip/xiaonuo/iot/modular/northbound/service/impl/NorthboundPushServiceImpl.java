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
package vip.xiaonuo.iot.modular.northbound.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vip.xiaonuo.iot.modular.device.entity.IotDevice;
import vip.xiaonuo.iot.modular.northboundconfig.entity.IotNorthboundConfig;
import vip.xiaonuo.iot.modular.northbounddevicerel.entity.IotNorthboundDeviceRel;
import vip.xiaonuo.iot.modular.northboundlog.entity.IotNorthboundLog;
import vip.xiaonuo.iot.modular.northboundlog.service.IotNorthboundLogService;
import vip.xiaonuo.iot.modular.northboundstatistics.service.IotNorthboundStatisticsService;
import vip.xiaonuo.iot.modular.northbound.handler.MqttPushHandler;
import vip.xiaonuo.iot.modular.northbound.handler.WebhookPushHandler;
import vip.xiaonuo.iot.modular.northbound.util.DataProcessUtil;
import vip.xiaonuo.iot.modular.northboundconfig.service.IotNorthboundConfigService;
import vip.xiaonuo.iot.modular.northbounddevicerel.service.IotNorthboundDeviceRelService;
import vip.xiaonuo.iot.modular.northbound.service.NorthboundPushService;

import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

/**
 * 北向推送服务实现
 *
 * @author yubaoshan
 * @date 2026/01/08
 */
@Slf4j
@Service
public class NorthboundPushServiceImpl implements NorthboundPushService {

    @Resource
    private IotNorthboundConfigService configService;

    @Resource
    private IotNorthboundDeviceRelService deviceRelService;

    @Resource
    private WebhookPushHandler webhookPushHandler;

    @Resource
    private MqttPushHandler mqttPushHandler;

    @Resource
    private IotNorthboundLogService logService;

    @Resource
    private IotNorthboundStatisticsService statisticsService;

    // 推送线程池
    private final ExecutorService pushExecutor = new ThreadPoolExecutor(
        4, 10, 60L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(500),
        new ThreadPoolExecutor.CallerRunsPolicy()
    );

    @Override
    public void pushDeviceData(IotDevice device, JSONObject data) {
        // 异步推送，避免阻塞设备数据处理
        pushExecutor.submit(() -> {
            try {
                // 查询启用的推送配置
                List<IotNorthboundConfig> configs = getEnabledConfigsByDevice(device.getId());
                
                if (CollUtil.isEmpty(configs)) {
                    return;
                }
                
                // 并发推送到多个目标
                for (IotNorthboundConfig config : configs) {
                    // 检查是否启用属性上报触发
                    if (!shouldTrigger(config, "PROPERTY_REPORT")) {
                        continue;
                    }
                    pushToTarget(config, device, data);
                }
                
            } catch (Exception e) {
                log.error("北向推送设备数据异常 - DeviceId: {}", device.getId(), e);
            }
        });
    }

    @Override
    public void pushDeviceEvent(IotDevice device, JSONObject eventData) {
        pushExecutor.submit(() -> {
            try {
                List<IotNorthboundConfig> configs = getEnabledConfigsByDevice(device.getId());
                
                if (CollUtil.isEmpty(configs)) {
                    return;
                }
                
                // 构建事件数据
                JSONObject data = new JSONObject();
                data.set("type", "event");
                data.set("event", eventData);
                
                for (IotNorthboundConfig config : configs) {
                    // 检查是否启用事件触发
                    if (!shouldTrigger(config, "EVENT")) {
                        continue;
                    }
                    pushToTarget(config, device, data);
                }
                
            } catch (Exception e) {
                log.error("北向推送设备事件异常 - DeviceId: {}", device.getId(), e);
            }
        });
    }

    @Override
    public void pushDeviceStatus(IotDevice device, String status) {
        pushExecutor.submit(() -> {
            try {
                List<IotNorthboundConfig> configs = getEnabledConfigsByDevice(device.getId());
                
                if (CollUtil.isEmpty(configs)) {
                    return;
                }
                
                // 构建状态数据
                JSONObject data = new JSONObject();
                data.set("type", "status");
                data.set("status", status);
                
                for (IotNorthboundConfig config : configs) {
                    // 检查是否启用状态变化触发
                    if (!shouldTrigger(config, "STATUS_CHANGE")) {
                        continue;
                    }
                    pushToTarget(config, device, data);
                }
                
            } catch (Exception e) {
                log.error("北向推送设备状态异常 - DeviceId: {}", device.getId(), e);
            }
        });
    }

    @Override
    public JSONObject testConnection(String configId) {
        IotNorthboundConfig config = configService.getById(configId);
        if (ObjectUtil.isNull(config)) {
            JSONObject result = new JSONObject();
            result.set("success", false);
            result.set("message", "配置不存在");
            return result;
        }

        String pushType = config.getPushType();
        if ("WEBHOOK".equals(pushType)) {
            return webhookPushHandler.testConnection(config);
        } else if ("MQTT".equals(pushType)) {
            return mqttPushHandler.testConnection(config);
        } else {
            JSONObject result = new JSONObject();
            result.set("success", false);
            result.set("message", "不支持的推送类型: " + pushType);
            return result;
        }
    }

    /**
     * 查询启用的推送配置(按设备过滤)
     */
    private List<IotNorthboundConfig> getEnabledConfigsByDevice(String deviceId) {
        // 查询设备关联的配置ID
        LambdaQueryWrapper<IotNorthboundDeviceRel> relWrapper = new LambdaQueryWrapper<>();
        relWrapper.eq(IotNorthboundDeviceRel::getDeviceId, deviceId);
        List<IotNorthboundDeviceRel> relList = deviceRelService.list(relWrapper);
        
        if (CollUtil.isEmpty(relList)) {
            // 如果没有设备关联，查询全局配置(DEVICE_ID为NULL的配置)
            relWrapper = new LambdaQueryWrapper<>();
            relWrapper.isNull(IotNorthboundDeviceRel::getDeviceId);
            relList = deviceRelService.list(relWrapper);
        }
        
        if (CollUtil.isEmpty(relList)) {
            return List.of();
        }
        
        // 查询启用的配置
        List<String> configIds = relList.stream()
            .map(IotNorthboundDeviceRel::getConfigId)
            .toList();
        
        LambdaQueryWrapper<IotNorthboundConfig> configWrapper = new LambdaQueryWrapper<>();
        configWrapper.in(IotNorthboundConfig::getId, configIds);
        configWrapper.eq(IotNorthboundConfig::getEnabled, "ENABLE");
        
        return configService.list(configWrapper);
    }

    /**
     * 推送到目标系统（带日志、重试、统计）
     */
    private void pushToTarget(IotNorthboundConfig config, IotDevice device, JSONObject data) {
        String pushType = config.getPushType();
        int maxRetries = config.getRetryTimes() != null ? config.getRetryTimes() : 3;
        
        // 数据过滤
        JSONObject filteredData = DataProcessUtil.filterData(data, config.getDataFilter());
        if (filteredData == null) {
            log.debug("数据不符合过滤条件，忽略推送 - Device: {}", device.getDeviceKey());
            return;
        }
        
        // 数据转换
        JSONObject transformedData = DataProcessUtil.transformData(filteredData, config.getDataTransform());
        
        // 记录推送日志
        IotNorthboundLog pushLog = createPushLog(config, device, transformedData);
        
        boolean success = false;
        int retryCount = 0;
        String errorMessage = null;
        long startTime = System.currentTimeMillis();
        
        // 执行推送（带重试）
        while (!success && retryCount <= maxRetries) {
            try {
                if ("WEBHOOK".equals(pushType)) {
                    success = webhookPushHandler.push(config, device, transformedData);
                } else if ("MQTT".equals(pushType)) {
                    success = mqttPushHandler.push(config, device, transformedData);
                } else {
                    log.warn("不支持的推送类型 - Type: {}, ConfigId: {}", pushType, config.getId());
                    errorMessage = "不支持的推送类型: " + pushType;
                    break;
                }
                
                if (!success && retryCount < maxRetries) {
                    retryCount++;
                    log.warn("推送失败，执行第{}次重试 - ConfigId: {}, Device: {}", 
                        retryCount, config.getId(), device.getDeviceKey());
                    Thread.sleep(1000 * retryCount); // 递增延迟
                }
                
            } catch (Exception e) {
                errorMessage = e.getMessage();
                if (retryCount < maxRetries) {
                    retryCount++;
                    log.warn("推送异常，执行第{}次重试 - ConfigId: {}, Error: {}", 
                        retryCount, config.getId(), e.getMessage());
                    try {
                        Thread.sleep(1000 * retryCount);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    log.error("推送到目标系统异常 - ConfigId: {}, Device: {}", 
                        config.getId(), device.getDeviceKey(), e);
                    break;
                }
            }
        }
        
        long costTime = System.currentTimeMillis() - startTime;
        
        // 更新推送日志
        pushLog.setStatus(success ? "SUCCESS" : "FAILED");
        pushLog.setRetryCount(retryCount);
        pushLog.setCostTime((int) costTime);
        pushLog.setErrorMessage(errorMessage);
        pushLog.setPushTime(new Date());
        
        // 保存日志
        try {
            logService.save(pushLog);
        } catch (Exception e) {
            log.error("保存推送日志失败", e);
        }
        
        // 更新统计数据（异步）
        final boolean finalSuccess = success;
        final int finalCostTime = (int) costTime;
        final String configId = config.getId();
        pushExecutor.submit(() -> {
            try {
                statisticsService.updateStatistics(configId, finalSuccess, finalCostTime);
            } catch (Exception e) {
                log.error("更新统计数据失败", e);
            }
        });
    }
    
    /**
     * 创建推送日志对象
     */
    private IotNorthboundLog createPushLog(IotNorthboundConfig config, IotDevice device, JSONObject data) {
        IotNorthboundLog log = new IotNorthboundLog();
        log.setId(IdUtil.getSnowflakeNextIdStr());
        log.setConfigId(config.getId());
        log.setDeviceId(device.getId());
        log.setDeviceKey(device.getDeviceKey());
        log.setPushType(config.getPushType());
        log.setTargetUrl(config.getTargetUrl());
        log.setPayload(data.toString());
        log.setStatus("PENDING");
        log.setRetryCount(0);
        log.setCreateTime(new Date());
        return log;
    }

    /**
     * 判断是否应该触发推送
     *
     * @param config 推送配置
     * @param triggerType 触发类型: PROPERTY_REPORT, EVENT, STATUS_CHANGE, MANUAL, SCHEDULE
     * @return 是否触发
     */
    private boolean shouldTrigger(IotNorthboundConfig config, String triggerType) {
        String pushTrigger = config.getPushTrigger();
        
        // 如果未配置触发时机，默认全部触发
        if (StrUtil.isBlank(pushTrigger)) {
            return true;
        }
        
        // 检查是否包含指定的触发类型
        return pushTrigger.contains(triggerType);
    }
}
