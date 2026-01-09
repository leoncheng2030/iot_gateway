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

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MQTT会话管理器
 *
 * @author yubaoshan
 * @date 2024/12/11 07:00
 **/
@Slf4j
@Component
public class MqttSessionManager {

    /** ClientId -> Channel */
    private final Map<String, Channel> sessionMap = new ConcurrentHashMap<>();
    
    /** Channel -> ClientId */
    private final Map<Channel, String> channelMap = new ConcurrentHashMap<>();

    /**
     * 添加会话
     */
    public void addSession(String clientId, Channel channel) {
        // 如果该clientId已存在会话，先关闭旧连接
        Channel oldChannel = sessionMap.get(clientId);
        if (oldChannel != null && oldChannel.isActive()) {
            log.warn("设备重复连接，关闭旧连接 - ClientId: {}", clientId);
            // 先从map中移除，再关闭，避免channelInactive事件误处理
            sessionMap.remove(clientId);
            channelMap.remove(oldChannel);
            oldChannel.close();
        }
        
        sessionMap.put(clientId, channel);
        channelMap.put(channel, clientId);
        log.debug("添加会话 - ClientId: {}, Channel: {}", clientId, channel.id());
    }

    /**
     * 移除会话
     */
    public void removeSession(String clientId) {
        Channel channel = sessionMap.remove(clientId);
        if (channel != null) {
            channelMap.remove(channel);
            log.debug("移除会话 - ClientId: {}", clientId);
        }
    }

    /**
     * 获取Channel
     */
    public Channel getChannel(String clientId) {
        return sessionMap.get(clientId);
    }

    /**
     * 获取ClientId
     */
    public String getClientId(Channel channel) {
        return channelMap.get(channel);
    }

    /**
     * 判断设备是否在线
     */
    public boolean isOnline(String clientId) {
        Channel channel = sessionMap.get(clientId);
        return channel != null && channel.isActive();
    }

    /**
     * 检查是否存在会话（用于MQTT CONNACK的sessionPresent标志）
     */
    public boolean hasSession(String clientId) {
        return sessionMap.containsKey(clientId);
    }

    /**
     * 获取在线设备数
     */
    public int getOnlineCount() {
        return (int) sessionMap.values().stream().filter(Channel::isActive).count();
    }
}
