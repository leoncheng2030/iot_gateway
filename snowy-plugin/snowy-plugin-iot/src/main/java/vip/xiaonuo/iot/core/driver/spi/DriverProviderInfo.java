package vip.xiaonuo.iot.core.driver.spi;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 驱动提供者信息
 * 
 * @author wqs
 * @date 2026/01/15
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverProviderInfo {
    
    /** 提供者类名 */
    private String className;
    
    /** 版本号 */
    private String version;
    
    /** 供应商 */
    private String vendor;
    
    /** 描述 */
    private String description;
    
    /** 加载时间 */
    private long loadTime;
    
    /** 是否为内置驱动 */
    private boolean builtin;
    
    /**
     * 简单构造函数
     */
    public DriverProviderInfo(String className, String version, String vendor) {
        this.className = className;
        this.version = version;
        this.vendor = vendor;
        this.loadTime = System.currentTimeMillis();
        this.builtin = true;
    }
    
    /**
     * 外部驱动构造函数
     */
    public static DriverProviderInfo external(String className, String version, String vendor, String description) {
        DriverProviderInfo info = new DriverProviderInfo();
        info.setClassName(className);
        info.setVersion(version);
        info.setVendor(vendor);
        info.setDescription(description);
        info.setLoadTime(System.currentTimeMillis());
        info.setBuiltin(false);
        return info;
    }
}
