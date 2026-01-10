package vip.xiaonuo.iot.core.driver.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import vip.xiaonuo.iot.core.config.DriverConfigField;
import vip.xiaonuo.iot.core.driver.AbstractDeviceDriver;
import vip.xiaonuo.iot.core.driver.DriverConfig;
import vip.xiaonuo.iot.core.driver.annotation.Driver;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自定义驱动
 * 支持用户扩展的自定义协议驱动
 * 
 * 功能：
 * 1. 动态加载自定义协议处理器
 * 2. 支持脚本化数据解析
 * 3. 灵活的数据转换机制
 * 4. 插件式扩展架构
 *
 * 使用方式：
 * 1. 实现自定义协议处理器接口
 * 2. 在配置中指定处理器类名
 * 3. 通过反射动态加载执行
 *
 * @author yubaoshan
 * @date 2025/12/13
 */
@Slf4j
@Driver(type = "CUSTOM", name = "自定义驱动", description = "用户扩展自定义驱动")
public class CustomDriver extends AbstractDeviceDriver {

	/** 自定义协议处理器类名 */
	private String handlerClass;
	
	/** 处理器实例 */
	private Object handlerInstance;
	
	/** 设备上下文 deviceKey -> context */
	private final Map<String, JSONObject> deviceContextMap = new ConcurrentHashMap<>();
	
	/** 自定义配置参数 */
	private JSONObject customConfig;

	public CustomDriver(DriverConfig config) {
		super(config);
	}

	@Override
	public String getDriverType() {
		return "CUSTOM";
	}

	@Override
	public String getDriverName() {
		return "自定义驱动";
	}

	@Override
	protected void doStart() throws Exception {
		// 读取自定义处理器类名
		handlerClass = config.getString("handlerClass");
		if (handlerClass == null) {
			throw new IllegalArgumentException("自定义驱动必须配置 handlerClass 参数");
		}
		
		// 读取自定义配置
		String customConfigStr = config.getString("customConfig");
		if (customConfigStr != null) {
			customConfig = JSONUtil.parseObj(customConfigStr);
		} else {
			customConfig = JSONUtil.createObj();
		}
		
		// 动态加载处理器
		try {
			Class<?> clazz = Class.forName(handlerClass);
			handlerInstance = clazz.getDeclaredConstructor().newInstance();
			
			// 调用初始化方法（如果存在）
			try {
				Method initMethod = clazz.getMethod("init", JSONObject.class);
				initMethod.invoke(handlerInstance, customConfig);
			} catch (NoSuchMethodException e) {
				// 没有init方法，忽略
			}
			
			log.info("自定义驱动启动成功，处理器: {}", handlerClass);
		} catch (Exception e) {
			log.error("加载自定义处理器失败: {}", handlerClass, e);
			throw new RuntimeException("加载自定义处理器失败", e);
		}
	}

	@Override
	protected void doStop() throws Exception {
		// 调用销毁方法（如果存在）
		if (handlerInstance != null) {
			try {
				Method destroyMethod = handlerInstance.getClass().getMethod("destroy");
				destroyMethod.invoke(handlerInstance);
			} catch (NoSuchMethodException e) {
				// 没有destroy方法，忽略
			}
		}
		
		deviceContextMap.clear();
		handlerInstance = null;
		log.info("自定义驱动已停止");
	}

	/**
	 * 注册设备
	 */
	public void registerDevice(String deviceKey, JSONObject context) {
		deviceContextMap.put(deviceKey, context);
		log.info("自定义驱动注册设备: {}, 上下文: {}", deviceKey, context);
	}

	/**
	 * 处理接收到的数据
	 */
	public void handleReceivedData(String deviceKey, byte[] rawData) {
		try {
			// 调用处理器的解析方法
			Method parseMethod = handlerInstance.getClass()
				.getMethod("parseData", String.class, byte[].class, JSONObject.class);
			
			JSONObject context = deviceContextMap.get(deviceKey);
			Object result = parseMethod.invoke(handlerInstance, deviceKey, rawData, context);
			
			if (result instanceof JSONObject) {
				onDataReceived(deviceKey, (JSONObject) result);
			}
		} catch (Exception e) {
			log.error("自定义驱动处理数据失败: {}", deviceKey, e);
		}
	}

	/**
	 * 发送数据到设备
	 */
	public boolean sendToDevice(String deviceKey, JSONObject data) {
		try {
			// 调用处理器的编码方法
			Method encodeMethod = handlerInstance.getClass()
				.getMethod("encodeData", String.class, JSONObject.class, JSONObject.class);
			
			JSONObject context = deviceContextMap.get(deviceKey);
			Object result = encodeMethod.invoke(handlerInstance, deviceKey, data, context);
			
			if (result instanceof byte[]) {
				byte[] encodedData = (byte[]) result;
				// TODO: 实际发送数据
				log.info("自定义驱动发送数据到设备: {}, 长度: {}", deviceKey, encodedData.length);
				return true;
			}
			return false;
		} catch (Exception e) {
			log.error("自定义驱动发送数据失败: {}", deviceKey, e);
			return false;
		}
	}

	/**
	 * 数据接收处理（子类可重写）
	 */
	protected void onDataReceived(String deviceKey, JSONObject data) {
		log.info("自定义驱动设备 [{}] 收到数据: {}", deviceKey, data);
	}

	@Override
	public JSONObject readData(String deviceKey, JSONObject params) throws Exception {
		if (handlerInstance == null) {
			throw new IllegalStateException("自定义处理器未初始化");
		}
		
		try {
			Method readMethod = handlerInstance.getClass()
				.getMethod("readData", String.class, JSONObject.class, JSONObject.class);
			
			JSONObject context = deviceContextMap.get(deviceKey);
			Object result = readMethod.invoke(handlerInstance, deviceKey, params, context);
			
			if (result instanceof JSONObject) {
				return (JSONObject) result;
			}
			return JSONUtil.createObj().set("result", result);
		} catch (NoSuchMethodException e) {
			throw new UnsupportedOperationException("自定义处理器未实现 readData 方法");
		}
	}

	@Override
	public boolean writeData(String deviceKey, JSONObject data) throws Exception {
		if (handlerInstance == null) {
			log.warn("自定义处理器未初始化");
			return false;
		}
		
		return sendToDevice(deviceKey, data);
	}

	@Override
	public boolean isDeviceOnline(String deviceKey) {
		// 自定义驱动的在线状态由处理器决定
		if (handlerInstance == null) {
			return false;
		}
		
		try {
			Method onlineMethod = handlerInstance.getClass()
				.getMethod("isDeviceOnline", String.class, JSONObject.class);
			
			JSONObject context = deviceContextMap.get(deviceKey);
			Object result = onlineMethod.invoke(handlerInstance, deviceKey, context);
			
			if (result instanceof Boolean) {
				return (Boolean) result;
			}
		} catch (NoSuchMethodException e) {
			// 没有实现在线检测方法，默认认为在线
		} catch (Exception e) {
			log.error("检查设备在线状态失败", e);
		}
		
		// 默认判断是否已注册
		return deviceContextMap.containsKey(deviceKey);
	}

	/**
	 * 执行自定义命令
	 */
	public Object executeCommand(String command, JSONObject params) {
		try {
			Method commandMethod = handlerInstance.getClass()
				.getMethod("executeCommand", String.class, JSONObject.class);
			return commandMethod.invoke(handlerInstance, command, params);
		} catch (Exception e) {
			log.error("执行自定义命令失败: {}", command, e);
			return null;
		}
	}

	/**
	 * 获取驱动状态
	 */
	public JSONObject getStatus() {
		try {
			Method statusMethod = handlerInstance.getClass().getMethod("getStatus");
			Object result = statusMethod.invoke(handlerInstance);
			if (result instanceof JSONObject) {
				return (JSONObject) result;
			}
		} catch (NoSuchMethodException e) {
			// 没有getStatus方法，返回默认状态
		} catch (Exception e) {
			log.error("获取驱动状态失败", e);
		}
		
		// 返回默认状态
		return JSONUtil.createObj()
			.set("running", isRunning())
			.set("devices", deviceContextMap.size())
			.set("handlerClass", handlerClass);
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
			"handlerClass", "处理器类名", "text", null, 
			null, null, null, 
			"请输入处理器类名", "如: com.example.MyProtocolHandler", 24,
			"driver", true
		));
		
		fields.add(new DriverConfigField(
			"customConfig", "自定义配置", "textarea", null, 
			null, null, null, 
			"请输入JSON格式的自定义配置", "传递给处理器的配置参数", 24,
			"driver", false
		));
		
		return fields;
	}
}
