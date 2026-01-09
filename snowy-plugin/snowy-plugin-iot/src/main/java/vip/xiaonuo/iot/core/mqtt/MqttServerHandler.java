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
package vip.xiaonuo.iot.core.mqtt;

import cn.hutool.core.util.StrUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vip.xiaonuo.iot.core.message.DeviceMessageService;

/**
 * MQTT消息处理器
 *
 * @author yubaoshan
 * @date 2024/12/11 07:00
 **/
@Slf4j
@Component
@ChannelHandler.Sharable
public class MqttServerHandler extends ChannelInboundHandlerAdapter {

    @Resource
    private DeviceMessageService deviceMessageService;

    @Resource
    private MqttSessionManager sessionManager;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof MqttMessage)) {
            return;
        }

        MqttMessage mqttMessage = (MqttMessage) msg;
        
        // 空指针保护：检查fixedHeader是否为null
        if (mqttMessage.fixedHeader() == null) {
            log.warn("接收到无效的MQTT消息，fixedHeader为null");
            return;
        }
        
        MqttMessageType messageType = mqttMessage.fixedHeader().messageType();

        try {
            switch (messageType) {
                case CONNECT:
                    if (mqttMessage instanceof MqttConnectMessage) {
                        handleConnect(ctx, (MqttConnectMessage) mqttMessage);
                    } else {
                        log.warn("消息类型为CONNECT，但不是MqttConnectMessage实例");
                    }
                    break;
                case PUBLISH:
                    if (mqttMessage instanceof MqttPublishMessage) {
                        handlePublish(ctx, (MqttPublishMessage) mqttMessage);
                    } else {
                        log.warn("消息类型为PUBLISH，但不是MqttPublishMessage实例");
                    }
                    break;
                case PUBACK:
                    handlePubAck(ctx, mqttMessage);
                    break;
                case SUBSCRIBE:
                    if (mqttMessage instanceof MqttSubscribeMessage) {
                        handleSubscribe(ctx, (MqttSubscribeMessage) mqttMessage);
                    } else {
                        log.warn("消息类型为SUBSCRIBE，但不是MqttSubscribeMessage实例");
                    }
                    break;
                case UNSUBSCRIBE:
                    if (mqttMessage instanceof MqttUnsubscribeMessage) {
                        handleUnSubscribe(ctx, (MqttUnsubscribeMessage) mqttMessage);
                    } else {
                        log.warn("消息类型为UNSUBSCRIBE，但不是MqttUnsubscribeMessage实例");
                    }
                    break;
                case PINGREQ:
                    handlePingReq(ctx);
                    break;
                case DISCONNECT:
                    handleDisconnect(ctx);
                    break;
                default:
                    log.warn("未处理的消息类型: {}", messageType);
                    break;
            }
        } catch (ClassCastException e) {
            log.error("MQTT消息类型转换异常 - MessageType: {}, ActualClass: {}", 
                messageType, mqttMessage.getClass().getName(), e);
            ctx.close();
        }
    }

    /**
     * 处理连接请求
     */
    private void handleConnect(ChannelHandlerContext ctx, MqttConnectMessage msg) {
        String clientId = msg.payload().clientIdentifier();
        String username = msg.payload().userName();
        String password = msg.payload().passwordInBytes() != null ? 
                new String(msg.payload().passwordInBytes()) : "";
        
        // 获取客户端的cleanSession标志
        boolean cleanSession = msg.variableHeader().isCleanSession();
        // 获取协议版本
        int protocolVersion = msg.variableHeader().version();
        String protocolName = msg.variableHeader().name();

        log.info("设备连接请求 - ClientId: {}, Username: {}, Protocol: {} v{}, CleanSession: {}", 
                clientId, username, protocolName, protocolVersion, cleanSession);
        
        // 检查协议版本支持（MQTT 3.1=3, 3.1.1=4, 5.0=5）
        if (protocolVersion != 3 && protocolVersion != 4) {
            log.warn("不支持的MQTT协议版本 - ClientId: {}, Version: {}", clientId, protocolVersion);
            MqttConnAckMessage ackMessage = (MqttConnAckMessage) MqttMessageFactory.newMessage(
                    new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                    new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION, false),
                    null
            );
            ctx.writeAndFlush(ackMessage).addListener(future -> ctx.close());
            return;
        }

        // 设备认证
        boolean authSuccess = deviceMessageService.authenticate(clientId, username, password);

        MqttConnAckMessage ackMessage;
        if (authSuccess) {
            // 检查是否存在旧会话
            boolean sessionPresent = sessionManager.hasSession(clientId);
            
            // 如果cleanSession=true，清除旧会话
            if (cleanSession) {
                sessionManager.removeSession(clientId);
                sessionPresent = false;
            }
            
            // 保存新会话
            sessionManager.addSession(clientId, ctx.channel());
            
            // 设备上线
            deviceMessageService.deviceOnline(clientId, ctx.channel().remoteAddress().toString());

            // 返回连接成功，sessionPresent根据实际情况返回
            ackMessage = (MqttConnAckMessage) MqttMessageFactory.newMessage(
                    new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                    new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_ACCEPTED, sessionPresent && !cleanSession),
                    null
            );
            log.info("设备认证成功 - ClientId: {}, SessionPresent: {}", clientId, sessionPresent && !cleanSession);
        } else {
            // 返回认证失败
            ackMessage = (MqttConnAckMessage) MqttMessageFactory.newMessage(
                    new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                    new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD, false),
                    null
            );
            log.warn("设备认证失败 - ClientId: {}", clientId);
        }

        ctx.writeAndFlush(ackMessage);
    }

    /**
     * 处理消息发布
     */
    private void handlePublish(ChannelHandlerContext ctx, MqttPublishMessage msg) {
        String topic = msg.variableHeader().topicName();
        byte[] payload = new byte[msg.payload().readableBytes()];
        msg.payload().readBytes(payload);
        String message = new String(payload);

        log.debug("📨 MQTT收到设备消息 - Topic: {}, Payload长度: {} bytes", topic, payload.length);
        if (log.isDebugEnabled()) {
            log.debug("📨 消息内容: {}", message);
        }

        // 处理设备消息
        deviceMessageService.handleDeviceMessage(topic, message);

        // QoS 1需要返回PUBACK
        if (msg.fixedHeader().qosLevel() == MqttQoS.AT_LEAST_ONCE) {
            MqttPubAckMessage pubAckMessage = (MqttPubAckMessage) MqttMessageFactory.newMessage(
                    new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                    MqttMessageIdVariableHeader.from(msg.variableHeader().packetId()),
                    null
            );
            ctx.writeAndFlush(pubAckMessage);
        }
    }

    /**
     * 处理PUBACK
     */
    private void handlePubAck(ChannelHandlerContext ctx, MqttMessage msg) {
        log.debug("收到PUBACK确认");
    }

    /**
     * 处理订阅请求
     */
    private void handleSubscribe(ChannelHandlerContext ctx, MqttSubscribeMessage msg) {
        String clientId = sessionManager.getClientId(ctx.channel());
        
        log.debug("设备订阅请求 - ClientId: {}, Topics: {}", clientId, msg.payload().topicSubscriptions());

        // 返回订阅确认
        MqttSubAckMessage subAckMessage = (MqttSubAckMessage) MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.SUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                MqttMessageIdVariableHeader.from(msg.variableHeader().messageId()),
                new MqttSubAckPayload(MqttQoS.AT_LEAST_ONCE.value())
        );
        ctx.writeAndFlush(subAckMessage);
    }

    /**
     * 处理取消订阅请求
     */
    private void handleUnSubscribe(ChannelHandlerContext ctx, MqttUnsubscribeMessage msg) {
        log.debug("设备取消订阅请求 - Topics: {}", msg.payload().topics());

        // 返回取消订阅确认
        MqttUnsubAckMessage unsubAckMessage = (MqttUnsubAckMessage) MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.UNSUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                MqttMessageIdVariableHeader.from(msg.variableHeader().messageId()),
                null
        );
        ctx.writeAndFlush(unsubAckMessage);
    }

    /**
     * 处理心跳请求
     */
    private void handlePingReq(ChannelHandlerContext ctx) {
        log.debug("收到设备心跳");
        
        // 返回心跳响应
        MqttMessage pingRespMessage = MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false, 0),
                null,
                null
        );
        ctx.writeAndFlush(pingRespMessage);
    }

    /**
     * 处理断开连接
     */
    private void handleDisconnect(ChannelHandlerContext ctx) {
        String clientId = sessionManager.getClientId(ctx.channel());
        log.debug("设备主动断开连接 - ClientId: {}", clientId);
        
        if (StrUtil.isNotBlank(clientId)) {
            deviceMessageService.deviceOffline(clientId);
            sessionManager.removeSession(clientId);
        }
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                String clientId = sessionManager.getClientId(ctx.channel());
                log.warn("设备心跳超时 - ClientId: {}", clientId);
                
                if (StrUtil.isNotBlank(clientId)) {
                    deviceMessageService.deviceOffline(clientId);
                    sessionManager.removeSession(clientId);
                }
                ctx.close();
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        String clientId = sessionManager.getClientId(ctx.channel());
        log.error("MQTT处理异常 - ClientId: {}, 错误: {}", clientId, cause.getMessage(), cause);
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        String clientId = sessionManager.getClientId(ctx.channel());
        if (StrUtil.isNotBlank(clientId)) {
            log.debug("设备连接断开 - ClientId: {}", clientId);
            deviceMessageService.deviceOffline(clientId);
            sessionManager.removeSession(clientId);
        }
    }
}
