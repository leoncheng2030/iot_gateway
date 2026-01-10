package vip.xiaonuo.iot.core.protocol.impl;

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
        int readTimeout = getConfigInt(config, "readTimeout", 0);  // 禁用读超时，设备被动响应模式
        int writeTimeout = getConfigInt(config, "writeTimeout", 0);  // 禁用写超时
        int idleTimeout = getConfigInt(config, "idleTimeout", 0);  // 禁用空闲超时

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
                            
                            // 超时处理（0表示禁用）
                            if (readTimeout > 0 || writeTimeout > 0 || idleTimeout > 0) {
                                pipeline.addLast(new IdleStateHandler(readTimeout, writeTimeout, idleTimeout, TimeUnit.SECONDS));
                            }
                            
                            // 不使用任何编解码器，直接处理原始ByteBuf，避免数据丢失
                            // 业务处理器会同时处理文本和二进制数据
                            
                            // 业务处理器
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

    /**
     * 获取配置中的整数值
     */
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

    /**
     * TCP业务处理器
     */
    @ChannelHandler.Sharable
    static class TcpServerHandler extends SimpleChannelInboundHandler<Object> {

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            log.info(">>> ========================================");
            log.info(">>> TCP客户端连接成功");
            log.info(">>> 远程地址: {}", ctx.channel().remoteAddress());
            log.info(">>> 本地地址: {}", ctx.channel().localAddress());
            log.info(">>> ========================================");
            
            // 尝试发送多种握手消息，触发设备开始通信
            // 设备可能需要特定指令才开始数据上报
            try {
                // 延迟100ms，确保连接稳定
                Thread.sleep(100);
                
                // 尝试1: 简单的OK响应
                sendMessage(ctx, "OK\n");
                Thread.sleep(50);
                
                // 尝试2: 注册确认
                sendMessage(ctx, "REG_OK\n");
                Thread.sleep(50);
                
                // 尝试3: 握手确认
                sendMessage(ctx, "HANDSHAKE\n");
                Thread.sleep(50);
                
                // 尝试4: 心跳响应（模拟对reg001的响应）
                sendMessage(ctx, "ACK\n");
                
                log.info(">>> 已发送握手序列: OK, REG_OK, HANDSHAKE, ACK");
            } catch (Exception e) {
                log.warn(">>> 发送握手消息失败", e);
            }
        }
        
        /**
         * 发送文本消息
         */
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
            
            // 统一处理为ByteBuf
            if (msg instanceof ByteBuf) {
                ByteBuf buf = (ByteBuf) msg;
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                
                log.info(">>> 数据类型: ByteBuf");
                log.info(">>> 数据长度: {} 字节", bytes.length);
                log.info(">>> 数据内容(HEX): {}", bytesToHex(bytes));
                
                // 尝试转换为ASCII字符串
                String asciiStr = new String(bytes, StandardCharsets.UTF_8);
                if (isPrintable(asciiStr)) {
                    log.info(">>> 数据内容(ASCII): {}", asciiStr.trim());
                } else {
                    log.info(">>> 数据内容(ASCII): [不可打印字符]");
                }
                
                // 回复确认消息
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
        
        /**
         * 字节数组转十六进制字符串
         */
        private String bytesToHex(byte[] bytes) {
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02X ", b));
            }
            return sb.toString().trim();
        }
        
        /**
         * 检查字符串是否可打印
         */
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
