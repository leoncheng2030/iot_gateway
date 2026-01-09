/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 */
package vip.xiaonuo.iot.core.driver.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;
import vip.xiaonuo.iot.core.config.DriverConfigField;
import vip.xiaonuo.iot.core.driver.AbstractDeviceDriver;
import vip.xiaonuo.iot.core.driver.DriverConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DTU网关驱动 - 串口转网络
 * 
 * 功能：
 * 1. TCP服务器接收DTU设备数据
 * 2. 设备注册与管理
 * 3. 数据透传与解析
 * 4. 心跳保活机制
 *
 * @author yubaoshan
 * @date 2025/12/13
 */
@Slf4j
public class DtuGatewayDriver extends AbstractDeviceDriver {

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    
    /** 设备连接通道映射 deviceKey -> Channel */
    private final Map<String, Channel> deviceChannels = new ConcurrentHashMap<>();

    public DtuGatewayDriver(DriverConfig config) {
        super(config);
    }

    @Override
    public String getDriverType() {
        return "DTU_GATEWAY";
    }

    @Override
    public String getDriverName() {
        return "DTU网关驱动";
    }

    @Override
    protected void doStart() throws Exception {
        Integer portConfig = config.getInteger("port");
        int port = portConfig != null ? portConfig : 9000;

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                            .addLast(new StringDecoder())
                            .addLast(new StringEncoder())
                            .addLast(new DtuMessageHandler());
                    }
                });

        ChannelFuture future = bootstrap.bind(port).sync();
        serverChannel = future.channel();
        log.info("DTU网关驱动启动成功, 监听端口: {}", port);
    }

    @Override
    protected void doStop() throws Exception {
        if (serverChannel != null) {
            serverChannel.close().sync();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        deviceChannels.clear();
    }

    @Override
    public JSONObject readData(String deviceKey, JSONObject params) throws Exception {
        // DTU是被动接收数据，不支持主动读取
        throw new UnsupportedOperationException("DTU网关不支持主动读取数据");
    }

    @Override
    public boolean writeData(String deviceKey, JSONObject data) throws Exception {
        Channel channel = deviceChannels.get(deviceKey);
        if (channel == null || !channel.isActive()) {
            log.warn("设备 [{}] 未连接或通道已关闭", deviceKey);
            return false;
        }

        String message = data.toString();
        channel.writeAndFlush(message);
        log.info("向设备 [{}] 下发指令: {}", deviceKey, message);
        return true;
    }

    @Override
    public boolean isDeviceOnline(String deviceKey) {
        Channel channel = deviceChannels.get(deviceKey);
        return channel != null && channel.isActive();
    }

    /**
     * DTU消息处理器
     */
    private class DtuMessageHandler extends SimpleChannelInboundHandler<String> {

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            log.info("DTU设备连接: {}", ctx.channel().remoteAddress());
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String msg) {
            log.debug("收到DTU数据: {}", msg);
            
            try {
                // 解析设备标识（需根据实际协议调整）
                JSONObject data = JSONUtil.parseObj(msg);
                String deviceKey = data.getStr("deviceKey");
                
                if (deviceKey != null) {
                    // 注册设备通道
                    deviceChannels.put(deviceKey, ctx.channel());
                    
                    // TODO: 调用设备数据处理服务
                    // deviceDataHandler.handleData(deviceKey, data);
                }
            } catch (Exception e) {
                log.error("DTU数据处理异常", e);
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            // 设备断开，移除通道
            deviceChannels.entrySet().removeIf(entry -> entry.getValue() == ctx.channel());
            log.info("DTU设备断开: {}", ctx.channel().remoteAddress());
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error("DTU通道异常", cause);
            ctx.close();
        }
    }

	@Override
	public List<DriverConfigField> getConfigFields() {
		return getStaticConfigFields();
	}

	/**
	 * 获取驱动配置字段（静态方法）
	 */
    public static List<DriverConfigField> getStaticConfigFields() {
        List<DriverConfigField> fields = new ArrayList<>();

        fields.add(new DriverConfigField(
                "port", "监听端口", "number", 9000,
                1024, 65535, null,
                "请输入监听端口", "DTU设备连接的服务端口", 24,
                "driver", true
        ));

        return fields;
    }
}
