package vip.xiaonuo.iot.core.driver.spi.provider;

import vip.xiaonuo.iot.core.config.DriverConfigField;
import vip.xiaonuo.iot.core.driver.DeviceDriver;
import vip.xiaonuo.iot.core.driver.DriverConfig;
import vip.xiaonuo.iot.core.driver.impl.HttpDriver;
import vip.xiaonuo.iot.core.driver.spi.DriverProvider;
import vip.xiaonuo.iot.core.driver.spi.DriverProviderInfo;

import java.util.List;

/**
 * HTTP驱动提供者
 * 
 * @author wqs
 * @date 2026/01/15
 */
public class HttpDriverProvider implements DriverProvider {
    
    @Override
    public String getDriverType() {
        return "HTTP";
    }
    
    @Override
    public String getDriverName() {
        return "HTTP驱动";
    }
    
    @Override
    public String getDriverDescription() {
        return "HTTP协议驱动，支持RESTful API设备接入";
    }
    
    @Override
    public DeviceDriver createDriver(DriverConfig config) {
        return new HttpDriver(config);
    }
    
    @Override
    public List<DriverConfigField> getConfigFields() {
        return HttpDriver.getStaticConfigFields();
    }
    
    @Override
    public String getProtocolVersion() {
        return "1.1";
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
