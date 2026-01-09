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
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import vip.xiaonuo.iot.modular.device.entity.IotDevice;
import vip.xiaonuo.iot.modular.northboundconfig.entity.IotNorthboundConfig;

/**
 * Webhook推送处理器
 *
 * @author yubaoshan
 * @date 2026/01/08
 */
@Slf4j
@Component
public class WebhookPushHandler {

    private final RestTemplate restTemplate;

    public WebhookPushHandler() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * 推送数据到Webhook
     */
    public boolean push(IotNorthboundConfig config, IotDevice device, JSONObject data) {
        try {
            // 构建推送数据
            JSONObject payload = buildPayload(device, data);
            
            // 设置请求头
            HttpHeaders headers = buildHeaders(config);
            
            // 发送HTTP POST请求
            HttpEntity<String> request = new HttpEntity<>(payload.toString(), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                config.getTargetUrl(), request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("Webhook推送成功 - URL: {}, Device: {}", 
                    config.getTargetUrl(), device.getDeviceKey());
                return true;
            } else {
                log.warn("Webhook推送失败 - URL: {}, Status: {}", 
                    config.getTargetUrl(), response.getStatusCode());
                return false;
            }
            
        } catch (Exception e) {
            log.error("Webhook推送异常 - URL: {}, Device: {}, Error: {}", 
                config.getTargetUrl(), device.getDeviceKey(), e.getMessage());
            return false;
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
     * 构建请求头
     */
    private HttpHeaders buildHeaders(IotNorthboundConfig config) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // 处理认证
        String authType = config.getAuthType();
        if ("TOKEN".equals(authType) && StrUtil.isNotBlank(config.getAuthToken())) {
            headers.set("Authorization", "Bearer " + config.getAuthToken());
        } else if ("BASIC".equals(authType) && StrUtil.isNotBlank(config.getAuthUsername())) {
            String auth = config.getAuthUsername() + ":" + config.getAuthPassword();
            String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes());
            headers.set("Authorization", "Basic " + encodedAuth);
        } else if ("APIKEY".equals(authType) && StrUtil.isNotBlank(config.getAuthToken())) {
            headers.set("X-API-Key", config.getAuthToken());
        }
        
        // 添加自定义请求头
        if (StrUtil.isNotBlank(config.getCustomHeaders())) {
            try {
                JSONObject customHeaders = JSONUtil.parseObj(config.getCustomHeaders());
                customHeaders.forEach((key, value) -> headers.set(key, value.toString()));
            } catch (Exception e) {
                log.warn("解析自定义请求头失败", e);
            }
        }
        
        return headers;
    }

    /**
     * 测试连接
     */
    public JSONObject testConnection(IotNorthboundConfig config) {
        JSONObject result = new JSONObject();
        try {
            // 构建测试数据
            JSONObject testData = new JSONObject();
            testData.set("test", true);
            testData.set("timestamp", System.currentTimeMillis());
            
            HttpHeaders headers = buildHeaders(config);
            HttpEntity<String> request = new HttpEntity<>(testData.toString(), headers);
            
            long startTime = System.currentTimeMillis();
            ResponseEntity<String> response = restTemplate.postForEntity(
                config.getTargetUrl(), request, String.class);
            long costTime = System.currentTimeMillis() - startTime;
            
            result.set("success", true);
            result.set("statusCode", response.getStatusCode().value());
            result.set("costTime", costTime);
            result.set("message", "连接测试成功");
            
        } catch (Exception e) {
            result.set("success", false);
            result.set("message", "连接测试失败: " + e.getMessage());
        }
        return result;
    }
}
