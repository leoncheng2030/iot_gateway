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
import vip.xiaonuo.iot.core.protocol.modbus.Modbus4jTcpClient;
import vip.xiaonuo.iot.modular.device.entity.IotDevice;
import vip.xiaonuo.iot.modular.device.service.IotDeviceService;
import vip.xiaonuo.iot.modular.devicedriverrel.service.IotDeviceDriverRelService;

import java.util.ArrayList;
import java.util.List;

/**
 * Modbus TCP驱动 - 网关模式
 * 
 * 功能：
 * 1. 复用现有Modbus4jTcpClient实现
 * 2. 管理绑定到此驱动的所有Modbus设备
 * 3. 设备连接状态监控
 * 4. 支持读写寄存器操作
 *
 * 说明：
 * - 驱动启动时不主动连接设备
 * - 依赖ModbusPollingTimerTask定时轮询
 * - 提供统一的设备管理接口
 *
 * @author yubaoshan
 * @date 2025/12/13
 */
@Slf4j
@Driver(type = "MODBUS_TCP", name = "Modbus TCP驱动", description = "Modbus TCP协议驱动")
public class ModbusTcpDriver extends AbstractDeviceDriver {

	@Resource
	private Modbus4jTcpClient modbus4jTcpClient;
	
	@Resource
	private IotDeviceService iotDeviceService;
	
	@Resource
	private IotDeviceDriverRelService iotDeviceDriverRelService;
	
	/** 驱动ID */
	private String driverId;

	public ModbusTcpDriver(DriverConfig config) {
		super(config);
		this.driverId = config.getString("driverId");
	}

	@Override
	public String getDriverType() {
		return "MODBUS_TCP";
	}

	@Override
	public String getDriverName() {
		return "Modbus TCP驱动";
	}

	@Override
	protected void doStart() throws Exception {
		log.info("Modbus TCP驱动启动，复用现有Modbus4jTcpClient和ModbusPollingTimerTask");
		// 不需要额外操作，依赖现有轮询任务
	}

	@Override
	protected void doStop() throws Exception {
		// 断开所有绑定设备的连接
		List<IotDevice> devices = getBindDevices();
		for (IotDevice device : devices) {
			// Modbus TCP设备都有从站地址配置
			modbus4jTcpClient.disconnect(device.getId());
		}
		log.info("Modbus TCP驱动已停止，已断开 {} 个设备连接", devices.size());
	}

	/**
	 * 获取绑定到此驱动的设备列表
	 */
	private List<IotDevice> getBindDevices() {
		if (driverId == null) {
			return List.of();
		}
		
		// 查询使用Modbus TCP协议的设备
		// 注：这里简化处理，实际应根据产品协议类型判断
		LambdaQueryWrapper<IotDevice> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(IotDevice::getDeviceStatus, "ONLINE");
		
		return iotDeviceService.list(queryWrapper);
	}

	@Override
	public JSONObject readData(String deviceKey, JSONObject params) throws Exception {
		// 查询设备
		LambdaQueryWrapper<IotDevice> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(IotDevice::getDeviceKey, deviceKey);
		IotDevice device = iotDeviceService.getOne(queryWrapper);
		
		if (device == null) {
			throw new IllegalArgumentException("设备不存在: " + deviceKey);
		}
		
		// 查询设备驱动关联（获取设备级配置）
		LambdaQueryWrapper<vip.xiaonuo.iot.modular.devicedriverrel.entity.IotDeviceDriverRel> relQuery = new LambdaQueryWrapper<>();
		relQuery.eq(vip.xiaonuo.iot.modular.devicedriverrel.entity.IotDeviceDriverRel::getDeviceId, device.getId())
			    .eq(vip.xiaonuo.iot.modular.devicedriverrel.entity.IotDeviceDriverRel::getDriverId, driverId);
		vip.xiaonuo.iot.modular.devicedriverrel.entity.IotDeviceDriverRel driverRel = 
			iotDeviceDriverRelService.getOne(relQuery);
		
		// 使用现有客户端读取
		int functionCode = params.getInt("functionCode", 0x03);
		int startAddress = params.getInt("startAddress", 0);
		int quantity = params.getInt("quantity", 10);
		
		switch (functionCode) {
			case 0x01:
				modbus4jTcpClient.readCoils(device, driverRel, startAddress, quantity);
				break;
			case 0x02:
				modbus4jTcpClient.readDiscreteInputs(device, driverRel, startAddress, quantity);
				break;
			case 0x03:
				modbus4jTcpClient.readHoldingRegisters(device, driverRel, startAddress, quantity);
				break;
			case 0x04:
				modbus4jTcpClient.readInputRegisters(device, driverRel, startAddress, quantity);
				break;
			default:
				throw new IllegalArgumentException("不支持的功能码: " + functionCode);
		}
		
		return new JSONObject();
	}

	@Override
	public boolean writeData(String deviceKey, JSONObject data) throws Exception {
		// 查询设备
		LambdaQueryWrapper<IotDevice> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(IotDevice::getDeviceKey, deviceKey);
		IotDevice device = iotDeviceService.getOne(queryWrapper);
		
		if (device == null) {
			return false;
		}
		
		// 写入单个寄存器
		if (data.containsKey("address") && data.containsKey("value")) {
			int address = data.getInt("address");
			int value = data.getInt("value");
			modbus4jTcpClient.writeSingleRegister(device, address, value);
			return true;
		}
		
		return false;
	}

	@Override
	public boolean isDeviceOnline(String deviceKey) {
		LambdaQueryWrapper<IotDevice> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(IotDevice::getDeviceKey, deviceKey);
		IotDevice device = iotDeviceService.getOne(queryWrapper);
		
		if (device == null) {
			return false;
		}
		
		return "ONLINE".equals(device.getDeviceStatus());
	}

	/**
	 * 获取Modbus TCP驱动配置字段定义
	 */
	@Override
	public List<DriverConfigField> getConfigFields() {
		return getStaticConfigFields();
	}

	/**
	 * 获取驱动配置字段（静态方法）
	 * 用于Controller直接调用，无需创建实例
	 */
	public static List<DriverConfigField> getStaticConfigFields() {
		List<DriverConfigField> fields = new ArrayList<>();
		
		// 驱动级配置：默认轮询间隔
		DriverConfigField pollingIntervalField = new DriverConfigField();
		pollingIntervalField.setKey("pollingInterval");
		pollingIntervalField.setLabel("默认轮询间隔");
		pollingIntervalField.setType("number");
		pollingIntervalField.setLevel("driver");
		pollingIntervalField.setRequired(false);
		pollingIntervalField.setDefaultValue(5);
		pollingIntervalField.setMin(1);
		pollingIntervalField.setMax(60);
		pollingIntervalField.setPlaceholder("请输入默认轮询间隔");
		pollingIntervalField.setTip("设备轮询频率的默认值（秒）");
		pollingIntervalField.setSpan(12);
		fields.add(pollingIntervalField);
		
		// 驱动级配置：默认超时时间
		DriverConfigField timeoutField = new DriverConfigField();
		timeoutField.setKey("timeout");
		timeoutField.setLabel("默认超时时间");
		timeoutField.setType("number");
		timeoutField.setLevel("driver");
		timeoutField.setRequired(false);
		timeoutField.setDefaultValue(3);
		timeoutField.setMin(1);
		timeoutField.setMax(10);
		timeoutField.setPlaceholder("请输入默认超时时间");
		timeoutField.setTip("单次请求的超时时间（秒）");
		timeoutField.setSpan(12);
		fields.add(timeoutField);
		
		// 设备级配置：IP地址（必填）
		DriverConfigField ipField = new DriverConfigField();
		ipField.setKey("host");
		ipField.setLabel("IP地址");
		ipField.setType("text");
		ipField.setLevel("device");
		ipField.setRequired(true);
		ipField.setDefaultValue("");
		ipField.setPlaceholder("请输入设备IP地址");
		ipField.setTip("设备的IP地址");
		ipField.setSpan(12);
		fields.add(ipField);
		
		// 设备级配置：端口（必填）
		DriverConfigField portField = new DriverConfigField();
		portField.setKey("port");
		portField.setLabel("端口");
		portField.setType("number");
		portField.setLevel("device");
		portField.setRequired(true);
		portField.setDefaultValue(502);
		portField.setMin(1);
		portField.setMax(65535);
		portField.setPlaceholder("请输入端口");
		portField.setTip("Modbus TCP端口");
		portField.setSpan(12);
		fields.add(portField);
		
		// 设备级配置：从站地址（必填）
		DriverConfigField slaveAddressField = new DriverConfigField();
		slaveAddressField.setKey("slaveAddress");
		slaveAddressField.setLabel("从站地址");
		slaveAddressField.setType("number");
		slaveAddressField.setLevel("device");
		slaveAddressField.setRequired(true);
		slaveAddressField.setDefaultValue(1);
		slaveAddressField.setMin(1);
		slaveAddressField.setMax(247);
		slaveAddressField.setPlaceholder("请输入从站地址");
		slaveAddressField.setTip("Modbus从站地址(1-247)");
		slaveAddressField.setSpan(12);
		fields.add(slaveAddressField);
		
		// 设备级配置：轮询间隔（可选，覆盖驱动默认值）
		DriverConfigField devicePollingField = new DriverConfigField();
		devicePollingField.setKey("pollingInterval");
		devicePollingField.setLabel("轮询间隔");
		devicePollingField.setType("number");
		devicePollingField.setLevel("device");
		devicePollingField.setRequired(false);
		devicePollingField.setDefaultValue(null);
		devicePollingField.setMin(1);
		devicePollingField.setMax(60);
		devicePollingField.setPlaceholder("留空使用驱动默认值");
		devicePollingField.setTip("覆盖驱动默认的轮询间隔（秒）");
		devicePollingField.setSpan(12);
		fields.add(devicePollingField);
		
		// 设备级配置：超时时间（可选，覆盖驱动默认值）
		DriverConfigField deviceTimeoutField = new DriverConfigField();
		deviceTimeoutField.setKey("timeout");
		deviceTimeoutField.setLabel("超时时间");
		deviceTimeoutField.setType("number");
		deviceTimeoutField.setLevel("device");
		deviceTimeoutField.setRequired(false);
		deviceTimeoutField.setDefaultValue(null);
		deviceTimeoutField.setMin(1);
		deviceTimeoutField.setMax(10);
		deviceTimeoutField.setPlaceholder("留空使用驱动默认值");
		deviceTimeoutField.setTip("覆盖驱动默认的超时时间（秒）");
		deviceTimeoutField.setSpan(12);
		fields.add(deviceTimeoutField);
		
		return fields;
	}
}
