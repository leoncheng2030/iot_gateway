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
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vip.xiaonuo.iot.core.config.NettyThreadPoolConfig;
import vip.xiaonuo.iot.core.protocol.ProtocolServer;
import vip.xiaonuo.iot.core.websocket.WebSocketServerHandler;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 动态WebSocket服务器
 *
 * @author jetox
 * @date 2025/12/11 10:40
 **/
@Slf4j
public class DynamicWebSocketServer implements ProtocolServer {

    private final WebSocketServerHandler webSocketServerHandler;
    
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private Integer port;

    public DynamicWebSocketServer(WebSocketServerHandler webSocketServerHandler) {
        this.webSocketServerHandler = webSocketServerHandler;
    }

    @Override
    public void start(Integer port, Map<String, Object> config) {
        this.port = port;
        
        // 从配置中获取参数
        String path = getConfigString(config, "path", "/iot/websocket");
        int heartbeatTimeout = getConfigInt(config, "heartbeatTimeout", 120);

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
                            
                            // HTTP编解码器
                            pipeline.addLast(new HttpServerCodec());
                            pipeline.addLast(new ChunkedWriteHandler());
                            pipeline.addLast(new HttpObjectAggregator(65536));
                            
                            // WebSocket协议处理器
                            pipeline.addLast(new WebSocketServerProtocolHandler(
                                    path,
                                    null,
                                    true,
                                    65536
                            ));
                            
                            // 心跳检测
                            pipeline.addLast(new IdleStateHandler(
                                    heartbeatTimeout,
                                    0,
                                    0,
                                    TimeUnit.SECONDS
                            ));
                            
                            // 业务处理器
                            pipeline.addLast(webSocketServerHandler);
                        }
                    });

            ChannelFuture future = bootstrap.bind(port).sync();
            serverChannel = future.channel();
            log.info(">>> 动态WebSocket服务器启动成功，监听端口: {}, 路径: {}", port, path);

        } catch (Exception e) {
            stop();
            throw new RuntimeException("WebSocket服务器启动失败: " + e.getMessage(), e);
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
        log.info(">>> 动态WebSocket服务器已关闭，端口: {}", port);
    }

    @Override
    public Integer getPort() {
        return port;
    }

    @Override
    public String getProtocolType() {
        return "WEBSOCKET";
    }

    private int getConfigInt(Map<String, Object> config, String key, int defaultValue) {
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

    private String getConfigString(Map<String, Object> config, String key, String defaultValue) {
        Object value = config.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * 工厂类，用于创建DynamicWebSocketServer实例
     */
    @Component
    public static class Factory {
        
        @Resource
        private WebSocketServerHandler webSocketServerHandler;

        public DynamicWebSocketServer create() {
            return new DynamicWebSocketServer(webSocketServerHandler);
        }
    }
}
