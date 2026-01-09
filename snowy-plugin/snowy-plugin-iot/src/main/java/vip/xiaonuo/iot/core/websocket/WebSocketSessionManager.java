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

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket会话管理器
 *
 * @author yubaoshan
 * @date 2024/12/11 07:50
 **/
@Slf4j
@Component
public class WebSocketSessionManager {

    /** DeviceKey -> Channel */
    private final Map<String, Channel> sessionMap = new ConcurrentHashMap<>();
    
    /** Channel -> DeviceKey */
    private final Map<Channel, String> channelMap = new ConcurrentHashMap<>();

    /**
     * 添加会话
     */
    public void addSession(String deviceKey, Channel channel) {
        // 如果该deviceKey已存在会话，先关闭旧连接
        Channel oldChannel = sessionMap.get(deviceKey);
        if (oldChannel != null && oldChannel.isActive()) {
            log.warn("WebSocket设备重复连接，关闭旧连接 - DeviceKey: {}", deviceKey);
            channelMap.remove(oldChannel);
            oldChannel.close();
        }
        
        sessionMap.put(deviceKey, channel);
        channelMap.put(channel, deviceKey);
        log.debug("添加WebSocket会话 - DeviceKey: {}", deviceKey);
    }

    /**
     * 移除会话
     */
    public void removeSession(String deviceKey) {
        Channel channel = sessionMap.remove(deviceKey);
        if (channel != null) {
            channelMap.remove(channel);
            log.debug("移除WebSocket会话 - DeviceKey: {}", deviceKey);
        }
    }

    /**
     * 获取Channel
     */
    public Channel getChannel(String deviceKey) {
        return sessionMap.get(deviceKey);
    }

    /**
     * 获取DeviceKey
     */
    public String getDeviceKey(Channel channel) {
        return channelMap.get(channel);
    }

    /**
     * 判断设备是否在线
     */
    public boolean isOnline(String deviceKey) {
        Channel channel = sessionMap.get(deviceKey);
        return channel != null && channel.isActive();
    }

    /**
     * 获取在线设备数
     */
    public int getOnlineCount() {
        return (int) sessionMap.values().stream().filter(Channel::isActive).count();
    }

    /**
     * 向设备发送消息
     */
    public boolean sendToDevice(String deviceKey, String message) {
        Channel channel = sessionMap.get(deviceKey);
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(new TextWebSocketFrame(message));
            return true;
        }
        return false;
    }

    /**
     * 广播消息到所有设备
     */
    public void broadcast(String message) {
        sessionMap.values().forEach(channel -> {
            if (channel.isActive()) {
                channel.writeAndFlush(new TextWebSocketFrame(message));
            }
        });
    }
}
