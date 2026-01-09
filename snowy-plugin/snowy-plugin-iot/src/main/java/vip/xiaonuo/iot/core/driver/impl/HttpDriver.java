/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 */
package vip.xiaonuo.iot.core.driver.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import vip.xiaonuo.iot.core.config.DriverConfigField;
import vip.xiaonuo.iot.core.driver.AbstractDeviceDriver;
import vip.xiaonuo.iot.core.driver.DriverConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HTTP驱动
 * 
 * 功能：
 * 1. RESTful API设备接入
 * 2. 支持GET/POST/PUT/DELETE请求
 * 3. 支持认证（Basic/Token）
 * 4. 支持自定义请求头
 *
 * @author yubaoshan
 * @date 2025/12/13
 */
@Slf4j
public class HttpDriver extends AbstractDeviceDriver {

	/** 基础URL */
	private String baseUrl;
	
	/** 认证方式 */
	private String authType;
	
	/** 认证Token */
	private String authToken;
	
	/** 超时时间(毫秒) */
	private int timeout = 10000;
	
	/** 设备配置映射 deviceKey -> HttpDeviceConfig */
	private final Map<String, HttpDeviceConfig> deviceConfigs = new ConcurrentHashMap<>();

	public HttpDriver(DriverConfig config) {
		super(config);
	}

	@Override
	public String getDriverType() {
		return "HTTP";
	}

	@Override
	public String getDriverName() {
		return "HTTP驱动";
	}

	@Override
	protected void doStart() throws Exception {
		baseUrl = config.getString("baseUrl");
		if (baseUrl == null) {
			throw new IllegalArgumentException("HTTP驱动缺少baseUrl配置");
		}
		
		authType = config.getString("authType");
		authToken = config.getString("authToken");
		
		Integer timeoutConfig = config.getInteger("timeout");
		if (timeoutConfig != null && timeoutConfig > 0) {
			timeout = timeoutConfig;
		}
		
		log.info("HTTP驱动初始化完成，BaseURL: {}, 认证方式: {}", baseUrl, authType);
	}

	@Override
	protected void doStop() throws Exception {
		deviceConfigs.clear();
	}

	/**
	 * 注册HTTP设备
	 */
	public void registerDevice(String deviceKey, String endpoint) {
		HttpDeviceConfig config = new HttpDeviceConfig(deviceKey, endpoint);
		deviceConfigs.put(deviceKey, config);
		log.info("HTTP设备 [{}] 注册成功，端点: {}", deviceKey, endpoint);
	}

	/**
	 * 注销设备
	 */
	public void unregisterDevice(String deviceKey) {
		deviceConfigs.remove(deviceKey);
		log.info("HTTP设备 [{}] 注销成功", deviceKey);
	}

	@Override
	public JSONObject readData(String deviceKey, JSONObject params) throws Exception {
		HttpDeviceConfig deviceConfig = deviceConfigs.get(deviceKey);
		if (deviceConfig == null) {
			throw new IllegalStateException("设备未注册: " + deviceKey);
		}
		
		String url = baseUrl + deviceConfig.endpoint;
		
		// 构造GET请求
		HttpRequest request = HttpRequest.get(url)
				.timeout(timeout);
		
		// 添加认证头
		addAuthHeader(request);
		
		// 发送请求
		HttpResponse response = request.execute();
		
		if (response.isOk()) {
			String body = response.body();
			log.debug("HTTP设备 [{}] 读取成功: {}", deviceKey, body);
			return JSONUtil.parseObj(body);
		} else {
			throw new RuntimeException("HTTP请求失败: " + response.getStatus());
		}
	}

	@Override
	public boolean writeData(String deviceKey, JSONObject data) throws Exception {
		HttpDeviceConfig deviceConfig = deviceConfigs.get(deviceKey);
		if (deviceConfig == null) {
			log.warn("HTTP设备 [{}] 未注册", deviceKey);
			return false;
		}
		
		String url = baseUrl + deviceConfig.endpoint;
		
		// 构造POST请求
		HttpRequest request = HttpRequest.post(url)
				.timeout(timeout)
				.body(data.toString());
		
		// 添加认证头
		addAuthHeader(request);
		
		// 发送请求
		HttpResponse response = request.execute();
		
		if (response.isOk()) {
			log.info("向HTTP设备 [{}] 写入数据成功", deviceKey);
			return true;
		} else {
			log.error("向HTTP设备 [{}] 写入数据失败: {}", deviceKey, response.getStatus());
			return false;
		}
	}

	@Override
	public boolean isDeviceOnline(String deviceKey) {
		// HTTP设备在线状态需要通过健康检查接口判断
		try {
			HttpDeviceConfig deviceConfig = deviceConfigs.get(deviceKey);
			if (deviceConfig == null) {
				return false;
			}
			
			String url = baseUrl + deviceConfig.endpoint + "/health";
			HttpResponse response = HttpRequest.get(url)
					.timeout(3000)
					.execute();
			
			return response.isOk();
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 添加认证头
	 */
	private void addAuthHeader(HttpRequest request) {
		if ("Bearer".equalsIgnoreCase(authType) && authToken != null) {
			request.header("Authorization", "Bearer " + authToken);
		} else if ("Basic".equalsIgnoreCase(authType) && authToken != null) {
			request.header("Authorization", "Basic " + authToken);
		}
	}

	/**
	 * HTTP设备配置
	 */
	private static class HttpDeviceConfig {
		private final String deviceKey;
		private final String endpoint;

		public HttpDeviceConfig(String deviceKey, String endpoint) {
			this.deviceKey = deviceKey;
			this.endpoint = endpoint;
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
			"baseUrl", "基础URL", "text", null, 
			null, null, null, 
			"请输入基础URL", "如: http://192.168.1.100:8080", 24,
			"driver", true
		));
		
		fields.add(new DriverConfigField(
			"authType", "认证方式", "select", null, 
			null, null, 
			List.of(
				new DriverConfigField.FieldOption("", "无需认证"),
				new DriverConfigField.FieldOption("Bearer", "Bearer Token"),
				new DriverConfigField.FieldOption("Basic", "Basic Auth")
			),
			"请选择认证方式", "默认无需认证", 12,
			"driver", false
		));
		
		fields.add(new DriverConfigField(
			"authToken", "认证Token", "password", null, 
			null, null, null, 
			"请输入认证Token", "如需认证时填写", 12,
			"driver", false
		));
		
		fields.add(new DriverConfigField(
			"timeout", "超时时间(毫秒)", "number", 10000, 
			1000, 60000, null, 
			"请输入超时时间", "HTTP请求超时时间", 12,
			"driver", false
		));
		
		return fields;
	}
}
