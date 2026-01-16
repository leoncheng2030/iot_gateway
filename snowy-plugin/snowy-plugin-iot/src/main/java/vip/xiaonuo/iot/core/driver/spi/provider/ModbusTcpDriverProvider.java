package vip.xiaonuo.iot.core.driver.spi.provider;

import vip.xiaonuo.iot.core.config.DriverConfigField;
import vip.xiaonuo.iot.core.driver.DeviceDriver;
import vip.xiaonuo.iot.core.driver.DriverConfig;
import vip.xiaonuo.iot.core.driver.impl.ModbusTcpDriver;
import vip.xiaonuo.iot.core.driver.spi.DriverProvider;
import vip.xiaonuo.iot.core.driver.spi.DriverProviderInfo;

import java.util.List;

/**
 * Modbus TCP驱动提供者
 * 
 * @author wqs
 * @date 2026/01/15
 */
public class ModbusTcpDriverProvider implements DriverProvider {
    
    @Override
    public String getDriverType() {
        return "MODBUS_TCP";
    }
    
    @Override
    public String getDriverName() {
        return "Modbus TCP驱动";
    }
    
    @Override
    public String getDriverDescription() {
        return "支持Modbus TCP协议的工业设备驱动，适用于PLC、传感器、仪表等设备";
    }
    
    @Override
    public DeviceDriver createDriver(DriverConfig config) {
        return new ModbusTcpDriver(config);
    }
    
    @Override
    public List<DriverConfigField> getConfigFields() {
        return ModbusTcpDriver.getStaticConfigFields();
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
    
    @Override
    public boolean supportsHotUpdate() {
        return false;
    }
}
