package vip.xiaonuo.iot.core.driver.impl;

import cn.hutool.json.JSONObject;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
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
 * LoRa网关驱动
 * 用于LoRa网关设备接入
 * 
 * 功能：
 * 1. 监听LoRa网关TCP连接
 * 2. 管理LoRa终端设备
 * 3. 处理上行数据和下发指令
 * 4. 支持设备注册和心跳检测
 *
 * @author yubaoshan
 * @date 2025/12/13
 */
@Slf4j
@Driver(type = "LORA_GATEWAY", name = "LoRa网关驱动", description = "LoRa网关设备驱动")
public class LoraGatewayDriver extends AbstractDeviceDriver {

	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;
	private Channel serverChannel;
	
	/** 网关连接映射 gatewayId -> Channel */
	private final Map<String, Channel> gatewayChannels = new ConcurrentHashMap<>();
	
	/** 设备映射 deviceKey -> gatewayId */
	private final Map<String, String> deviceGatewayMap = new ConcurrentHashMap<>();

	public LoraGatewayDriver(DriverConfig config) {
		super(config);
	}

	@Override
	public String getDriverType() {
		return "LORA_GATEWAY";
	}

	@Override
	public String getDriverName() {
		return "LoRa网关驱动";
	}

	@Override
	protected void doStart() throws Exception {
		// 读取配置
		Integer portConfig = config.getInteger("port");
		int port = portConfig != null ? portConfig : 7000;

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
					// 长度字段解码器
					pipeline.addLast(new LengthFieldBasedFrameDecoder(65535, 0, 2, 0, 2));
					pipeline.addLast(new LengthFieldPrepender(2));
					pipeline.addLast(new LoraGatewayHandler());
				}
			});

		ChannelFuture future = bootstrap.bind(port).sync();
		serverChannel = future.channel();
		log.info("LoRa网关驱动启动成功，监听端口: {}", port);
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
		log.info("LoRa网关驱动已停止");
	}

	/**
	 * 注册LoRa终端设备到指定网关
	 */
	public void registerDevice(String deviceKey, String gatewayId) {
		deviceGatewayMap.put(deviceKey, gatewayId);
		log.info("LoRa设备 [{}] 注册到网关 [{}]", deviceKey, gatewayId);
	}

	/**
	 * 向LoRa设备发送下行数据
	 */
	public void sendToDevice(String deviceKey, byte[] data) {
		String gatewayId = deviceGatewayMap.get(deviceKey);
		if (gatewayId == null) {
			log.warn("设备 [{}] 未注册到任何网关", deviceKey);
			return;
		}

		Channel channel = gatewayChannels.get(gatewayId);
		if (channel != null && channel.isActive()) {
			// TODO: 封装LoRa协议帧
			channel.writeAndFlush(data);
			log.info("向LoRa设备 [{}] 发送数据，网关: {}", deviceKey, gatewayId);
		} else {
			log.warn("网关 [{}] 通道未激活", gatewayId);
		}
	}

	/**
	 * LoRa网关处理器
	 */
	private class LoraGatewayHandler extends SimpleChannelInboundHandler<byte[]> {

		@Override
		public void channelActive(ChannelHandlerContext ctx) {
			log.info("LoRa网关连接: {}", ctx.channel().remoteAddress());
		}

		@Override
		protected void channelRead0(ChannelHandlerContext ctx, byte[] data) {
			try {
				// TODO: 解析LoRa网关协议
				// 1. 识别网关ID
				// 2. 识别终端设备地址
				// 3. 提取数据内容
				
				log.debug("收到LoRa网关数据，长度: {}", data.length);
				
				// 示例：假设前8字节是网关ID
				if (data.length >= 8) {
					String gatewayId = new String(data, 0, 8).trim();
					gatewayChannels.put(gatewayId, ctx.channel());
					
					// TODO: 解析设备数据并处理
					// String deviceKey = parseDeviceKey(data);
					// byte[] deviceData = parseDeviceData(data);
					// onDataReceived(deviceKey, deviceData);
				}
			} catch (Exception e) {
				log.error("处理LoRa网关数据异常", e);
			}
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx) {
			// 网关断开，清理映射
			gatewayChannels.values().remove(ctx.channel());
			log.info("LoRa网关断开: {}", ctx.channel().remoteAddress());
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
			log.error("LoRa网关通道异常", cause);
			ctx.close();
		}
	}

	/**
	 * 数据接收处理（子类可重写）
	 */
	protected void onDataReceived(String deviceKey, byte[] data) {
		log.info("LoRa设备 [{}] 收到数据，长度: {}", deviceKey, data.length);
	}

	@Override
	public JSONObject readData(String deviceKey, JSONObject params) throws Exception {
		// LoRa是被动接收数据，不支持主动读取
		throw new UnsupportedOperationException("LoRa网关不支持主动读取数据");
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
			// TODO: 封装LoRa协议帧
			byte[] bytes = data.toString().getBytes();
			channel.writeAndFlush(bytes);
			log.info("向LoRa设备 [{}] 发送数据，网关: {}", deviceKey, gatewayId);
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
			"port", "监听端口", "number", 7000, 
			1024, 65535, null, 
			"请输入监听端口", "LoRa网关连接的服务端口", 12,
			"driver", true
		));
		
		fields.add(new DriverConfigField(
			"frequency", "默认频率(MHz)", "number", 470, 
			433, 928, null, 
			"请输入频率", "LoRa通信频率，常用433/470/868/915MHz", 12,
			"driver", false
		));
		
		fields.add(new DriverConfigField(
			"spreadingFactor", "默认扩频因子", "number", 7, 
			7, 12, null, 
			"请输入扩频因子", "SF7-SF12，值越大距离越远但速率越慢", 12,
			"driver", false
		));
		
		fields.add(new DriverConfigField(
                    "bandwidth", "默认带宽(kHz)", "number", 7.8,
                500, 125, null,
                "请输入带宽", "常用125kHz、7.8/10.4/15.6/20.8/31.25/41.7/62.5/250/500kHz", 12,
                "driver", false
                ));
		
		return fields;
	}
}
