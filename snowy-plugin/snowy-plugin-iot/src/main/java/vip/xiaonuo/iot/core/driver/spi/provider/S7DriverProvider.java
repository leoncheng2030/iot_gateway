package vip.xiaonuo.iot.core.driver.spi.provider;

import vip.xiaonuo.iot.core.config.DriverConfigField;
import vip.xiaonuo.iot.core.driver.DeviceDriver;
import vip.xiaonuo.iot.core.driver.DriverConfig;
import vip.xiaonuo.iot.core.driver.impl.S7Driver;
import vip.xiaonuo.iot.core.driver.spi.DriverProvider;
import vip.xiaonuo.iot.core.driver.spi.DriverProviderInfo;

import java.util.List;

/**
 * S7驱动提供者（西门子PLC）
 * 
 * @author wqs
 * @date 2026/01/15
 */
public class S7DriverProvider implements DriverProvider {
    
    @Override
    public String getDriverType() {
        return "S7";
    }
    
    @Override
    public String getDriverName() {
        return "S7驱动";
    }
    
    @Override
    public String getDriverDescription() {
        return "西门子S7系列PLC驱动，支持S7-200/300/400/1200/1500系列";
    }
    
    @Override
    public DeviceDriver createDriver(DriverConfig config) {
        return new S7Driver(config);
    }
    
    @Override
    public List<DriverConfigField> getConfigFields() {
        return S7Driver.getStaticConfigFields();
    }
    
    @Override
    public String getProtocolVersion() {
        return "1.0";
    }
    
    @Override
    public DriverProviderInfo getProviderInfo() {
        return new DriverProviderInfo(
            getClass().getName(),
            "3.0.0",
            "WQS IoT"
        );
    }
    
    @Override
    public int getPriority() {
        return 10;
    }
}
