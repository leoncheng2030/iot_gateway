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
package vip.xiaonuo.iot.modular.northbound.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.stereotype.Component;
import vip.xiaonuo.iot.modular.device.entity.IotDevice;
import vip.xiaonuo.iot.modular.northboundconfig.entity.IotNorthboundConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MQTT推送处理器
 *
 * @author yubaoshan
 * @date 2026/01/08
 */
@Slf4j
@Component
public class MqttPushHandler {

    // 缓存MQTT客户端连接
    private final Map<String, MqttClient> clientCache = new ConcurrentHashMap<>();
    
    // 连接锁，防止并发创建
    private final Map<String, Object> connectionLocks = new ConcurrentHashMap<>();

    /**
     * 推送数据到MQTT Broker
     */
    public boolean push(IotNorthboundConfig config, IotDevice device, JSONObject data) {
        try {
            MqttClient client = getOrCreateClient(config);
            if (client == null || !client.isConnected()) {
                log.warn("MQTT客户端未连接 - ConfigId: {}", config.getId());
                return false;
            }

            // 构建推送数据
            JSONObject payload = buildPayload(device, data);
            
            // 构建Topic
            String topic = buildTopic(config, device);
            
            // 发布消息
            MqttMessage message = new MqttMessage(payload.toString().getBytes());
            message.setQos(config.getQos() != null ? config.getQos() : 0);
            message.setRetained(false);
            
            client.publish(topic, message);
            
            log.debug("MQTT推送成功 - Topic: {}, Device: {}", topic, device.getDeviceKey());
            return true;
            
        } catch (Exception e) {
            log.error("MQTT推送异常 - ConfigId: {}, Device: {}, Error: {}", 
                config.getId(), device.getDeviceKey(), e.getMessage());
            return false;
        }
    }

    /**
     * 获取或创建MQTT客户端
     */
    private MqttClient getOrCreateClient(IotNorthboundConfig config) {
        String configId = config.getId();
        
        // 双重检查：先检查缓存
        MqttClient client = clientCache.get(configId);
        if (client != null && client.isConnected()) {
            return client;
        }
        
        // 获取配置级别的锁，避免同一配置并发创建
        Object lock = connectionLocks.computeIfAbsent(configId, k -> new Object());
        
        synchronized (lock) {
            // 再次检查缓存（双重检查）
            client = clientCache.get(configId);
            if (client != null && client.isConnected()) {
                return client;
            }
            
            try {
                // 解析Broker地址
                String brokerUrl = config.getTargetUrl();
                if (!brokerUrl.startsWith("tcp://") && !brokerUrl.startsWith("ssl://")) {
                    brokerUrl = "tcp://" + brokerUrl;
                }
                
                // 关键日志:记录即将连接的Broker地址
                log.info("准备连接MQTT Broker - ConfigId: {}, Name: {}, URL: {}", 
                    configId, config.getName(), brokerUrl);

                // 创建客户端
                String clientId = "northbound_" + configId + "_" + System.currentTimeMillis();
                client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());

                // 配置连接选项
                MqttConnectOptions options = new MqttConnectOptions();
                options.setCleanSession(true);
                options.setAutomaticReconnect(true);
                options.setConnectionTimeout(config.getTimeout() != null ? config.getTimeout() / 1000 : 10);
                options.setKeepAliveInterval(60);

                // 认证
                if ("BASIC".equals(config.getAuthType()) && StrUtil.isNotBlank(config.getAuthUsername())) {
                    options.setUserName(config.getAuthUsername());
                    if (StrUtil.isNotBlank(config.getAuthPassword())) {
                        options.setPassword(config.getAuthPassword().toCharArray());
                    }
                }

                // 连接
                client.connect(options);
                
                // 缓存客户端
                clientCache.put(configId, client);
                
                log.info("MQTT客户端连接成功 - ConfigId: {}, Broker: {}", configId, brokerUrl);
                return client;
                
            } catch (Exception e) {
                log.error("MQTT客户端连接失败 - ConfigId: {}, Name: {}, TargetUrl: {}, Error: {}", 
                    configId, config.getName(), config.getTargetUrl(), e.getMessage());
                return null;
            }
        }
    }

    /**
     * 构建推送数据
     */
    private JSONObject buildPayload(IotDevice device, JSONObject data) {
        JSONObject payload = new JSONObject();
        payload.set("deviceId", device.getId());
        payload.set("deviceKey", device.getDeviceKey());
        payload.set("deviceName", device.getDeviceName());
        payload.set("productId", device.getProductId());
        payload.set("data", data);
        payload.set("timestamp", System.currentTimeMillis());
        return payload;
    }

    /**
     * 构建MQTT Topic
     */
    private String buildTopic(IotNorthboundConfig config, IotDevice device) {
        String topic = config.getTargetTopic();
        if (StrUtil.isBlank(topic)) {
            // 默认Topic格式
            topic = "northbound/" + device.getProductId() + "/" + device.getDeviceKey();
        } else {
            // 支持变量替换
            topic = topic.replace("{productId}", device.getProductId())
                         .replace("{deviceKey}", device.getDeviceKey())
                         .replace("{deviceId}", device.getId());
        }
        return topic;
    }

    /**
     * 测试连接
     */
    public JSONObject testConnection(IotNorthboundConfig config) {
        JSONObject result = new JSONObject();
        try {
            long startTime = System.currentTimeMillis();
            MqttClient client = getOrCreateClient(config);
            long costTime = System.currentTimeMillis() - startTime;
            
            if (client != null && client.isConnected()) {
                result.set("success", true);
                result.set("costTime", costTime);
                result.set("message", "MQTT连接测试成功");
            } else {
                result.set("success", false);
                result.set("message", "MQTT连接失败");
            }
            
        } catch (Exception e) {
            result.set("success", false);
            result.set("message", "MQTT连接测试失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 断开并清理客户端
     */
    public void disconnect(String configId) {
        MqttClient client = clientCache.remove(configId);
        if (client != null && client.isConnected()) {
            try {
                client.disconnect();
                client.close();
                log.info("MQTT客户端已断开 - ConfigId: {}", configId);
            } catch (Exception e) {
                log.error("MQTT客户端断开异常", e);
            }
        }
    }
}
