/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 */
package vip.xiaonuo.iot.core.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import vip.xiaonuo.iot.modular.device.entity.IotDevice;
import vip.xiaonuo.iot.modular.devicedriver.entity.IotDeviceDriver;
import vip.xiaonuo.iot.modular.devicedriverrel.entity.IotDeviceDriverRel;

/**
 * 驱动配置工具类
 * 
 * 功能：
 * 1. 从驱动extJson中获取通用配置（轮询间隔、超时时间等）
 * 2. 支持设备级配置覆盖驱动配置
 * 3. 提供默认值兜底
 *
 * @author jetox
 * @date 2025/12/13
 */
public class DriverConfigUtil {

    /** 默认轮询间隔（秒） */
    private static final int DEFAULT_POLLING_INTERVAL = 5;

    /** 默认超时时间（秒） */
    private static final int DEFAULT_TIMEOUT = 3;

    /** 默认重试次数 */
    private static final int DEFAULT_RETRY_TIMES = 3;

    /**
     * 获取设备实际轮询间隔
     * 优先级：设备级配置(deviceConfig) > 驱动配置(configJson) > 系统默认值
     *
     * @param driverRel 设备驱动关联（可为null）
     * @param driver 驱动（可为null）
     * @return 轮询间隔（秒）
     */
    public static int getPollingInterval(IotDeviceDriverRel driverRel, IotDeviceDriver driver) {
        // 1. 优先使用设备级配置（device_config）
        if (driverRel != null && StrUtil.isNotBlank(driverRel.getDeviceConfig())) {
            try {
                JSONObject deviceConfig = JSONUtil.parseObj(driverRel.getDeviceConfig());
                Integer pollingInterval = deviceConfig.getInt("pollingInterval");
                if (pollingInterval != null && pollingInterval > 0) {
                    return pollingInterval;
                }
            } catch (Exception e) {
                // 解析失败，继续使用驱动配置
            }
        }

        // 2. 使用驱动配置
        if (driver != null && StrUtil.isNotBlank(driver.getConfigJson())) {
            try {
                JSONObject configJson = JSONUtil.parseObj(driver.getConfigJson());
                Integer driverPolling = configJson.getInt("defaultPollingInterval");
                if (driverPolling != null && driverPolling > 0) {
                    return driverPolling;
                }
            } catch (Exception e) {
                // 解析失败，继续使用默认值
            }
        }

        // 3. 系统默认值
        return DEFAULT_POLLING_INTERVAL;
    }

    /**
     * 获取设备实际超时时间
     * 优先级：设备级配置(deviceConfig) > 驱动配置(configJson) > 系统默认值
     *
     * @param driverRel 设备驱动关联（可为null）
     * @param driver 驱动（可为null）
     * @return 超时时间（秒）
     */
    public static int getTimeout(IotDeviceDriverRel driverRel, IotDeviceDriver driver) {
        // 1. 优先使用设备级配置（device_config）
        if (driverRel != null && StrUtil.isNotBlank(driverRel.getDeviceConfig())) {
            try {
                JSONObject deviceConfig = JSONUtil.parseObj(driverRel.getDeviceConfig());
                Integer timeout = deviceConfig.getInt("timeout");
                if (timeout != null && timeout > 0) {
                    return timeout;
                }
            } catch (Exception e) {
                // 解析失败，继续使用驱动配置
            }
        }

        // 2. 使用驱动配置
        if (driver != null && StrUtil.isNotBlank(driver.getConfigJson())) {
            try {
                JSONObject configJson = JSONUtil.parseObj(driver.getConfigJson());
                Integer driverTimeout = configJson.getInt("defaultTimeout");
                if (driverTimeout != null && driverTimeout > 0) {
                    return driverTimeout;
                }
            } catch (Exception e) {
                // 解析失败，继续使用默认值
            }
        }

        // 3. 系统默认值
        return DEFAULT_TIMEOUT;
    }

    /**
     * 获取驱动默认轮询间隔
     *
     * @param driver 驱动
     * @return 默认轮询间隔（秒）
     */
    public static int getDriverDefaultPollingInterval(IotDeviceDriver driver) {
        if (driver == null || StrUtil.isBlank(driver.getConfigJson())) {
            return DEFAULT_POLLING_INTERVAL;
        }

        try {
            JSONObject configJson = JSONUtil.parseObj(driver.getConfigJson());
            Integer pollingInterval = configJson.getInt("defaultPollingInterval");
            return pollingInterval != null && pollingInterval > 0 ? pollingInterval : DEFAULT_POLLING_INTERVAL;
        } catch (Exception e) {
            return DEFAULT_POLLING_INTERVAL;
        }
    }

    /**
     * 获取驱动默认超时时间
     *
     * @param driver 驱动
     * @return 默认超时时间（秒）
     */
    public static int getDriverDefaultTimeout(IotDeviceDriver driver) {
        if (driver == null || StrUtil.isBlank(driver.getConfigJson())) {
            return DEFAULT_TIMEOUT;
        }

        try {
            JSONObject configJson = JSONUtil.parseObj(driver.getConfigJson());
            Integer timeout = configJson.getInt("defaultTimeout");
            return timeout != null && timeout > 0 ? timeout : DEFAULT_TIMEOUT;
        } catch (Exception e) {
            return DEFAULT_TIMEOUT;
        }
    }

    /**
     * 设置驱动Modbus配置（轮询间隔、超时时间）
     * 注意：会保留configJson中的其他配置
     *
     * @param driver 驱动
     * @param pollingInterval 默认轮询间隔
     * @param timeout 默认超时时间
     */
    public static void setDriverModbusConfig(IotDeviceDriver driver, Integer pollingInterval, Integer timeout) {
        JSONObject configJson;
        if (StrUtil.isNotBlank(driver.getConfigJson())) {
            try {
                configJson = JSONUtil.parseObj(driver.getConfigJson());
            } catch (Exception e) {
                configJson = new JSONObject();
            }
        } else {
            configJson = new JSONObject();
        }

        if (pollingInterval != null && pollingInterval > 0) {
            configJson.set("defaultPollingInterval", pollingInterval);
        }
        if (timeout != null && timeout > 0) {
            configJson.set("defaultTimeout", timeout);
        }

        driver.setConfigJson(configJson.toString());
    }

    /**
     * 从设备扩展配置中获取Modbus从站地址
     * 
     * @param device 设备
     * @return Modbus从站地址，默认1
     */
    public static int getModbusSlaveAddress(IotDevice device) {
        if (device == null || StrUtil.isBlank(device.getExtJson())) {
            return 1; // 默认从站地址
        }
        
        try {
            JSONObject extJson = JSONUtil.parseObj(device.getExtJson());
            Integer slaveAddress = extJson.getInt("modbusSlaveAddress");
            return slaveAddress != null && slaveAddress > 0 ? slaveAddress : 1;
        } catch (Exception e) {
            return 1;
        }
    }

    /**
     * 从设备扩展配置中获取超时时间
     * 
     * @param device 设备
     * @return 超时时间(秒)，默认3秒
     */
    public static int getDeviceTimeout(IotDevice device) {
        if (device == null || StrUtil.isBlank(device.getExtJson())) {
            return DEFAULT_TIMEOUT;
        }
        
        try {
            JSONObject extJson = JSONUtil.parseObj(device.getExtJson());
            Integer timeout = extJson.getInt("timeout");
            return timeout != null && timeout > 0 ? timeout : DEFAULT_TIMEOUT;
        } catch (Exception e) {
            return DEFAULT_TIMEOUT;
        }
    }

    /**
     * 从驱动关联配置或设备扩展配置中获取IP地址
     * 优先级：驱动关联device_config > 设备extJson
     * 
     * @param driverRel 设备驱动关联（可为null）
     * @param device 设备（可为null）
     * @return IP地址，可能为null
     */
    public static String getIpAddress(IotDeviceDriverRel driverRel, IotDevice device) {
        // 1. 优先使用驱动关联配置（device_config中的ip字段）
        if (driverRel != null && StrUtil.isNotBlank(driverRel.getDeviceConfig())) {
            try {
                JSONObject deviceConfig = JSONUtil.parseObj(driverRel.getDeviceConfig());
                String ip = deviceConfig.getStr("ip");
                if (StrUtil.isNotBlank(ip)) {
                    return ip;
                }
            } catch (Exception e) {
                // 解析失败，继续使用设备配置
            }
        }
        
        // 2. 使用设备扩展配置（extJson中的ipAddress字段）
        if (device != null && StrUtil.isNotBlank(device.getExtJson())) {
            try {
                JSONObject extJson = JSONUtil.parseObj(device.getExtJson());
                return extJson.getStr("ipAddress");
            } catch (Exception e) {
                return null;
            }
        }
        
        return null;
    }
    
    /**
     * 从设备扩展配置中获取IP地址（兼容旧版本调用）
     * 
     * @param device 设备
     * @return IP地址，可能为null
     */
    public static String getIpAddress(IotDevice device) {
        return getIpAddress(null, device);
    }

    /**
     * 从驱动关联配置或设备扩展配置中获取端口
     * 优先级：驱动关联device_config > 设备extJson
     * 
     * @param driverRel 设备驱动关联（可为null）
     * @param device 设备（可为null）
     * @return 端口，默认502
     */
    public static int getPort(IotDeviceDriverRel driverRel, IotDevice device) {
        // 1. 优先使用驱动关联配置（device_config中的port字段）
        if (driverRel != null && StrUtil.isNotBlank(driverRel.getDeviceConfig())) {
            try {
                JSONObject deviceConfig = JSONUtil.parseObj(driverRel.getDeviceConfig());
                Integer port = deviceConfig.getInt("port");
                if (port != null && port > 0) {
                    return port;
                }
            } catch (Exception e) {
                // 解析失败，继续使用设备配置
            }
        }
        
        // 2. 使用设备扩展配置（extJson中的port字段）
        if (device != null && StrUtil.isNotBlank(device.getExtJson())) {
            try {
                JSONObject extJson = JSONUtil.parseObj(device.getExtJson());
                Integer port = extJson.getInt("port");
                return port != null && port > 0 ? port : 502;
            } catch (Exception e) {
                return 502;
            }
        }
        
        return 502; // Modbus TCP默认端口
    }
    
    /**
     * 从设备扩展配置中获取端口（兼容旧版本调用）
     * 
     * @param device 设备
     * @return 端口，默认502
     */
    public static int getPort(IotDevice device) {
        return getPort(null, device);
    }
}
