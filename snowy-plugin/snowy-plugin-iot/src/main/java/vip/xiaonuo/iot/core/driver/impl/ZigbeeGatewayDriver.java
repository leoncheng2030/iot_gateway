package vip.xiaonuo.iot.core.driver.impl;

import cn.hutool.json.JSONObject;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import lombok.extern.slf4j.Slf4j;
import vip.xiaonuo.iot.core.config.DriverConfigField;
import vip.xiaonuo.iot.core.driver.AbstractDeviceDriver;
import vip.xiaonuo.iot.core.driver.DriverConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Zigbee网关驱动
 * 用于Zigbee网关设备接入
 * 
 * 功能：
 * 1. 监听Zigbee网关TCP连接
 * 2. 管理Zigbee终端设备
 * 3. 处理上行数据和下发指令
 * 4. 支持设备入网和组网管理
 *
 * @author yubaoshan
 * @date 2025/12/13
 */
@Slf4j
public class ZigbeeGatewayDriver extends AbstractDeviceDriver {

	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;
	private Channel serverChannel;
	
	/** 网关连接映射 gatewayId -> Channel */
	private final Map<String, Channel> gatewayChannels = new ConcurrentHashMap<>();
	
	/** 设备映射 deviceKey -> gatewayId */
	private final Map<String, String> deviceGatewayMap = new ConcurrentHashMap<>();
	
	/** Zigbee短地址映射 shortAddr -> deviceKey */
	private final Map<String, String> addressDeviceMap = new ConcurrentHashMap<>();

	public ZigbeeGatewayDriver(DriverConfig config) {
		super(config);
	}

	@Override
	public String getDriverType() {
		return "ZIGBEE_GATEWAY";
	}

	@Override
	public String getDriverName() {
		return "Zigbee网关驱动";
	}

	@Override
	protected void doStart() throws Exception {
		// 读取配置
		Integer portConfig = config.getInteger("port");
		int port = portConfig != null ? portConfig : 8000;

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
					ChannelPipeline pipeline = ch.pipeline();
					// 使用分隔符解码器（示例使用换行符）
					pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
					pipeline.addLast(new ZigbeeGatewayHandler());
				}
			});

		ChannelFuture future = bootstrap.bind(port).sync();
		serverChannel = future.channel();
		log.info("Zigbee网关驱动启动成功，监听端口: {}", port);
	}

	@Override
	protected void doStop() throws Exception {
		if (serverChannel != null) {
			serverChannel.close().sync();
		}
		if (workerGroup != null) {
			workerGroup.shutdownGracefully().sync();
		}
		if (bossGroup != null) {
			bossGroup.shutdownGracefully().sync();
		}
		gatewayChannels.clear();
		deviceGatewayMap.clear();
		addressDeviceMap.clear();
		log.info("Zigbee网关驱动已停止");
	}

	/**
	 * 注册Zigbee终端设备到指定网关
	 */
	public void registerDevice(String deviceKey, String gatewayId, String shortAddr) {
		deviceGatewayMap.put(deviceKey, gatewayId);
		addressDeviceMap.put(shortAddr, deviceKey);
		log.info("Zigbee设备 [{}] 注册到网关 [{}]，短地址: {}", deviceKey, gatewayId, shortAddr);
	}

	/**
	 * 向Zigbee设备发送下行数据
	 */
	public void sendToDevice(String deviceKey, byte[] data) {
		String gatewayId = deviceGatewayMap.get(deviceKey);
		if (gatewayId == null) {
			log.warn("设备 [{}] 未注册到任何网关", deviceKey);
			return;
		}

		Channel channel = gatewayChannels.get(gatewayId);
		if (channel != null && channel.isActive()) {
			// TODO: 封装Zigbee协议帧
			channel.writeAndFlush(data);
			log.info("向Zigbee设备 [{}] 发送数据，网关: {}", deviceKey, gatewayId);
		} else {
			log.warn("网关 [{}] 通道未激活", gatewayId);
		}
	}

	/**
	 * 允许设备入网
	 */
	public void permitJoin(String gatewayId, int duration) {
		Channel channel = gatewayChannels.get(gatewayId);
		if (channel != null && channel.isActive()) {
			// TODO: 发送允许入网指令
			log.info("网关 [{}] 允许设备入网，持续时间: {}秒", gatewayId, duration);
		}
	}

	/**
	 * Zigbee网关处理器
	 */
	private class ZigbeeGatewayHandler extends SimpleChannelInboundHandler<byte[]> {

		@Override
		public void channelActive(ChannelHandlerContext ctx) {
			log.info("Zigbee网关连接: {}", ctx.channel().remoteAddress());
		}

		@Override
		protected void channelRead0(ChannelHandlerContext ctx, byte[] data) {
			try {
				// TODO: 解析Zigbee网关协议
				// 1. 识别网关ID
				// 2. 识别终端设备地址（短地址或IEEE地址）
				// 3. 提取数据内容
				// 4. 处理设备入网、离网事件
				
				log.debug("收到Zigbee网关数据，长度: {}", data.length);
				
				// 示例：假设前8字节是网关ID
				if (data.length >= 8) {
					String gatewayId = new String(data, 0, 8).trim();
					gatewayChannels.put(gatewayId, ctx.channel());
					
					// TODO: 解析设备数据并处理
					// String shortAddr = parseShortAddr(data);
					// String deviceKey = addressDeviceMap.get(shortAddr);
					// if (deviceKey != null) {
					//     byte[] deviceData = parseDeviceData(data);
					//     onDataReceived(deviceKey, deviceData);
					// }
				}
			} catch (Exception e) {
				log.error("处理Zigbee网关数据异常", e);
			}
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx) {
			// 网关断开，清理映射
			gatewayChannels.values().remove(ctx.channel());
			log.info("Zigbee网关断开: {}", ctx.channel().remoteAddress());
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
			log.error("Zigbee网关通道异常", cause);
			ctx.close();
		}
	}

	/**
	 * 数据接收处理（子类可重写）
	 */
	protected void onDataReceived(String deviceKey, byte[] data) {
		log.info("Zigbee设备 [{}] 收到数据，长度: {}", deviceKey, data.length);
	}

	@Override
	public JSONObject readData(String deviceKey, JSONObject params) throws Exception {
		// Zigbee是被动接收数据，不支持主动读取
		throw new UnsupportedOperationException("Zigbee网关不支持主动读取数据");
	}

	@Override
	public boolean writeData(String deviceKey, JSONObject data) throws Exception {
		String gatewayId = deviceGatewayMap.get(deviceKey);
		if (gatewayId == null) {
			log.warn("设备 [{}] 未注册到任何网关", deviceKey);
			return false;
		}

		Channel channel = gatewayChannels.get(gatewayId);
		if (channel != null && channel.isActive()) {
			// TODO: 封装Zigbee协议帧
			byte[] bytes = data.toString().getBytes();
			channel.writeAndFlush(bytes);
			log.info("向Zigbee设备 [{}] 发送数据，网关: {}", deviceKey, gatewayId);
			return true;
		} else {
			log.warn("网关 [{}] 通道未激活", gatewayId);
			return false;
		}
	}

	@Override
	public boolean isDeviceOnline(String deviceKey) {
		String gatewayId = deviceGatewayMap.get(deviceKey);
		if (gatewayId == null) {
			return false;
		}
		Channel channel = gatewayChannels.get(gatewayId);
		return channel != null && channel.isActive();
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
			"port", "监听端口", "number", 8000, 
			1024, 65535, null, 
			"请输入监听端口", "Zigbee网关连接的服务端口", 12,
			"driver", true
		));
		
		fields.add(new DriverConfigField(
			"channel", "默认信道", "number", 11, 
			11, 26, null, 
			"请输入信道", "Zigbee通信信道，范圍11-26", 12,
			"driver", false
		));
		
		fields.add(new DriverConfigField(
			"panId", "默认PAN ID", "text", "0x1234", 
			null, null, null, 
			"请输入PAN ID", "个人区域网络标识符，如 0x1234", 12,
			"driver", false
		));
		
		fields.add(new DriverConfigField(
			"permitJoinDuration", "允许入网时长(秒)", "number", 60, 
			0, 255, null, 
			"请输入入网时长", "设备允许入网的默认时长，0表示禁止入网", 12,
			"driver", false
		));
		
		return fields;
	}
}
