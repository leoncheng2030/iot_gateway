package vip.xiaonuo.iot.core.driver.spi;

import vip.xiaonuo.iot.core.config.DriverConfigField;
import vip.xiaonuo.iot.core.driver.DeviceDriver;
import vip.xiaonuo.iot.core.driver.DriverConfig;

import java.util.List;

/**
 * 驱动提供者SPI接口
 * 第三方可通过实现此接口扩展自定义驱动
 * 
 * @author wqs
 * @date 2026/01/15
 */
public interface DriverProvider {
    
    /**
     * 获取驱动类型（唯一标识）
     * 
     * @return 驱动类型，如 "MODBUS_TCP", "S7", "MQTT" 等
     */
    String getDriverType();
    
    /**
     * 获取驱动名称（显示名称）
     * 
     * @return 驱动名称
     */
    String getDriverName();
    
    /**
     * 获取驱动描述
     * 
     * @return 驱动描述信息
     */
    String getDriverDescription();
    
    /**
     * 创建驱动实例
     * 
     * @param config 驱动配置
     * @return 驱动实例
     */
    DeviceDriver createDriver(DriverConfig config);
    
    /**
     * 获取驱动配置字段定义
     * 用于动态生成配置表单
     * 
     * @return 配置字段列表
     */
    List<DriverConfigField> getConfigFields();
    
    /**
     * 获取驱动支持的协议版本
     * 
     * @return 协议版本号
     */
    default String getProtocolVersion() {
        return "1.0";
    }
    
    /**
     * 获取驱动提供者信息
     * 
     * @return 提供者信息
     */
    default DriverProviderInfo getProviderInfo() {
        return new DriverProviderInfo(
            getClass().getName(),
            "1.0.0",
            "Unknown"
        );
    }
    
    /**
     * 驱动优先级（数值越小优先级越高）
     * 当多个驱动提供相同类型时，使用优先级最高的
     * 
     * @return 优先级
     */
    default int getPriority() {
        return 100;
    }
    
    /**
     * 是否支持热更新
     * 
     * @return true表示支持在运行时更新配置
     */
    default boolean supportsHotUpdate() {
        return false;
    }
    
    /**
     * 验证配置有效性
     * 
     * @param config 配置对象
     * @return 验证结果，null表示验证通过，否则返回错误信息
     */
    default String validateConfig(DriverConfig config) {
        return null;
    }
}
