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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket帧转MQTT消息处理器
 * 将WebSocket二进制帧中的MQTT消息提取出来，传递给MQTT解码器
 *
 * @author yubaoshan
 * @date 2026/01/08
 **/
@Slf4j
public class MqttWebSocketFrameHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof BinaryWebSocketFrame) {
            // WebSocket二进制帧，提取MQTT消息
            BinaryWebSocketFrame frame = (BinaryWebSocketFrame) msg;
            ByteBuf content = frame.content();
            
            // 保留引用计数，传递给下一个处理器
            content.retain();
            
            // 传递ByteBuf给MQTT解码器
            ctx.fireChannelRead(content);
            
            // 释放原始WebSocket帧
            frame.release();
        } else if (msg instanceof WebSocketFrame) {
            // 其他WebSocket帧类型，跳过
            log.debug("收到非二进制WebSocket帧，类型: {}", msg.getClass().getSimpleName());
            ((WebSocketFrame) msg).release();
        } else {
            // 非WebSocket帧，直接传递（可能是普通TCP连接）
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("WebSocket MQTT帧处理异常", cause);
        ctx.close();
    }
}
