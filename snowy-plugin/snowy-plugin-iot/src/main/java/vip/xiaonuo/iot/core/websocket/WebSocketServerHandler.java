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
package vip.xiaonuo.iot.core.websocket;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vip.xiaonuo.iot.core.message.DeviceMessageService;

/**
 * WebSocket消息处理器
 *
 * @author yubaoshan
 * @date 2024/12/11 07:50
 **/
@Slf4j
@Component
@io.netty.channel.ChannelHandler.Sharable
public class WebSocketServerHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    @Resource
    private DeviceMessageService deviceMessageService;

    @Resource
    private WebSocketSessionManager sessionManager;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("WebSocket连接建立 - {}", ctx.channel().remoteAddress());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (frame instanceof TextWebSocketFrame) {
            String message = ((TextWebSocketFrame) frame).text();
            handleTextMessage(ctx, message);
        }
    }

    /**
     * 处理文本消息
     */
    private void handleTextMessage(ChannelHandlerContext ctx, String message) {
        try {
            JSONObject json = JSONUtil.parseObj(message);
            String type = json.getStr("type");

            switch (type) {
                case "auth":
                    handleAuth(ctx, json);
                    break;
                case "heartbeat":
                    handleHeartbeat(ctx);
                    break;
                case "publish":
                    handlePublish(ctx, json);
                    break;
                default:
                    log.warn("未知消息类型: {}", type);
            }
        } catch (Exception e) {
            log.error("处理WebSocket消息异常: {}", message, e);
            sendErrorMessage(ctx, "消息处理失败: " + e.getMessage());
        }
    }

    /**
     * 处理认证
     */
    private void handleAuth(ChannelHandlerContext ctx, JSONObject json) {
        String deviceKey = json.getStr("deviceKey");
        String deviceSecret = json.getStr("deviceSecret");

        // 设备认证
        boolean authSuccess = deviceMessageService.authenticate(deviceKey, deviceKey, deviceSecret);

        if (authSuccess) {
            // 保存会话
            sessionManager.addSession(deviceKey, ctx.channel());
            
            // 设备上线
            deviceMessageService.deviceOnline(deviceKey, ctx.channel().remoteAddress().toString());

            // 返回认证成功
            JSONObject response = new JSONObject();
            response.set("type", "auth");
            response.set("code", 200);
            response.set("message", "认证成功");
            ctx.writeAndFlush(new TextWebSocketFrame(response.toString()));
            
            log.info("WebSocket设备认证成功 - DeviceKey: {}", deviceKey);
        } else {
            // 返回认证失败
            JSONObject response = new JSONObject();
            response.set("type", "auth");
            response.set("code", 401);
            response.set("message", "认证失败");
            ctx.writeAndFlush(new TextWebSocketFrame(response.toString()));
            
            log.warn("WebSocket设备认证失败 - DeviceKey: {}", deviceKey);
            ctx.close();
        }
    }

    /**
     * 处理心跳
     */
    private void handleHeartbeat(ChannelHandlerContext ctx) {
        JSONObject response = new JSONObject();
        response.set("type", "heartbeat");
        response.set("timestamp", System.currentTimeMillis());
        ctx.writeAndFlush(new TextWebSocketFrame(response.toString()));
    }

    /**
     * 处理消息发布
     */
    private void handlePublish(ChannelHandlerContext ctx, JSONObject json) {
        String topic = json.getStr("topic");
        String payload = json.getStr("payload");

        // 处理设备消息
        deviceMessageService.handleDeviceMessage(topic, payload);

        // 返回发布成功
        JSONObject response = new JSONObject();
        response.set("type", "publish");
        response.set("code", 200);
        response.set("message", "发布成功");
        ctx.writeAndFlush(new TextWebSocketFrame(response.toString()));
    }

    /**
     * 发送错误消息
     */
    private void sendErrorMessage(ChannelHandlerContext ctx, String error) {
        JSONObject response = new JSONObject();
        response.set("type", "error");
        response.set("code", 500);
        response.set("message", error);
        ctx.writeAndFlush(new TextWebSocketFrame(response.toString()));
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                String deviceKey = sessionManager.getDeviceKey(ctx.channel());
                log.warn("WebSocket心跳超时 - DeviceKey: {}", deviceKey);
                
                if (StrUtil.isNotBlank(deviceKey)) {
                    deviceMessageService.deviceOffline(deviceKey);
                    sessionManager.removeSession(deviceKey);
                }
                ctx.close();
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("WebSocket处理异常", cause);
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        String deviceKey = sessionManager.getDeviceKey(ctx.channel());
        if (StrUtil.isNotBlank(deviceKey)) {
            log.info("WebSocket连接断开 - DeviceKey: {}", deviceKey);
            deviceMessageService.deviceOffline(deviceKey);
            sessionManager.removeSession(deviceKey);
        }
    }
}
