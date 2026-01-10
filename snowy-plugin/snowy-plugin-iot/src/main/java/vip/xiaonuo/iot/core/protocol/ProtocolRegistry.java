/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 */
package vip.xiaonuo.iot.core.protocol;

import cn.hutool.core.util.ClassUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vip.xiaonuo.iot.core.protocol.annotation.Protocol;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 协议注册中心
 * 通过扫描注解自动注册协议类
 *
 * @author xiaonuo
 * @date 2026/01/10
 */
@Slf4j
@Component
public class ProtocolRegistry {

    /**
     * 协议类型 -> 协议类全名映射
     */
    private static final Map<String, String> PROTOCOL_CLASS_MAP = new ConcurrentHashMap<>();

    /**
     * 协议类型 -> 协议信息映射
     */
    private static final Map<String, ProtocolInfo> PROTOCOL_INFO_MAP = new ConcurrentHashMap<>();

    static {
        scanAndRegisterProtocols();
    }

    /**
     * 扫描并注册所有协议
     */
    private static void scanAndRegisterProtocols() {
        log.info("开始扫描协议类...");
        
        // 扫描 protocol 包及其所有子包
        Set<Class<?>> classes = ClassUtil.scanPackageByAnnotation(
            "vip.xiaonuo.iot.core.protocol", 
            Protocol.class
        );
        
        for (Class<?> clazz : classes) {
            Protocol protocolAnnotation = clazz.getAnnotation(Protocol.class);
            if (protocolAnnotation != null) {
                String type = protocolAnnotation.type();
                String name = protocolAnnotation.name();
                String description = protocolAnnotation.description();
                
                // 注册协议类
                PROTOCOL_CLASS_MAP.put(type, clazz.getName());
                
                // 注册协议信息
                ProtocolInfo info = new ProtocolInfo(type, name, description, clazz.getName());
                PROTOCOL_INFO_MAP.put(type, info);
                
                log.info("发现协议: {} - {} ({})", type, name, clazz.getSimpleName());
            }
        }
        
        log.info("协议扫描完成，共发现 {} 个协议", PROTOCOL_CLASS_MAP.size());
    }

    /**
     * 根据协议类型获取协议类全名
     */
    public static String getProtocolClass(String protocolType) {
        return PROTOCOL_CLASS_MAP.get(protocolType);
    }

    /**
     * 根据协议类型获取协议信息
     */
    public static ProtocolInfo getProtocolInfo(String protocolType) {
        return PROTOCOL_INFO_MAP.get(protocolType);
    }

    /**
     * 获取所有已注册的协议类型
     */
    public static Set<String> getAllProtocolTypes() {
        return PROTOCOL_CLASS_MAP.keySet();
    }

    /**
     * 获取所有协议信息
     */
    public static Map<String, ProtocolInfo> getAllProtocolInfo() {
        return new ConcurrentHashMap<>(PROTOCOL_INFO_MAP);
    }

    /**
     * 检查协议类型是否已注册
     */
    public static boolean isRegistered(String protocolType) {
        return PROTOCOL_CLASS_MAP.containsKey(protocolType);
    }

    /**
     * 协议信息
     */
    public static class ProtocolInfo {
        private final String type;
        private final String name;
        private final String description;
        private final String className;

        public ProtocolInfo(String type, String name, String description, String className) {
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
