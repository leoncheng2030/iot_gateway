package vip.xiaonuo.iot.core.driver.spi;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 驱动SPI加载器
 * 负责加载和管理所有驱动提供者
 * 
 * @author wqs
 * @date 2026/01/15
 */
@Slf4j
@Component
public class DriverSpiLoader {
    
    /** 已注册的驱动提供者 <driverType, DriverProvider> */
    private final Map<String, DriverProvider> providers = new ConcurrentHashMap<>();
    
    /** 加载状态 */
    private volatile boolean loaded = false;
    
    @PostConstruct
    public void loadDrivers() {
        if (loaded) {
            log.warn("驱动已加载，跳过重复加载");
            return;
        }
        
        log.info("开始加载驱动SPI...");
        
        try {
            // 使用Java SPI机制加载
            ServiceLoader<DriverProvider> loader = ServiceLoader.load(DriverProvider.class);
            
            List<DriverProvider> providerList = new ArrayList<>();
            for (DriverProvider provider : loader) {
                providerList.add(provider);
            }
            
            // 按优先级排序（数值越小优先级越高）
            providerList.sort(Comparator.comparingInt(DriverProvider::getPriority));
            
            for (DriverProvider provider : providerList) {
                try {
                    registerProvider(provider);
                } catch (Exception e) {
                    log.error("注册驱动失败: {}", provider.getClass().getName(), e);
                }
            }
            
            loaded = true;
            log.info("驱动SPI加载完成，共{}个驱动: {}", providers.size(), providers.keySet());
            
        } catch (Exception e) {
            log.error("加载驱动SPI失败", e);
        }
    }
    
    /**
     * 注册驱动提供者
     * 
     * @param provider 驱动提供者
     */
    private void registerProvider(DriverProvider provider) {
        String type = provider.getDriverType();
        
        if (type == null || type.isBlank()) {
            log.warn("驱动类型为空，跳过: {}", provider.getClass().getName());
            return;
        }
        
        if (providers.containsKey(type)) {
            DriverProvider existing = providers.get(type);
            if (provider.getPriority() < existing.getPriority()) {
                log.info("驱动类型[{}]已存在，使用更高优先级的驱动: {} -> {}", 
                    type, existing.getClass().getName(), provider.getClass().getName());
                providers.put(type, provider);
            } else {
                log.warn("驱动类型[{}]已存在，跳过低优先级驱动: {}", type, provider.getClass().getName());
            }
            return;
        }
        
        providers.put(type, provider);
        log.info("加载驱动: type={}, name={}, class={}, priority={}", 
            type, provider.getDriverName(), provider.getClass().getName(), provider.getPriority());
    }
    
    /**
     * 获取驱动提供者
     * 
     * @param driverType 驱动类型
     * @return 驱动提供者，不存在返回null
     */
    public DriverProvider getProvider(String driverType) {
        return providers.get(driverType);
    }
    
    /**
     * 获取所有驱动类型
     * 
     * @return 驱动类型集合
     */
    public Set<String> getAllDriverTypes() {
        return Collections.unmodifiableSet(providers.keySet());
    }
    
    /**
     * 获取所有驱动提供者
     * 
     * @return 驱动提供者集合
     */
    public Collection<DriverProvider> getAllProviders() {
        return Collections.unmodifiableCollection(providers.values());
    }
    
    /**
     * 检查驱动类型是否存在
     * 
     * @param driverType 驱动类型
     * @return true表示存在
     */
    public boolean hasDriver(String driverType) {
        return providers.containsKey(driverType);
    }
    
    /**
     * 注册外部驱动（支持动态加载）
     * 
     * @param provider 驱动提供者
     */
    public void registerDriver(DriverProvider provider) {
        String type = provider.getDriverType();
        
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("驱动类型不能为空");
        }
        
        if (providers.containsKey(type)) {
            log.warn("驱动类型已存在，将被覆盖: {}", type);
        }
        
        providers.put(type, provider);
        log.info("注册外部驱动: type={}, name={}", type, provider.getDriverName());
    }
    
    /**
     * 卸载驱动
     * 
     * @param driverType 驱动类型
     * @return true表示卸载成功
     */
    public boolean unregisterDriver(String driverType) {
        DriverProvider removed = providers.remove(driverType);
        if (removed != null) {
            log.info("卸载驱动: type={}", driverType);
            return true;
        }
        return false;
    }
    
    /**
     * 获取驱动信息列表
     * 
     * @return 驱动信息列表
     */
    public List<DriverInfo> getDriverInfoList() {
        List<DriverInfo> list = new ArrayList<>();
        
        providers.forEach((type, provider) -> {
            DriverInfo info = new DriverInfo();
            info.setType(type);
            info.setName(provider.getDriverName());
            info.setDescription(provider.getDriverDescription());
            info.setProtocolVersion(provider.getProtocolVersion());
            info.setProviderInfo(provider.getProviderInfo());
            info.setConfigFields(provider.getConfigFields());
            info.setSupportsHotUpdate(provider.supportsHotUpdate());
            list.add(info);
        });
        
        return list;
    }
    
    /**
     * 重新加载驱动
     */
    public void reload() {
        log.info("重新加载驱动SPI...");
        providers.clear();
        loaded = false;
        loadDrivers();
    }
    
    /**
     * 驱动信息
     */
    @lombok.Data
    public static class DriverInfo {
        private String type;
        private String name;
        private String description;
        private String protocolVersion;
        private DriverProviderInfo providerInfo;
        private List<vip.xiaonuo.iot.core.config.DriverConfigField> configFields;
        private boolean supportsHotUpdate;
    }
}
