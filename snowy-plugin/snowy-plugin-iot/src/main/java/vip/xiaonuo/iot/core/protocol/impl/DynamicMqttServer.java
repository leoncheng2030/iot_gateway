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
package vip.xiaonuo.iot.core.protocol.impl;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vip.xiaonuo.iot.core.config.NettyThreadPoolConfig;
import vip.xiaonuo.iot.core.mqtt.MqttServerHandler;
import vip.xiaonuo.iot.core.mqtt.MqttWebSocketFrameHandler;
import vip.xiaonuo.iot.core.protocol.ProtocolServer;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 动态MQTT服务器
 *
 * @author jetox
 * @date 2025/12/11 10:40
 **/
@Slf4j
public class DynamicMqttServer implements ProtocolServer {

    private final MqttServerHandler mqttServerHandler;
    
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private Integer port;

    public DynamicMqttServer(MqttServerHandler mqttServerHandler) {
        this.mqttServerHandler = mqttServerHandler;
    }

    @Override
    public void start(Integer port, Map<String, Object> config) {
        this.port = port;
        
        // 从配置中获取参数，如果没有则使用默认值
        int keepAlive = getConfigInt(config, "keepAlive", 60);
        int maxMessageSize = getConfigInt(config, "maxMessageSize", 8192);
        boolean enableWebSocket = getConfigBoolean(config, "enableWebSocket", false);
        String wsPath = getConfigString(config, "wsPath", "/mqtt");

        try {
            bossGroup = new NioEventLoopGroup(NettyThreadPoolConfig.getBossThreads());
            workerGroup = new NioEventLoopGroup(NettyThreadPoolConfig.getWorkerThreads());

            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            
                            // 心跳检测
                            pipeline.addLast(new IdleStateHandler(
                                    keepAlive * 2,
                                    0,
                                    0,
                                    TimeUnit.SECONDS
                            ));
                            
                            // MQTT编解码器（纯TCP模式）
                            pipeline.addLast("decoder", new MqttDecoder(maxMessageSize));
                            pipeline.addLast("encoder", MqttEncoder.INSTANCE);
                            
                            // MQTT业务处理器
                            pipeline.addLast("handler", mqttServerHandler);
                        }
                    });

            ChannelFuture future = bootstrap.bind(port).sync();
            serverChannel = future.channel();
            log.info(">>> 动态MQTT服务器启动成功，监听端口: {}", port);

        } catch (Exception e) {
            stop();
            throw new RuntimeException("MQTT服务器启动失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void stop() {
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully(
                NettyThreadPoolConfig.getShutdownQuietPeriod(),
                NettyThreadPoolConfig.getShutdownTimeout(),
                TimeUnit.SECONDS
            );
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully(
                NettyThreadPoolConfig.getShutdownQuietPeriod(),
                NettyThreadPoolConfig.getShutdownTimeout(),
                TimeUnit.SECONDS
            );
        }
        log.info(">>> 动态MQTT服务器已关闭，端口: {}", port);
    }

    @Override
    public Integer getPort() {
        return port;
    }

    @Override
    public String getProtocolType() {
        return "MQTT";
    }

    private int getConfigInt(Map<String, Object> config, String key, int defaultValue) {
        if (config == null) return defaultValue;
        Object value = config.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private boolean getConfigBoolean(Map<String, Object> config, String key, boolean defaultValue) {
        if (config == null) return defaultValue;
        Object value = config.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }

    private String getConfigString(Map<String, Object> config, String key, String defaultValue) {
        if (config == null) return defaultValue;
        Object value = config.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * 工厂类，用于创建DynamicMqttServer实例
     */
    @Component
    public static class Factory {
        
        @Resource
        private MqttServerHandler mqttServerHandler;

        public DynamicMqttServer create() {
            return new DynamicMqttServer(mqttServerHandler);
        }
    }
}
