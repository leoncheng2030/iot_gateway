/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 */
package vip.xiaonuo.iot.core.driver.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;
import vip.xiaonuo.iot.core.config.DriverConfigField;
import vip.xiaonuo.iot.core.driver.AbstractDeviceDriver;
import vip.xiaonuo.iot.core.driver.DriverConfig;
import vip.xiaonuo.iot.core.driver.annotation.Driver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TCP直连设备驱动
 * 
 * 功能：
 * 1. TCP客户端主动连接设备
 * 2. 数据读写
 * 3. 心跳保活
 * 4. 断线重连
 *
 * @author yubaoshan
 * @date 2025/12/13
 */
@Slf4j
@Driver(type = "TCP_DIRECT", name = "TCP直连驱动", description = "TCP直连设备驱动")
public class TcpDirectDriver extends AbstractDeviceDriver {

    private EventLoopGroup workerGroup;
    
    /** 设备连接通道映射 deviceKey -> Channel */
    private final Map<String, Channel> deviceChannels = new ConcurrentHashMap<>();
    
    /** 设备配置映射 deviceKey -> DeviceConfig */
    private final Map<String, DeviceConfig> deviceConfigs = new ConcurrentHashMap<>();

    public TcpDirectDriver(DriverConfig config) {
        super(config);
    }

    @Override
    public String getDriverType() {
        return "TCP_DIRECT";
    }

    @Override
    public String getDriverName() {
        return "TCP直连设备驱动";
    }

    @Override
    protected void doStart() throws Exception {
        workerGroup = new NioEventLoopGroup();
        log.info("TCP直连设备驱动初始化完成");
    }

    @Override
    protected void doStop() throws Exception {
        // 关闭所有设备连接
        for (Channel channel : deviceChannels.values()) {
            if (channel.isActive()) {
                channel.close();
            }
        }
        
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        
        deviceChannels.clear();
        deviceConfigs.clear();
    }

    /**
     * 连接设备
     */
    public void connectDevice(String deviceKey, String host, int port) throws Exception {
        if (deviceChannels.containsKey(deviceKey)) {
            log.warn("设备 [{}] 已连接", deviceKey);
            return;
        }

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                            .addLast(new StringDecoder())
                            .addLast(new StringEncoder())
                            .addLast(new TcpMessageHandler(deviceKey));
                    }
                });

        ChannelFuture future = bootstrap.connect(host, port).sync();
        deviceChannels.put(deviceKey, future.channel());
        
        DeviceConfig deviceConfig = new DeviceConfig(host, port);
        deviceConfigs.put(deviceKey, deviceConfig);
        
        log.info("TCP设备 [{}] 连接成功: {}:{}", deviceKey, host, port);
    }

    /**
     * 断开设备
     */
    public void disconnectDevice(String deviceKey) {
        Channel channel = deviceChannels.remove(deviceKey);
        if (channel != null && channel.isActive()) {
            channel.close();
            log.info("TCP设备 [{}] 已断开", deviceKey);
        }
        deviceConfigs.remove(deviceKey);
    }

    @Override
    public JSONObject readData(String deviceKey, JSONObject params) throws Exception {
        Channel channel = deviceChannels.get(deviceKey);
        if (channel == null || !channel.isActive()) {
            throw new IllegalStateException("设备未连接: " + deviceKey);
        }

        // 发送读取指令
        String readCommand = params.getStr("command");
        channel.writeAndFlush(readCommand);
        
        // TODO: 实现同步等待响应
        return new JSONObject();
    }

    @Override
    public boolean writeData(String deviceKey, JSONObject data) throws Exception {
        Channel channel = deviceChannels.get(deviceKey);
        if (channel == null || !channel.isActive()) {
            log.warn("设备 [{}] 未连接", deviceKey);
            return false;
        }

        String message = data.toString();
        channel.writeAndFlush(message);
        log.info("向TCP设备 [{}] 写入数据: {}", deviceKey, message);
        return true;
    }

    @Override
    public boolean isDeviceOnline(String deviceKey) {
        Channel channel = deviceChannels.get(deviceKey);
        return channel != null && channel.isActive();
    }

    /**
     * TCP消息处理器
     */
    private class TcpMessageHandler extends SimpleChannelInboundHandler<String> {
        
        private final String deviceKey;

        public TcpMessageHandler(String deviceKey) {
            this.deviceKey = deviceKey;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String msg) {
            log.debug("收到TCP设备 [{}] 数据: {}", deviceKey, msg);
            
            try {
                JSONObject data = JSONUtil.parseObj(msg);
                // TODO: 调用设备数据处理服务
                // deviceDataHandler.handleData(deviceKey, data);
            } catch (Exception e) {
                log.error("TCP数据处理异常", e);
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            log.warn("TCP设备 [{}] 连接断开", deviceKey);
            deviceChannels.remove(deviceKey);
            
            // TODO: 实现自动重连
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error("TCP通道异常: {}", deviceKey, cause);
            ctx.close();
        }
    }

    /**
     * 设备配置
     */
    private static class DeviceConfig {
        private final String host;
        private final int port;

        public DeviceConfig(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }
    }

	@Override
	public List<DriverConfigField> getConfigFields() {
		return getStaticConfigFields();
	}

	/**
	 * 获取驱动配置字段（静态方法）
	 * TCP直连驱动不需要驱动级配置，每个设备单独配置主机和端口
	 */
	public static List<DriverConfigField> getStaticConfigFields() {
		return new ArrayList<>();
	}
}
