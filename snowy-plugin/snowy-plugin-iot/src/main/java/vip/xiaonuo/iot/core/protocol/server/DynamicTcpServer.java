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
package vip.xiaonuo.iot.core.protocol.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vip.xiaonuo.iot.core.config.NettyThreadPoolConfig;
import vip.xiaonuo.iot.core.protocol.ProtocolServer;
import vip.xiaonuo.iot.core.protocol.annotation.Protocol;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 动态TCP协议服务器
 *
 * @author jetox
 * @date 2025/12/11
 */
@Slf4j
@Component
@Protocol(type = "TCP", name = "TCP协议", description = "动态TCP协议服务器，支持自定义编解码")
public class DynamicTcpServer implements ProtocolServer {

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private Integer port;

    @Override
    public void start(Integer port, Map<String, Object> config) {
        this.port = port;

        // 获取配置参数
        int maxFrameLength = getConfigInt(config, "maxFrameLength", 1024);
        int readTimeout = getConfigInt(config, "readTimeout", 0);
        int writeTimeout = getConfigInt(config, "writeTimeout", 0);
        int idleTimeout = getConfigInt(config, "idleTimeout", 0);

        bossGroup = new NioEventLoopGroup(NettyThreadPoolConfig.getBossThreads());
        workerGroup = new NioEventLoopGroup(NettyThreadPoolConfig.getWorkerThreads());

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            
                            if (readTimeout > 0 || writeTimeout > 0 || idleTimeout > 0) {
                                pipeline.addLast(new IdleStateHandler(readTimeout, writeTimeout, idleTimeout, TimeUnit.SECONDS));
                            }
                            
                            pipeline.addLast(new TcpServerHandler());
                        }
                    });

            ChannelFuture future = bootstrap.bind(port).sync();
            serverChannel = future.channel();
            
            log.info(">>> 动态TCP服务器启动成功，监听端口: {}", port);
            
        } catch (Exception e) {
            log.error(">>> 动态TCP服务器启动失败", e);
            stop();
            throw new RuntimeException("TCP服务器启动失败: " + e.getMessage());
        }
    }

    @Override
    public void stop() {
        try {
            if (serverChannel != null) {
                serverChannel.close().sync();
            }
        } catch (Exception e) {
            log.error(">>> TCP服务器Channel关闭失败", e);
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
        
        log.info(">>> 动态TCP服务器已停止，端口: {}", port);
    }

    @Override
    public Integer getPort() {
        return port;
    }

    @Override
    public String getProtocolType() {
        return "TCP";
    }

    private int getConfigInt(Map<String, Object> config, String key, int defaultValue) {
        if (config == null || !config.containsKey(key)) {
            return defaultValue;
        }
        Object value = config.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            log.warn(">>> 配置项 {} 转换失败，使用默认值 {}", key, defaultValue);
            return defaultValue;
        }
    }

    @ChannelHandler.Sharable
    static class TcpServerHandler extends SimpleChannelInboundHandler<Object> {

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            log.info(">>> ========================================");
            log.info(">>> TCP客户端连接成功");
            log.info(">>> 远程地址: {}", ctx.channel().remoteAddress());
            log.info(">>> 本地地址: {}", ctx.channel().localAddress());
            log.info(">>> ========================================");
            
            try {
                Thread.sleep(100);
                
                sendMessage(ctx, "OK\n");
                Thread.sleep(50);
                
                sendMessage(ctx, "REG_OK\n");
                Thread.sleep(50);
                
                sendMessage(ctx, "HANDSHAKE\n");
                Thread.sleep(50);
                
                sendMessage(ctx, "ACK\n");
                
                log.info(">>> 已发送握手序列: OK, REG_OK, HANDSHAKE, ACK");
            } catch (Exception e) {
                log.warn(">>> 发送握手消息失败", e);
            }
        }
        
        private void sendMessage(ChannelHandlerContext ctx, String msg) {
            ByteBuf buf = ctx.alloc().buffer();
            buf.writeBytes(msg.getBytes(StandardCharsets.UTF_8));
            ctx.writeAndFlush(buf);
            log.info(">>> 发送: {}", msg.trim());
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
            log.info(">>> ========================================");
            log.info(">>> 收到TCP数据");
            log.info(">>> 来自: {}", ctx.channel().remoteAddress());
            
            if (msg instanceof ByteBuf) {
                ByteBuf buf = (ByteBuf) msg;
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                
                log.info(">>> 数据类型: ByteBuf");
                log.info(">>> 数据长度: {} 字节", bytes.length);
                log.info(">>> 数据内容(HEX): {}", bytesToHex(bytes));
                
                String asciiStr = new String(bytes, StandardCharsets.UTF_8);
                if (isPrintable(asciiStr)) {
                    log.info(">>> 数据内容(ASCII): {}", asciiStr.trim());
                } else {
                    log.info(">>> 数据内容(ASCII): [不可打印字符]");
                }
                
                ByteBuf response = ctx.alloc().buffer();
                response.writeBytes("OK\n".getBytes(StandardCharsets.UTF_8));
                ctx.writeAndFlush(response);
                log.info(">>> 已回复: OK");
            }
            else {
                log.info(">>> 数据类型: {}", msg.getClass().getName());
                log.info(">>> 数据内容: {}", msg);
            }
            
            log.info(">>> ========================================");
        }
        
        private String bytesToHex(byte[] bytes) {
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02X ", b));
            }
            return sb.toString().trim();
        }
        
        private boolean isPrintable(String str) {
            for (char c : str.toCharArray()) {
                if (c < 32 && c != '\n' && c != '\r' && c != '\t') {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            log.info(">>> TCP客户端断开: {}", ctx.channel().remoteAddress());
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent event = (IdleStateEvent) evt;
                log.warn(">>> TCP连接超时: {} 类型: {}", ctx.channel().remoteAddress(), event.state());
                ctx.close();
            } else {
                super.userEventTriggered(ctx, evt);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error(">>> TCP连接异常: {}", ctx.channel().remoteAddress(), cause);
            ctx.close();
        }
    }
}
