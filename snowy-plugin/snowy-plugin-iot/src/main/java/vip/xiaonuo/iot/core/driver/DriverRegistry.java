/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 */
package vip.xiaonuo.iot.core.driver;

import cn.hutool.core.util.ClassUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vip.xiaonuo.iot.core.driver.annotation.Driver;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 驱动注册中心
 * 通过扫描注解自动注册驱动类
 *
 * @author xiaonuo
 * @date 2026/01/10
 */
@Slf4j
@Component
public class DriverRegistry {

    /**
     * 驱动类型 -> 驱动类全名映射
     */
    private static final Map<String, String> DRIVER_CLASS_MAP = new ConcurrentHashMap<>();

    /**
     * 驱动类型 -> 驱动信息映射
     */
    private static final Map<String, DriverInfo> DRIVER_INFO_MAP = new ConcurrentHashMap<>();

    static {
        scanAndRegisterDrivers();
    }

    /**
     * 扫描并注册所有驱动
     */
    private static void scanAndRegisterDrivers() {
        log.info("开始扫描驱动类...");
        
        // 扫描指定包下的所有类
        Set<Class<?>> classes = ClassUtil.scanPackageByAnnotation(
            "vip.xiaonuo.iot.core.driver.impl", 
            Driver.class
        );
        
        for (Class<?> clazz : classes) {
            Driver driverAnnotation = clazz.getAnnotation(Driver.class);
            if (driverAnnotation != null) {
                String type = driverAnnotation.type();
                String name = driverAnnotation.name();
                String description = driverAnnotation.description();
                
                // 注册驱动类
                DRIVER_CLASS_MAP.put(type, clazz.getName());
                
                // 注册驱动信息
                DriverInfo info = new DriverInfo(type, name, description, clazz.getName());
                DRIVER_INFO_MAP.put(type, info);
                
                log.info("发现驱动: {} - {} ({})", type, name, clazz.getSimpleName());
            }
        }
        
        log.info("驱动扫描完成，共发现 {} 个驱动", DRIVER_CLASS_MAP.size());
    }

    /**
     * 根据驱动类型获取驱动类全名
     */
    public static String getDriverClass(String driverType) {
        return DRIVER_CLASS_MAP.get(driverType);
    }

    /**
     * 根据驱动类型获取驱动信息
     */
    public static DriverInfo getDriverInfo(String driverType) {
        return DRIVER_INFO_MAP.get(driverType);
    }

    /**
     * 获取所有已注册的驱动类型
     */
    public static Set<String> getAllDriverTypes() {
        return DRIVER_CLASS_MAP.keySet();
    }

    /**
     * 获取所有驱动信息
     */
    public static Map<String, DriverInfo> getAllDriverInfo() {
        return new ConcurrentHashMap<>(DRIVER_INFO_MAP);
    }

    /**
     * 检查驱动类型是否已注册
     */
    public static boolean isRegistered(String driverType) {
        return DRIVER_CLASS_MAP.containsKey(driverType);
    }

    /**
     * 驱动信息
     */
    public static class DriverInfo {
        private final String type;
        private final String name;
        private final String description;
        private final String className;

        public DriverInfo(String type, String name, String description, String className) {
            this.type = type;
            this.name = name;
            this.description = description;
            this.className = className;
        }

        public String getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getClassName() {
            return className;
        }
    }
}
