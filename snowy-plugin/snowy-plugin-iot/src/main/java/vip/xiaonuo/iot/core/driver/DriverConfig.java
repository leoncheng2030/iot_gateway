/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 */
package vip.xiaonuo.iot.core.driver;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 驱动配置类
 * 
 * @author yubaoshan
 * @date 2025/12/13
 */
@Data
public class DriverConfig {

    /** 驱动ID */
    private String driverId;

    /** 驱动名称 */
    private String driverName;

    /** 驱动类型 */
    private String driverType;

    /** 配置参数 Map */
    private Map<String, Object> configMap = new HashMap<>();

    /**
     * 获取配置参数
     */
    public Object getConfig(String key) {
        return configMap.get(key);
    }

    /**
     * 获取配置参数（带默认值）
     */
    public Object getConfig(String key, Object defaultValue) {
        return configMap.getOrDefault(key, defaultValue);
    }

    /**
     * 获取字符串配置
     */
    public String getString(String key) {
        Object value = configMap.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 获取整数配置
     */
    public Integer getInteger(String key) {
        Object value = configMap.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    /**
     * 获取布尔配置
     */
    public Boolean getBoolean(String key) {
        Object value = configMap.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return null;
    }
}
