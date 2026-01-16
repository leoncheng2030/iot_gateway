package vip.xiaonuo.iot.core.driver.spi.provider;

import vip.xiaonuo.iot.core.config.DriverConfigField;
import vip.xiaonuo.iot.core.driver.DeviceDriver;
import vip.xiaonuo.iot.core.driver.DriverConfig;
import vip.xiaonuo.iot.core.driver.impl.MqttDriver;
import vip.xiaonuo.iot.core.driver.spi.DriverProvider;
import vip.xiaonuo.iot.core.driver.spi.DriverProviderInfo;

import java.util.List;

/**
 * MQTT驱动提供者
 * 
 * @author wqs
 * @date 2026/01/15
 */
public class MqttDriverProvider implements DriverProvider {
    
    @Override
    public String getDriverType() {
        return "MQTT";
    }
    
    @Override
    public String getDriverName() {
        return "MQTT驱动";
    }
    
    @Override
    public String getDriverDescription() {
        return "MQTT协议驱动，支持设备通过MQTT协议接入网关";
    }
    
    @Override
    public DeviceDriver createDriver(DriverConfig config) {
        return new MqttDriver(config);
    }
    
    @Override
    public List<DriverConfigField> getConfigFields() {
        return MqttDriver.getStaticConfigFields();
    }
    
    @Override
    public String getProtocolVersion() {
        return "3.1.1";
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
