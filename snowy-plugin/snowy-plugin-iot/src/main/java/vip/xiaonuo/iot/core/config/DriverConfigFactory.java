/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 */
package vip.xiaonuo.iot.core.config;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 驱动配置工厂
 * 负责获取各驱动类型的配置字段定义
 * 支持动态注册，无需硬编码
 *
 * @author jetox
 * @date 2025/12/13
 */
@Slf4j
public class DriverConfigFactory {

    /**
     * 驱动配置提供者映射表
     */
    private static final Map<String, Supplier<List<DriverConfigField>>> CONFIG_PROVIDERS = new HashMap<>();

    /**
     * 获取指定驱动类型的配置字段
     *
     * @param driverType 驱动类型
     * @return 配置字段列表
     */
    public static List<DriverConfigField> getConfigFields(String driverType) {
        log.info("开始获取驱动配置 - driverType: {}", driverType);
        
        Supplier<List<DriverConfigField>> provider = CONFIG_PROVIDERS.get(driverType);
        if (provider == null) {
            log.info("缓存中未找到，尝试反射自动发现 - driverType: {}", driverType);
            // 如果未找到，尝试通过反射自动发现
            provider = discoverConfigProvider(driverType);
            if (provider != null) {
                CONFIG_PROVIDERS.put(driverType, provider);
                log.info("自动发现成功，已缓存 - driverType: {}", driverType);
            }
        } else {
            log.info("从缓存中获取 - driverType: {}", driverType);
        }
        
        if (provider == null) {
            log.warn("未找到驱动类型 {} 的配置提供者", driverType);
            return new ArrayList<>();
        }
        
        try {
            List<DriverConfigField> fields = provider.get();
            log.info("成功获取配置字段 - driverType: {}, 字段数量: {}", driverType, fields.size());
            return fields;
        } catch (Exception e) {
            log.error("获取驱动配置失败: {}", driverType, e);
            return new ArrayList<>();
        }
    }

    /**
     * 注册驱动配置提供者
     *
     * @param driverType 驱动类型
     * @param provider 配置提供者
     */
    public static void registerConfigProvider(String driverType, Supplier<List<DriverConfigField>> provider) {
        CONFIG_PROVIDERS.put(driverType, provider);
        log.info("注册驱动配置提供者: {}", driverType);
    }

    /**
     * 通过反射自动发现驱动配置提供者
     * 根据驱动类型自动查找对应的Driver类并调用其getStaticConfigFields方法
     */
    private static Supplier<List<DriverConfigField>> discoverConfigProvider(String driverType) {
        try {
            // 将驱动类型转换为类名 (如: MODBUS_TCP -> ModbusTcpDriver)
            String className = convertTypeToClassName(driverType);
            String fullClassName = "vip.xiaonuo.iot.core.driver.impl." + className;
            
            log.info("尝试加载驱动类 - driverType: {}, className: {}, fullClassName: {}", 
                driverType, className, fullClassName);
            
            // 通过反射加载类
            Class<?> driverClass = Class.forName(fullClassName);
            
            // 获取静态方法 getStaticConfigFields
            var method = driverClass.getMethod("getStaticConfigFields");
            
            log.info("找到驱动类和配置方法 - driverType: {}, class: {}", 
                driverType, driverClass.getName());
            
            // 返回方法引用的Supplier
            return () -> {
                try {
                    @SuppressWarnings("unchecked")
                    List<DriverConfigField> result = (List<DriverConfigField>) method.invoke(null);
                    return result;
                } catch (Exception e) {
                    log.error("反射调用配置方法失败: {}", driverType, e);
                    return new ArrayList<>();
                }
            };
        } catch (ClassNotFoundException e) {
            log.warn("未找到驱动类: {}", driverType);
            return null;
        } catch (NoSuchMethodException e) {
            log.warn("驱动类未实现getStaticConfigFields方法: {}", driverType);
            return null;
        } catch (Exception e) {
            log.error("自动发现驱动配置提供者失败: {}", driverType, e);
            return null;
        }
    }

    /**
     * 将驱动类型转换为类名
     * 例如: MODBUS_TCP -> ModbusTcpDriver
     *       DTU_GATEWAY -> DtuGatewayDriver
     */
    private static String convertTypeToClassName(String driverType) {
        if (driverType == null || driverType.isEmpty()) {
            return "";
        }
        
        // 特殊处理映射
        Map<String, String> specialMappings = Map.of(
            "MQTT_GATEWAY", "MqttDriver",
            "OPC_UA", "OpcUaDriver",
            "HTTP", "HttpDriver",
            "CUSTOM", "CustomDriver"
        );
        
        if (specialMappings.containsKey(driverType)) {
            return specialMappings.get(driverType);
        }
        
        // 通用转换逻辑: MODBUS_TCP -> ModbusTcpDriver
        String[] parts = driverType.split("_");
        StringBuilder className = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                className.append(Character.toUpperCase(part.charAt(0)));
                className.append(part.substring(1).toLowerCase());
            }
        }
        className.append("Driver");
        return className.toString();
    }
}
