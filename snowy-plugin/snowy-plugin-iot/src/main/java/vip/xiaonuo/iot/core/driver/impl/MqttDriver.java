/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 */
package vip.xiaonuo.iot.core.driver.impl;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import vip.xiaonuo.iot.core.config.DriverConfigField;
import vip.xiaonuo.iot.core.driver.AbstractDeviceDriver;
import vip.xiaonuo.iot.core.driver.DriverConfig;
import vip.xiaonuo.iot.core.driver.annotation.Driver;
import vip.xiaonuo.iot.core.message.DeviceMessageService;
import vip.xiaonuo.iot.core.mqtt.MqttSessionManager;
import vip.xiaonuo.iot.core.protocol.ProtocolManager;
import vip.xiaonuo.iot.modular.device.entity.IotDevice;
import vip.xiaonuo.iot.modular.device.service.IotDeviceService;
import vip.xiaonuo.iot.modular.protocol.entity.IotProtocol;
import vip.xiaonuo.iot.modular.protocol.service.IotProtocolService;

import java.util.ArrayList;
import java.util.List;

/**
 * MQTT驱动 - 网关模式
 * 
 * 功能：
 * 1. 管理连接到本地MQTT服务器的设备
 * 2. 复用现有DynamicMqttServer和MqttServerHandler
 * 3. 支持设备消息发布
 * 4. 设备在线状态监控
 *
 * 说明：
 * - 驱动启动时确俜MQTT服务器已运行
 * - 设备通过MQTT协议主动连接到服务器
 * - 使用MqttSessionManager管理设备会话
 * - 使用DeviceMessageService发送消息到设备
 *
 * @author yubaoshan
 * @date 2025/12/13
 */
@Slf4j
@Driver(type = "MQTT", name = "MQTT驱动", description = "MQTT协议驱动")
public class MqttDriver extends AbstractDeviceDriver {

	@Resource
	private ProtocolManager protocolManager;
	
	@Resource
	private MqttSessionManager mqttSessionManager;
	
	@Resource
	private DeviceMessageService deviceMessageService;
	
	@Resource
	private IotDeviceService iotDeviceService;
	
	@Resource
	private IotProtocolService iotProtocolService;
	
	/** 驱动ID */
	private String driverId;
	
	/** MQTT服务器端口 */
	private Integer mqttPort = 1883;

	public MqttDriver(DriverConfig config) {
		super(config);
		this.driverId = config.getString("driverId");
		Integer port = config.getInteger("port");
		if (port != null) {
			mqttPort = port;
		}
	}

	@Override
	public String getDriverType() {
		return "MQTT";
	}

	@Override
	public String getDriverName() {
		return "MQTT驱动";
	}

	@Override
	protected void doStart() throws Exception {
		// 查询MQTT协议配置（端口1883，用于设备连接）
		LambdaQueryWrapper<IotProtocol> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(IotProtocol::getProtocolType, "MQTT")
					.eq(IotProtocol::getProtocolPort, 1883); // 指定查询1883端口，避免查到北向1884端口
		IotProtocol mqttProtocol = iotProtocolService.getOne(queryWrapper);
		
		if (mqttProtocol == null) {
			throw new IllegalStateException("MQTT协议未配置，请先在协议管理中添加MQTT协议");
		}
		
		// 检查MQTT服务器是否运行
		if (!protocolManager.isRunning(mqttProtocol.getId())) {
			log.info("MQTT服务器未运行，尝试启动...");
			// 注：这里不自动启动，由系统管理员手动启动
			throw new IllegalStateException("MQTT服务器未运行，请先在协议管理中启动MQTT服务器");
		}
		
		log.info("MQTT驱动启动，复用现有MQTT服务器 (Port: {})", mqttPort);
		log.info("设备通过MQTT客户端连接到本地MQTT Broker");
	}

	@Override
	protected void doStop() throws Exception {
		// 断开所有绑定设备的连接
		List<IotDevice> devices = getBindDevices();
		for (IotDevice device : devices) {
			String deviceKey = device.getDeviceKey();
			if (mqttSessionManager.isOnline(deviceKey)) {
				mqttSessionManager.removeSession(deviceKey);
				log.info("已断开MQTT设备: {}", deviceKey);
			}
		}
		log.info("MQTT驱动已停止，已断开 {} 个设备连接", devices.size());
	}

	/**
	 * 获取绑定到此驱动的设备列表
	 */
	private List<IotDevice> getBindDevices() {
		if (driverId == null) {
			return List.of();
		}
		
		// 查询使用MQTT协议的设备
		// 注：这里简化处理，实际应根据产品协议类型判断
		LambdaQueryWrapper<IotDevice> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(IotDevice::getDeviceStatus, "ONLINE");
		
		return iotDeviceService.list(queryWrapper);
	}

	@Override
	public JSONObject readData(String deviceKey, JSONObject params) throws Exception {
		// MQTT是被动接收数据，不支持主动读取
		throw new UnsupportedOperationException("MQTT驱动不支持主动读取数据，请等待设备上报");
	}

	@Override
	public boolean writeData(String deviceKey, JSONObject data) throws Exception {
		// 查询设备
		LambdaQueryWrapper<IotDevice> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(IotDevice::getDeviceKey, deviceKey);
		IotDevice device = iotDeviceService.getOne(queryWrapper);
		
		if (device == null) {
			log.warn("设备不存在: {}", deviceKey);
			return false;
		}
		
		// 构建下行消息Topic：/{productKey}/{deviceKey}/command/down
		String topic = String.format("/%s/%s/command/down", device.getProductId(), deviceKey);
		
		// 发送消息到设备
		boolean success = deviceMessageService.sendToDevice(deviceKey, topic, data.toString());
		
		if (success) {
			log.info("已发送消息到MQTT设备: {}, Topic: {}", deviceKey, topic);
		} else {
			log.warn("发送消息失败，设备可能未连接: {}", deviceKey);
		}
		
		return success;
	}

	@Override
	public boolean isDeviceOnline(String deviceKey) {
		return mqttSessionManager.isOnline(deviceKey);
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
			"brokerUrl", "MQTT Broker地址", "text", "tcp://localhost:1883", 
			null, null, null, 
			"请输入MQTT Broker地址", "MQTT服务器完整地址，如 tcp://localhost:1883", 24,
			"driver", true
		));
		
		fields.add(new DriverConfigField(
			"port", "MQTT服务端口", "number", 1883, 
			1024, 65535, null, 
			"请输入MQTT服务端口", "MQTT Broker的监听端口", 12,
			"driver", true
		));
		
		fields.add(new DriverConfigField(
			"topicPrefix", "Topic前缀", "text", "/device", 
			null, null, null, 
			"请输入Topic前缀", "MQTT消息主题的前缀，默认为 /device", 12,
			"driver", false
		));
		
		fields.add(new DriverConfigField(
			"qos", "默认QoS级别", "number", 1, 
			0, 2, null, 
			"请输入QoS级别", "消息质量等级：0-至多一次，1-至少一次，2-仅一次", 12,
			"driver", false
		));
		
		return fields;
	}
}
