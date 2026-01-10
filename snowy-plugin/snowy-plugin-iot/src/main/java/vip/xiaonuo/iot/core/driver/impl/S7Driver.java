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
import vip.xiaonuo.iot.core.protocol.s7.S7ProtocolServer;
import vip.xiaonuo.iot.modular.device.entity.IotDevice;
import vip.xiaonuo.iot.modular.device.service.IotDeviceService;
import vip.xiaonuo.iot.modular.devicedriverrel.entity.IotDeviceDriverRel;
import vip.xiaonuo.iot.modular.devicedriverrel.service.IotDeviceDriverRelService;
import vip.xiaonuo.iot.modular.register.entity.IotDeviceRegisterMapping;
import vip.xiaonuo.iot.modular.register.service.IotDeviceRegisterMappingService;

import java.util.ArrayList;
import java.util.List;

/**
 * S7驱动 - 西门子PLC
 * 
 * 功能：
 * 1. 支持S7-200/300/400/1200/1500系列PLC
 * 2. 主动轮询数据采集
 * 3. 支持DB块、M区、V区读写
 * 4. 管理绑定到此驱动的所有S7设备
 *
 * @author xiaonuo
 * @date 2026/01/10
 */
@Slf4j
@Driver(type = "S7", name = "S7驱动", description = "西门子S7系列PLC驱动")
public class S7Driver extends AbstractDeviceDriver {

	@Resource
	private S7ProtocolServer s7ProtocolServer;
	
	@Resource
	private IotDeviceService iotDeviceService;
	
	@Resource
	private IotDeviceDriverRelService iotDeviceDriverRelService;
	
	@Resource
	private IotDeviceRegisterMappingService registerMappingService;
	
	/** 驱动ID */
	private String driverId;

	public S7Driver(DriverConfig config) {
		super(config);
		// 从 DriverConfig 字段直接读取，而不是从 configMap
		this.driverId = config.getDriverId();
	}

	@Override
	public String getDriverType() {
		return "S7";
	}

	@Override
	public String getDriverName() {
		return "S7驱动";
	}

	@Override
	protected void doStart() throws Exception {
		log.info("S7驱动启动，开始管理S7设备连接");
		
		// 启动S7协议服务
		s7ProtocolServer.start();
		
		// 加载所有绑定到此驱动的设备
		List<IotDevice> devices = getBindDevices();
		for (IotDevice device : devices) {
			try {
				addDevice(device);
			} catch (Exception e) {
				log.error("添加S7设备失败 - DeviceId: {}", device.getId(), e);
			}
		}
		
		log.info("S7驱动启动成功，已加载 {} 个设备", devices.size());
	}

	@Override
	protected void doStop() throws Exception {
		// 移除所有设备
		List<IotDevice> devices = getBindDevices();
		for (IotDevice device : devices) {
			try {
				s7ProtocolServer.removeDevice(device.getId());
			} catch (Exception e) {
				log.error("移除S7设备失败 - DeviceId: {}", device.getId(), e);
			}
		}
		
		// 停止S7协议服务
		s7ProtocolServer.stop();
		
		log.info("S7驱动已停止，已断开 {} 个设备连接", devices.size());
	}

	/**
	 * 添加设备到S7协议服务
	 */
	private void addDevice(IotDevice device) throws Exception {
		// 查询设备驱动关联
		LambdaQueryWrapper<IotDeviceDriverRel> relQuery = new LambdaQueryWrapper<>();
		relQuery.eq(IotDeviceDriverRel::getDeviceId, device.getId())
				.eq(IotDeviceDriverRel::getDriverId, driverId);
		IotDeviceDriverRel driverRel = iotDeviceDriverRelService.getOne(relQuery);
		
		if (driverRel == null) {
			throw new IllegalStateException("设备未绑定驱动 - DeviceId: " + device.getId());
		}
		
		// 查询寄存器映射
		LambdaQueryWrapper<IotDeviceRegisterMapping> mappingQuery = new LambdaQueryWrapper<>();
		mappingQuery.eq(IotDeviceRegisterMapping::getDeviceId, device.getId())
					.eq(IotDeviceRegisterMapping::getEnabled, true)
					.orderByAsc(IotDeviceRegisterMapping::getSortCode);
		List<IotDeviceRegisterMapping> registerMappings = registerMappingService.list(mappingQuery);
		
		// 添加到S7协议服务
		s7ProtocolServer.addDevice(device, driverRel, registerMappings);
	}

	/**
	 * 获取绑定到此驱动的设备列表
	 */
	private List<IotDevice> getBindDevices() {
		if (driverId == null) {
			return List.of();
		}
		
		// 查询绑定关系
		LambdaQueryWrapper<IotDeviceDriverRel> relQuery = new LambdaQueryWrapper<>();
		relQuery.eq(IotDeviceDriverRel::getDriverId, driverId);
		List<IotDeviceDriverRel> rels = iotDeviceDriverRelService.list(relQuery);
		
		if (rels.isEmpty()) {
			return List.of();
		}
		
		// 查询设备
		List<String> deviceIds = rels.stream()
				.map(IotDeviceDriverRel::getDeviceId)
				.toList();
		
		return iotDeviceService.listByIds(deviceIds);
	}

	@Override
	public JSONObject readData(String deviceKey, JSONObject params) throws Exception {
		// S7采用定时轮询，不支持主动读取
		throw new UnsupportedOperationException("S7驱动采用定时轮询模式，不支持主动读取数据");
	}

	@Override
	public boolean writeData(String deviceKey, JSONObject data) throws Exception {
		// TODO: 实现S7数据写入功能
		log.warn("S7驱动暂不支持数据写入功能");
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
		
		// 检查S7连接状态
		return s7ProtocolServer != null && 
			   s7ProtocolServer.getS7Client() != null && 
			   s7ProtocolServer.getS7Client().isConnected(device.getId());
	}

	@Override
	public List<DriverConfigField> getConfigFields() {
		return getStaticConfigFields();
	}

	/**
	 * 获取S7驱动配置字段（静态方法）
	 */
	public static List<DriverConfigField> getStaticConfigFields() {
		List<DriverConfigField> fields = new ArrayList<>();
		
		// 驱动级配置
		fields.add(new DriverConfigField(
			"defaultPollingInterval", "默认轮询间隔(ms)", "number", 5000, 
			1000, 60000, null, 
			"请输入轮询间隔", "数据采集间隔，单位毫秒", 24,
			"driver", false
		));
		
		fields.add(new DriverConfigField(
			"timeout", "连接超时(ms)", "number", 5000, 
			1000, 30000, null, 
			"请输入超时时间", "S7连接超时时间，单位毫秒", 12,
			"driver", false
		));
		
		// 设备级配置
		fields.add(new DriverConfigField(
			"host", "PLC地址", "text", null, 
			null, null, null, 
			"请输入PLC IP地址", "西门子PLC的IP地址，如 192.168.1.100", 12,
			"device", true
		));
		
		fields.add(new DriverConfigField(
			"port", "端口号", "number", 102, 
			1, 65535, null, 
			"请输入端口号", "S7协议端口，默认102", 12,
			"device", false
		));
		
		fields.add(new DriverConfigField(
			"rack", "机架号", "number", 0, 
			0, 7, null, 
			"请输入机架号", "PLC机架号，S7-200/1200通常为0", 12,
			"device", false
		));
		
		fields.add(new DriverConfigField(
			"slot", "插槽号", "number", 2, 
			0, 31, null, 
			"请输入插槽号", "CPU插槽号，S7-200/1200通常为2", 12,
			"device", false
		));
		
		fields.add(new DriverConfigField(
			"interval", "采集间隔(ms)", "number", 5000, 
			1000, 60000, null, 
			"请输入采集间隔", "此设备的数据采集间隔，单位毫秒", 12,
			"device", false
		));
		
		return fields;
	}
}
