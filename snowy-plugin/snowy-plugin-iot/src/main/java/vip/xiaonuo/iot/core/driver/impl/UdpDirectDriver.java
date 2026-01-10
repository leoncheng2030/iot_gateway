package vip.xiaonuo.iot.core.driver.impl;

import cn.hutool.json.JSONObject;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import vip.xiaonuo.iot.core.config.DriverConfigField;
import vip.xiaonuo.iot.core.driver.AbstractDeviceDriver;
import vip.xiaonuo.iot.core.driver.DriverConfig;
import vip.xiaonuo.iot.core.driver.annotation.Driver;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * UDP直连驱动
 * 用于UDP协议直接连接的设备
 */
@Slf4j
@Driver(type = "UDP_DIRECT", name = "UDP直连驱动", description = "UDP直连设备驱动")
public class UdpDirectDriver extends AbstractDeviceDriver {

	private EventLoopGroup group;
	private Channel channel;
	private final Map<String, InetSocketAddress> deviceAddresses = new ConcurrentHashMap<>();
	private int heartbeatInterval = 60; // 心跳间隔，单位：秒

	public UdpDirectDriver(DriverConfig config) {
		super(config);
	}

	@Override
	public String getDriverType() {
		return "UDP_DIRECT";
	}

	@Override
	public String getDriverName() {
		return "UDP直连驱动";
	}

	@Override
	protected void doStart() throws Exception {
		// 读取配置
		Integer portConfig = config.getInteger("port");
		int port = portConfig != null ? portConfig : 5000;
		
		Integer heartbeatConfig = config.getInteger("heartbeatInterval");
		if (heartbeatConfig != null) {
			heartbeatInterval = heartbeatConfig;
		}

		group = new NioEventLoopGroup();
		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(group)
			.channel(NioDatagramChannel.class)
			.option(ChannelOption.SO_BROADCAST, true)
			.handler(new ChannelInitializer<NioDatagramChannel>() {
				@Override
				protected void initChannel(NioDatagramChannel ch) {
					ChannelPipeline pipeline = ch.pipeline();
					pipeline.addLast(new IdleStateHandler(0, heartbeatInterval, 0, TimeUnit.SECONDS));
					pipeline.addLast(new UdpServerHandler());
				}
			});

		ChannelFuture future = bootstrap.bind(port).sync();
		channel = future.channel();
		log.info("UDP直连驱动启动成功，监听端口: {}", port);
	}

	@Override
	protected void doStop() throws Exception {
		if (channel != null) {
			channel.close().sync();
		}
		if (group != null) {
			group.shutdownGracefully().sync();
		}
		deviceAddresses.clear();
		log.info("UDP直连驱动已停止");
	}

	/**
	 * 注册设备地址
	 */
	public void registerDevice(String deviceKey, String host, int port) {
		deviceAddresses.put(deviceKey, new InetSocketAddress(host, port));
		log.info("注册UDP设备: {} -> {}:{}", deviceKey, host, port);
	}

	/**
	 * 发送数据到设备
	 */
	public void sendToDevice(String deviceKey, byte[] data) {
		InetSocketAddress address = deviceAddresses.get(deviceKey);
		if (address != null && channel != null) {
			DatagramPacket packet = new DatagramPacket(
				Unpooled.copiedBuffer(data),
				address
			);
			channel.writeAndFlush(packet);
		} else {
			log.warn("设备未注册或通道未就绪: {}", deviceKey);
		}
	}

	/**
	 * UDP服务器处理器
	 */
	private class UdpServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {

		@Override
		protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
			try {
				// 读取数据
				byte[] data = new byte[packet.content().readableBytes()];
				packet.content().readBytes(data);
				
				InetSocketAddress sender = packet.sender();
				log.debug("收到UDP数据 from {}:{}, 长度: {}", 
					sender.getHostString(), sender.getPort(), data.length);

				// 查找对应的设备
				String deviceKey = findDeviceByAddress(sender);
				if (deviceKey != null) {
					// 触发数据接收事件
					onDataReceived(deviceKey, data);
				} else {
					log.warn("未知设备地址: {}:{}", sender.getHostString(), sender.getPort());
				}
			} catch (Exception e) {
				log.error("处理UDP数据异常", e);
			}
		}

		@Override
		public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
			if (evt instanceof IdleStateEvent) {
				IdleStateEvent event = (IdleStateEvent) evt;
				if (event.state() == IdleStateEvent.WRITER_IDLE_STATE_EVENT.state()) {
					// 发送心跳到所有已注册设备
					sendHeartbeatToAllDevices();
				}
			}
			super.userEventTriggered(ctx, evt);
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
			log.error("UDP通道异常", cause);
		}
	}

	/**
	 * 根据地址查找设备
	 */
	private String findDeviceByAddress(InetSocketAddress address) {
		for (Map.Entry<String, InetSocketAddress> entry : deviceAddresses.entrySet()) {
			InetSocketAddress deviceAddr = entry.getValue();
			if (deviceAddr.getHostString().equals(address.getHostString()) &&
				deviceAddr.getPort() == address.getPort()) {
				return entry.getKey();
			}
		}
		return null;
	}

	/**
	 * 向所有设备发送心跳
	 */
	private void sendHeartbeatToAllDevices() {
		byte[] heartbeat = "HEARTBEAT".getBytes(StandardCharsets.UTF_8);
		for (String deviceKey : deviceAddresses.keySet()) {
			sendToDevice(deviceKey, heartbeat);
		}
	}

	/**
	 * 数据接收处理（子类可重写）
	 */
	protected void onDataReceived(String deviceKey, byte[] data) {
		// 默认实现，可以被子类重写
		log.info("设备 {} 收到数据，长度: {}", deviceKey, data.length);
	}

	@Override
	public JSONObject readData(String deviceKey, JSONObject params) throws Exception {
		// UDP是被动接收数据，不支持主动读取
		throw new UnsupportedOperationException("UDP直连驱动不支持主动读取数据");
	}

	@Override
	public boolean writeData(String deviceKey, JSONObject data) throws Exception {
		InetSocketAddress address = deviceAddresses.get(deviceKey);
		if (address == null) {
			log.warn("设备 [{}] 未注册地址", deviceKey);
			return false;
		}

		if (channel != null && channel.isActive()) {
			byte[] bytes = data.toString().getBytes();
			DatagramPacket packet = new DatagramPacket(
				Unpooled.copiedBuffer(bytes),
				address
			);
			channel.writeAndFlush(packet);
			log.info("向UDP设备 [{}] 发送数据: {}:{}", deviceKey, address.getHostString(), address.getPort());
			return true;
		} else {
			log.warn("UDP通道未就绪");
			return false;
		}
	}

	@Override
	public boolean isDeviceOnline(String deviceKey) {
		// UDP无连接状态，只要注册了地址就认为在线
		return deviceAddresses.containsKey(deviceKey);
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
			"port", "监听端口", "number", 5000, 
			1024, 65535, null, 
			"请输入监听端口", "UDP设备连接的服务端口", 12,
			"driver", true
		));
		
		fields.add(new DriverConfigField(
			"heartbeatInterval", "心跳间隔(秒)", "number", 60, 
			10, 300, null, 
			"请输入心跳间隔", "向设备发送心跳包的时间间隔", 12,
			"driver", false
		));
		
		return fields;
	}
}
