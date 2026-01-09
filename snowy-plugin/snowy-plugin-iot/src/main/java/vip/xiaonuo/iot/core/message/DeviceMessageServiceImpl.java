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
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vip.xiaonuo.iot.core.mqtt.MqttSessionManager;
import vip.xiaonuo.iot.modular.device.entity.IotDevice;
import vip.xiaonuo.iot.modular.device.service.IotDeviceService;
import vip.xiaonuo.iot.core.handler.GatewayMessageHandler;
import vip.xiaonuo.dev.api.DevSseApi;

import java.nio.charset.StandardCharsets;

/**
 * 设备消息服务实现
 *
 * @author yubaoshan
 * @date 2024/12/11 07:40
 **/
@Slf4j
@Service
public class DeviceMessageServiceImpl implements DeviceMessageService {

    @Resource
    private IotDeviceService iotDeviceService;

    @Resource
    private MqttSessionManager mqttSessionManager;

    @Resource
    private DeviceDataHandler deviceDataHandler;

    @Resource
    private RuleEngineService ruleEngineService;

    @Resource
    private GatewayMessageHandler gatewayMessageHandler;

    @Resource
    private DevSseApi devSseApi;

    // 使用DeviceDataHandler的SSE推送线程池（通过注入方式复用）

    @Override
    public boolean authenticate(String clientId, String username, String password) {
        try {
            // 根据设备标识查询设备
            IotDevice device = getDeviceByKey(clientId);
            
            if (ObjectUtil.isNull(device)) {
                log.warn("设备不存在 - DeviceKey: {}", clientId);
                return false;
            }

            // 验证设备密钥
            if (!StrUtil.equals(device.getDeviceSecret(), password)) {
                log.warn("设备密钥错误 - DeviceKey: {}", clientId);
                return false;
            }

            // 检查设备状态
            if ("DISABLE".equals(device.getDeviceStatus())) {
                log.warn("设备已禁用 - DeviceKey: {}", clientId);
                return false;
            }

            log.info("设备认证成功 - DeviceKey: {}, DeviceName: {}", clientId, device.getDeviceName());
            return true;
        } catch (Exception e) {
            log.error("设备认证异常 - DeviceKey: {}", clientId, e);
            return false;
        }
    }

    @Override
    public void deviceOnline(String clientId, String ipAddress) {
        try {
            IotDevice device = getDeviceByKey(clientId);
            if (ObjectUtil.isNotNull(device)) {
                // 更新设备状态为在线
                device.setDeviceStatus("ONLINE");
                device.setLastOnlineTime(DateUtil.date());
                // 首次激活
                if (ObjectUtil.isNull(device.getActiveTime())) {
                    device.setActiveTime(DateUtil.date());
                }
                
                iotDeviceService.updateById(device);
                
                // 推送SSE消息到前端
                pushDeviceStatusToFrontend(device, "ONLINE");
                
                log.info("设备上线 - DeviceKey: {}, IP: {}", clientId, ipAddress);
            }
        } catch (Exception e) {
            log.error("设备上线处理异常 - DeviceKey: {}", clientId, e);
        }
    }

    @Override
    public void deviceOffline(String clientId) {
        try {
            IotDevice device = getDeviceByKey(clientId);
            if (ObjectUtil.isNotNull(device)) {
                // 更新设备状态为离线
                device.setDeviceStatus("OFFLINE");
                iotDeviceService.updateById(device);
                
                // 推送SSE消息到前端
                pushDeviceStatusToFrontend(device, "OFFLINE");
                
                log.info("设备离线 - DeviceKey: {}", clientId);
            }
        } catch (Exception e) {
            log.error("设备离线处理异常 - DeviceKey: {}", clientId, e);
        }
    }

    @Override
    public void handleDeviceMessage(String topic, String message) {
        try {
            // 解析Topic: /{productKey}/{deviceKey}/property/post
            String[] parts = topic.split("/");
            if (parts.length < 4) {
                log.warn("Topic格式错误: {}", topic);
                return;
            }

            String deviceKey = parts[2];
            String messageType = parts[3]; // property/event/command/gateway

            // 解析消息
            JSONObject jsonMessage = JSONUtil.parseObj(message);

            // 获取设备信息
            IotDevice device = getDeviceByKey(deviceKey);
            if (ObjectUtil.isNull(device)) {
                log.warn("设备不存在 - DeviceKey: {}", deviceKey);
                return;
            }

            // 根据消息类型处理
            switch (messageType) {
                case "property":
                    // 属性上报
                    deviceDataHandler.handlePropertyData(device, jsonMessage);
                    // 触发规则引擎
                    ruleEngineService.triggerByDeviceData(device.getId(), jsonMessage);
                    break;
                case "event":
                    // 事件上报
                    deviceDataHandler.handleEventData(device, jsonMessage);
                    break;
                case "response":
                    // 指令响应
                    deviceDataHandler.handleCommandResponse(device, jsonMessage);
                    break;
                case "gateway":
                case "topo":
                    // 网关消息
                    gatewayMessageHandler.handleGatewayMessage(topic, message);
                    break;
                default:
                    log.warn("未知消息类型: {}", messageType);
            }
        } catch (Exception e) {
            log.error("处理设备消息异常 - Topic: {}, Message: {}", topic, message, e);
        }
    }

    @Override
    public boolean sendToDevice(String deviceKey, String topic, String message) {
        try {
            // 获取设备连接
            Channel channel = mqttSessionManager.getChannel(deviceKey);
            if (channel == null || !channel.isActive()) {
                log.warn("设备未连接或连接已断开 - DeviceKey: {}", deviceKey);
                return false;
            }

            // 构建MQTT消息
            MqttPublishMessage publishMessage = (MqttPublishMessage) MqttMessageFactory.newMessage(
                    new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_LEAST_ONCE, false, 0),
                    new MqttPublishVariableHeader(topic, (int) IdUtil.getSnowflakeNextId()),
                    Unpooled.copiedBuffer(message.getBytes(StandardCharsets.UTF_8))
            );

            // 发送消息
            channel.writeAndFlush(publishMessage);
            log.info("向设备下发消息成功 - DeviceKey: {}, Topic: {}", deviceKey, topic);
            return true;
        } catch (Exception e) {
            log.error("向设备下发消息失败 - DeviceKey: {}", deviceKey, e);
            return false;
        }
    }

    /**
     * 根据DeviceKey查询设备
     */
    private IotDevice getDeviceByKey(String deviceKey) {
        LambdaQueryWrapper<IotDevice> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(IotDevice::getDeviceKey, deviceKey);
        return iotDeviceService.getOne(queryWrapper);
    }

    /**
     * 推送设备状态变化到前端
     * 复用DeviceDataHandler的SSE线程池，避免创建新线程
     */
    private void pushDeviceStatusToFrontend(IotDevice device, String status) {
        // 委托给DeviceDataHandler统一处理SSE推送
        deviceDataHandler.pushDeviceStatus(device, status);
    }
}
