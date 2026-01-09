package vip.xiaonuo.iot.core.protocol.modbus;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.BatchRead;
import com.serotonin.modbus4j.BatchResults;
import com.serotonin.modbus4j.exception.ErrorResponseException;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.ip.IpParameters;
import com.serotonin.modbus4j.locator.BaseLocator;
import com.serotonin.modbus4j.code.DataType;
import com.serotonin.modbus4j.msg.WriteRegistersRequest;
import com.serotonin.modbus4j.msg.WriteCoilsRequest;
import jakarta.annotation.Resource;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vip.xiaonuo.iot.core.message.DeviceDataHandler;
import vip.xiaonuo.iot.core.message.RuleEngineService;
import vip.xiaonuo.iot.core.util.DriverConfigUtil;
import vip.xiaonuo.iot.modular.device.entity.IotDevice;
import vip.xiaonuo.iot.modular.device.service.IotDeviceService;
import vip.xiaonuo.iot.modular.devicedriverrel.entity.IotDeviceDriverRel;
import vip.xiaonuo.iot.modular.product.service.IotProductService;
import vip.xiaonuo.iot.modular.register.entity.IotDeviceRegisterMapping;
import vip.xiaonuo.iot.modular.register.service.IotDeviceRegisterMappingService;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于Modbus4J的Modbus TCP客户端
 * 
 * @author jetox
 * @date 2025/12/12
 */
@Slf4j
@Component
public class Modbus4jTcpClient {

    @Resource
    private IotProductService iotProductService;

    @Resource
    private IotDeviceRegisterMappingService iotDeviceRegisterMappingService;

    @Resource
    private DeviceDataHandler deviceDataHandler;

    @Resource
    private IotDeviceService iotDeviceService;
    
    @Resource
    private RuleEngineService ruleEngineService;

    // 连接信息内部类
    private static class ConnectionInfo {
        ModbusMaster master;
        long lastActiveTime;
        
        ConnectionInfo(ModbusMaster master) {
            this.master = master;
            this.lastActiveTime = System.currentTimeMillis();
        }
        
        void updateActiveTime() {
            this.lastActiveTime = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - lastActiveTime > CONNECTION_EXPIRE_TIME;
        }
    }

    // 设备连接缓存 <deviceId, ConnectionInfo>
    private final Map<String, ConnectionInfo> deviceMasters = new ConcurrentHashMap<>();
    
    // 最大连接数限制
    private static final int MAX_CONNECTIONS = 200;
    
    // 连接过期时间（30分钟无活动）
    private static final long CONNECTION_EXPIRE_TIME = 30 * 60 * 1000;
    
    // 定期清理线程池
    private final ScheduledExecutorService cleanupExecutor = Executors.newScheduledThreadPool(1, r -> {
        Thread thread = new Thread(r, "Modbus-Connection-Cleanup");
        thread.setDaemon(true);
        return thread;
    });
    
    // 初始化时启动定期清理任务
    public Modbus4jTcpClient() {
        // 每10分钟执行一次清理
        cleanupExecutor.scheduleWithFixedDelay(this::cleanupExpiredConnections, 
                10, 10, TimeUnit.MINUTES);
    }
    
    /**
     * 清理过期连接
     */
    private void cleanupExpiredConnections() {
        long currentTime = System.currentTimeMillis();
        List<String> expiredDevices = new ArrayList<>();
        
        for (Map.Entry<String, ConnectionInfo> entry : deviceMasters.entrySet()) {
            ConnectionInfo info = entry.getValue();
            if (currentTime - info.lastActiveTime > CONNECTION_EXPIRE_TIME) {
                expiredDevices.add(entry.getKey());
            }
        }
        
        // 移除过期连接
        for (String deviceId : expiredDevices) {
            disconnect(deviceId);
        }
        
        if (!expiredDevices.isEmpty()) {
            // 清理了过期连接
        }
    }
    
    /**
     * 获取当前连接数
     */
    public int getConnectionCount() {
        return deviceMasters.size();
    }

    /**
     * 连接Modbus设备（内部方法，使用驱动关联配置）
     * 
     * @param device 设备对象
     * @param driverRel 驱动关联对象（包含设备级配置）
     * @return ModbusMaster实例
     */
    private ModbusMaster getOrConnect(IotDevice device, IotDeviceDriverRel driverRel) {
        // 检查是否已连接
        ConnectionInfo existingInfo = deviceMasters.get(device.getId());
        if (existingInfo != null && existingInfo.master != null && !existingInfo.isExpired()) {
            existingInfo.updateActiveTime();
            return existingInfo.master;
        }
        
        // 检查连接数限制
        if (deviceMasters.size() >= MAX_CONNECTIONS) {
            return null;
        }

        String host = DriverConfigUtil.getIpAddress(driverRel, device);
        if (host == null || host.isEmpty()) {
            return null;
        }

        // 获取IP和端口
        String ip = host;
        int port = DriverConfigUtil.getPort(driverRel, device);

        try {
            // 创建Modbus工厂
            ModbusFactory modbusFactory = new ModbusFactory();

            // 配置TCP参数
            IpParameters params = new IpParameters();
            params.setHost(ip);
            params.setPort(port);
            params.setEncapsulated(true); // Modbus RTU over TCP（封装模式）

            // 创建Modbus Master
            ModbusMaster master = modbusFactory.createTcpMaster(params, true);
            
            // 优化：使用设备配置的超时时间，默认3秒
            int timeout = DriverConfigUtil.getDeviceTimeout(device) * 1000;
            master.setTimeout(timeout);
            master.setRetries(2); // 重试2次
            
            // 初始化连接
            master.init();

            // 缓存连接
            deviceMasters.put(device.getId(), new ConnectionInfo(master));

            // 连接成功后,将设备状态更新为ONLINE并更新最后在线时间
            if ("INACTIVE".equals(device.getDeviceStatus()) || "OFFLINE".equals(device.getDeviceStatus())) {
                String previousStatus = device.getDeviceStatus();
                device.setDeviceStatus("ONLINE");
                device.setLastOnlineTime(new java.util.Date());
                
                // 首次激活
                if (device.getActiveTime() == null) {
                    device.setActiveTime(new java.util.Date());
                }
                
                iotDeviceService.updateById(device);
                
                // 推送SSE消息到前端
                deviceDataHandler.pushDeviceStatus(device, "ONLINE");
                
                // 根据之前的状态记录不同的日志
                if ("OFFLINE".equals(previousStatus)) {
                    log.info("Modbus设备重连成功 - DeviceKey: {}, IP: {}:{}", device.getDeviceKey(), ip, port);
                } else {
                    log.info("Modbus设备首次连接成功 - DeviceKey: {}, IP: {}:{}", device.getDeviceKey(), ip, port);
                }
            }

            return master;

        } catch (ModbusInitException e) {
            // 区分首次连接和离线重连，使用不同的日志级别
            if ("OFFLINE".equals(device.getDeviceStatus())) {
                // 离线设备重连失败，使用DEBUG级别，避免日志刷屏
                log.debug("Modbus离线设备重连失败 - DeviceKey: {}, IP: {}:{}", 
                    device.getDeviceKey(), ip, port);
            } else {
                // 首次连接或在线设备断连，记录ERROR日志
                log.error("Modbus连接初始化失败 - DeviceKey: {}, IP: {}:{}, 原因: {}", 
                    device.getDeviceKey(), ip, port, e.getMessage());
            }
            return null;
        }
    }
    
    /**
     * 连接Modbus设备（公共方法，使用设备extJson配置）
     */
    public ModbusMaster connect(IotDevice device) {
        // 检查是否已连接
        ConnectionInfo existingInfo = deviceMasters.get(device.getId());
        if (existingInfo != null) {
            existingInfo.updateActiveTime();
            return existingInfo.master;
        }
        
        // 检查连接数限制
        if (deviceMasters.size() >= MAX_CONNECTIONS) {
            return null;
        }

        String host = DriverConfigUtil.getIpAddress(device);
        if (host == null || host.isEmpty()) {
            return null;
        }

        // 获取IP和端口
        String ip = host;
        int port = DriverConfigUtil.getPort(device);

        try {
            // 创建Modbus工厂
            ModbusFactory modbusFactory = new ModbusFactory();

            // 配置TCP参数
            IpParameters params = new IpParameters();
            params.setHost(ip);
            params.setPort(port);
            params.setEncapsulated(true); // Modbus RTU over TCP（封装模式）

            // 创建Modbus Master
            ModbusMaster master = modbusFactory.createTcpMaster(params, true);
            
            // 优化：使用设备配置的超时时间，默认3秒
            int timeout = DriverConfigUtil.getDeviceTimeout(device) * 1000;
            master.setTimeout(timeout);
            master.setRetries(2); // 重试2次
            
            // 初始化连接
            master.init();

            // 缓存连接
            deviceMasters.put(device.getId(), new ConnectionInfo(master));

            // 连接成功后,将设备状态更新为ONLINE并更新最后在线时间
            if ("INACTIVE".equals(device.getDeviceStatus()) || "OFFLINE".equals(device.getDeviceStatus())) {
                String previousStatus = device.getDeviceStatus();
                device.setDeviceStatus("ONLINE");
                device.setLastOnlineTime(new java.util.Date());
                
                // 首次激活
                if (device.getActiveTime() == null) {
                    device.setActiveTime(new java.util.Date());
                }
                
                iotDeviceService.updateById(device);
                
                // 推送SSE消息到前端
                deviceDataHandler.pushDeviceStatus(device, "ONLINE");
                
                // 根据之前的状态记录不同的日志
                if ("OFFLINE".equals(previousStatus)) {
                    log.info("Modbus设备重连成功 - DeviceKey: {}, IP: {}:{}", device.getDeviceKey(), ip, port);
                } else {
                    log.info("Modbus设备首次连接成功 - DeviceKey: {}, IP: {}:{}", device.getDeviceKey(), ip, port);
                }
            }

            return master;

        } catch (ModbusInitException e) {
            // 区分首次连接和离线重连，使用不同的日志级别
            if ("OFFLINE".equals(device.getDeviceStatus())) {
                // 离线设备重连失败，使用DEBUG级别，避免日志刷屏
                log.debug("Modbus离线设备重连失败 - DeviceKey: {}, IP: {}:{}", 
                    device.getDeviceKey(), ip, port);
            } else {
                // 首次连接或在线设备断连，记录ERROR日志
                log.error("Modbus连接初始化失败 - DeviceKey: {}, IP: {}:{}, 原因: {}", 
                    device.getDeviceKey(), ip, port, e.getMessage());
            }
            return null;
        }
    }

    /**
     * 断开设备连接
     */
    public void disconnect(String deviceId) {
        ConnectionInfo info = deviceMasters.remove(deviceId);
        if (info != null && info.master != null) {
            info.master.destroy();
        }
    }

    /**
     * 读取线圈（功能码0x01）
     * 
     * @param device 设备对象
     * @param driverRel 驱动关联对象（包含设备级配置）
     * @param startAddress 起始地址
     * @param quantity 数量
     */
    public void readCoils(IotDevice device, IotDeviceDriverRel driverRel, int startAddress, int quantity) {
        ModbusMaster master = getOrConnect(device, driverRel);
        if (master == null) {
            throw new RuntimeException("无法连接到设备 " + DriverConfigUtil.getIpAddress(driverRel, device) + ", 请检查设备IP地址和网络连接");
        }

        try {
            int slaveId = DriverConfigUtil.getModbusSlaveAddress(device);
            
            // 读取线圈状态（使用BatchRead）
            BatchRead<Integer> batchRead = new BatchRead<>();
            for (int i = 0; i < quantity; i++) {
                batchRead.addLocator(i, 
                    BaseLocator.coilStatus(slaveId, startAddress + i));
            }
            
            BatchResults<Integer> results = master.send(batchRead);
            
            // 转换为boolean数组
            boolean[] values = new boolean[quantity];
            for (int i = 0; i < quantity; i++) {
                Object value = results.getValue(i);
                values[i] = value != null && (Boolean) value;
            }
            
            // 解析数据
            Map<String, Object> parsedData = parseBitData(device, startAddress, values, "0x01");
            
            if (!parsedData.isEmpty()) {
                reportData(device, parsedData);
            }

        } catch (ModbusTransportException | ErrorResponseException e) {
            // 抛出异常，让轮询任务处理离线逻辑
            throw new RuntimeException("Modbus读取失败: " + e.getMessage(), e);
        }
    }

    /**
     * 读取离散输入（功能码0x02）
     * 
     * @param device 设备对象
     * @param driverRel 驱动关联对象（包含设备级配置）
     * @param startAddress 起始地址
     * @param quantity 数量
     */
    public void readDiscreteInputs(IotDevice device, IotDeviceDriverRel driverRel, int startAddress, int quantity) {
        ModbusMaster master = getOrConnect(device, driverRel);
        if (master == null) {
            throw new RuntimeException("无法连接到设备 " + DriverConfigUtil.getIpAddress(driverRel, device) + ", 请检查设备IP地址和网络连接");
        }

        try {
            int slaveId = DriverConfigUtil.getModbusSlaveAddress(device);
            
            // 读取离散输入状态（使用BatchRead）
            BatchRead<Integer> batchRead = new BatchRead<>();
            for (int i = 0; i < quantity; i++) {
                batchRead.addLocator(i, 
                    BaseLocator.inputStatus(slaveId, startAddress + i));
            }
            
            BatchResults<Integer> results = master.send(batchRead);
            
            // 转换为boolean数组
            boolean[] values = new boolean[quantity];
            for (int i = 0; i < quantity; i++) {
                Object value = results.getValue(i);
                values[i] = value != null && (Boolean) value;
            }
            
            // 解析数据
            Map<String, Object> parsedData = parseBitData(device, startAddress, values, "0x02");
            
            if (!parsedData.isEmpty()) {
                reportData(device, parsedData);
            }

        } catch (ModbusTransportException | ErrorResponseException e) {
            // 抛出异常，让轮询任务处理离线逻辑
            throw new RuntimeException("Modbus读取失败: " + e.getMessage(), e);
        }
    }

    /**
     * 读取保持寄存器（功能码0x03）
     * 
     * @param device 设备对象
     * @param driverRel 驱动关联对象（包含设备级配置）
     * @param startAddress 起始地址
     * @param quantity 数量
     */
    public void readHoldingRegisters(IotDevice device, IotDeviceDriverRel driverRel, int startAddress, int quantity) {
        ModbusMaster master = getOrConnect(device, driverRel);
        if (master == null) {
            throw new RuntimeException("无法连接到设备 " + DriverConfigUtil.getIpAddress(driverRel, device) + ", 请检查设备IP地址和网络连接");
        }

        try {
            int slaveId = DriverConfigUtil.getModbusSlaveAddress(device);
            
            // 读取保持寄存器（使用BatchRead）
            BatchRead<Integer> batchRead = new BatchRead<>();
            for (int i = 0; i < quantity; i++) {
                batchRead.addLocator(i, 
                    BaseLocator.holdingRegister(slaveId, startAddress + i, DataType.TWO_BYTE_INT_UNSIGNED));
            }
            
            BatchResults<Integer> results = master.send(batchRead);
            
            // 转换为short数组
            short[] values = new short[quantity];
            for (int i = 0; i < quantity; i++) {
                Object value = results.getValue(i);
                values[i] = value != null ? ((Number) value).shortValue() : 0;
            }
            
            // 解析数据
            Map<String, Object> parsedData = parseRegisterData(device, startAddress, values, "0x03");
            
            if (!parsedData.isEmpty()) {
                reportData(device, parsedData);
            }

        } catch (ModbusTransportException | ErrorResponseException e) {
            // 抛出异常，让轮询任务处理离线逻辑
            throw new RuntimeException("Modbus读取失败: " + e.getMessage(), e);
        }
    }

    /**
     * 读取输入寄存器（功能码0x04）
     * 
     * @param device 设备对象
     * @param driverRel 驱动关联对象（包含设备级配置）
     * @param startAddress 起始地址
     * @param quantity 数量
     */
    public void readInputRegisters(IotDevice device, IotDeviceDriverRel driverRel, int startAddress, int quantity) {
        ModbusMaster master = getOrConnect(device, driverRel);
        if (master == null) {
            throw new RuntimeException("无法连接到设备 " + DriverConfigUtil.getIpAddress(driverRel, device) + ", 请检查设备IP地址和网络连接");
        }

        try {
            int slaveId = DriverConfigUtil.getModbusSlaveAddress(device);
            
            // 读取输入寄存器（使用BatchRead）
            BatchRead<Integer> batchRead = new BatchRead<>();
            for (int i = 0; i < quantity; i++) {
                batchRead.addLocator(i, 
                    BaseLocator.inputRegister(slaveId, startAddress + i, DataType.TWO_BYTE_INT_UNSIGNED));
            }
            
            BatchResults<Integer> results = master.send(batchRead);
            
            // 转换为short数组
            short[] values = new short[quantity];
            for (int i = 0; i < quantity; i++) {
                Object value = results.getValue(i);
                values[i] = value != null ? ((Number) value).shortValue() : 0;
            }
            
            // 解析数据
            Map<String, Object> parsedData = parseRegisterData(device, startAddress, values, "0x04");
            
            if (!parsedData.isEmpty()) {
                reportData(device, parsedData);
            }

        } catch (ModbusTransportException | ErrorResponseException e) {
            // 抛出异常，让轮询任务处理离线逻辑
            throw new RuntimeException("Modbus读取失败: " + e.getMessage(), e);
        }
    }

    /**
     * 写单个线圈（功能码0x05）
     */
    public void writeSingleCoil(IotDevice device, int coilAddress, boolean value) {
    	ModbusMaster master = getOrConnect(device);
    	if (master == null) return;
    
    	try {
    		int slaveId = DriverConfigUtil.getModbusSlaveAddress(device);
    			
    		// 写单个线圈
    		master.setValue(
    			BaseLocator.coilStatus(slaveId, coilAddress),
    			value
    		);
    
    	} catch (ModbusTransportException | ErrorResponseException e) {
    		throw new RuntimeException("写单个线圈失败: " + e.getMessage(), e);
    	}
    }
    
    /**
     * 写多个线圈（功能码0x0F）
     */
    public void writeMultipleCoils(IotDevice device, int startAddress, boolean[] values) {
    	ModbusMaster master = getOrConnect(device);
    	if (master == null) return;
    
    	try {
    		int slaveId = DriverConfigUtil.getModbusSlaveAddress(device);
    			
    		// 使用WriteCoilsRequest写多个线圈
    		WriteCoilsRequest request = new WriteCoilsRequest(slaveId, startAddress, values);
    		master.send(request);
    
    	} catch (ModbusTransportException e) {
    		throw new RuntimeException("写多个线圈失败: " + e.getMessage(), e);
    	}
    }
    
    /**
     * 写单个寄存器（功能码0x06）
     */
    public void writeSingleRegister(IotDevice device, int registerAddress, int value) {
        ModbusMaster master = getOrConnect(device);
        if (master == null) return;

        try {
            int slaveId = DriverConfigUtil.getModbusSlaveAddress(device);
            
            // 写单个寄存器
            master.setValue(
                BaseLocator.holdingRegister(slaveId, registerAddress, DataType.TWO_BYTE_INT_UNSIGNED),
                value
            );

        } catch (ModbusTransportException | ErrorResponseException e) {
            // 静默处理
        }
    }

    /**
     * 写多个寄存器（功能码0x10）
     */
    public void writeMultipleRegisters(IotDevice device, int startAddress, int[] values) {
        ModbusMaster master = getOrConnect(device);
        if (master == null) return;

        try {
            int slaveId = DriverConfigUtil.getModbusSlaveAddress(device);
            
            // 转换为short数组
            short[] shortValues = new short[values.length];
            for (int i = 0; i < values.length; i++) {
                shortValues[i] = (short) values[i];
            }
            
            // 使用WriteRegistersRequest写多个寄存器
            WriteRegistersRequest request = new WriteRegistersRequest(slaveId, startAddress, shortValues);
            master.send(request);

        } catch (ModbusTransportException e) {
            throw new RuntimeException("写多个寄存器失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析位数据（线圈/离散输入）
     */
    private Map<String, Object> parseBitData(IotDevice device, int startAddress, boolean[] values, String functionCode) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 加载设备的寄存器映射（优先设备级）
            Map<Integer, IotDeviceRegisterMapping> mappingMap = iotDeviceRegisterMappingService.getRegisterMappingByFunctionCode(device.getId(), functionCode);
            
            if (mappingMap.isEmpty()) {
                return result;
            }

            // 解析每个位
            for (int i = 0; i < values.length; i++) {
                int address = startAddress + i;
                IotDeviceRegisterMapping mapping = mappingMap.get(address);
                
                if (mapping != null) {
                    result.put(mapping.getIdentifier(), values[i]);
                }
            }

        } catch (Exception e) {
            // 静默处理
        }
        
        return result;
    }

    /**
     * 解析寄存器数据
     */
    private Map<String, Object> parseRegisterData(IotDevice device, int startAddress, short[] values, String functionCode) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 加载设备的寄存器映射（优先设备级）
            Map<Integer, IotDeviceRegisterMapping> mappingMap = iotDeviceRegisterMappingService.getRegisterMappingByFunctionCode(device.getId(), functionCode);
            
            if (mappingMap.isEmpty()) {
                return result;
            }

            // 解析每个寄存器
            for (int i = 0; i < values.length; i++) {
                int address = startAddress + i;
                IotDeviceRegisterMapping mapping = mappingMap.get(address);
                
                if (mapping != null) {
                    try {
                        String dataType = mapping.getDataType() != null ? mapping.getDataType() : "int";
                        
                        int unsignedValue = values[i] & 0xFFFF; // 转为无符号
                        Object parsedValue = parseValueWithMapping(unsignedValue, mapping);
                        
                        result.put(mapping.getIdentifier(), parsedValue);
                    } catch (Exception e) {
                        // 静默处理
                    }
                }
            }

        } catch (Exception e) {
            // 静默处理
        }
        
        return result;
    }

    /**
     * 根据映射配置解析值(应用缩放系数和偏移量)
     */
    private Object parseValueWithMapping(int rawValue, IotDeviceRegisterMapping mapping) {
        String dataType = mapping.getDataType() != null ? mapping.getDataType() : "int";
        
        // 1. 处理位索引(用于布尔类型)
        if ("bool".equalsIgnoreCase(dataType) && mapping.getBitIndex() != null) {
            int bitIndex = mapping.getBitIndex();
            return ((rawValue >> bitIndex) & 1) == 1;
        }
        
        // 2. 基础类型转换
        Object baseValue = parseValue(rawValue, dataType);
        
        // 3. 布尔类型不需要缩放和偏移
        if ("bool".equalsIgnoreCase(dataType) || "boolean".equalsIgnoreCase(dataType)) {
            return baseValue;
        }
        
        // 4. 数值类型应用缩放系数和偏移量
        if (baseValue instanceof Number) {
            double numericValue = ((Number) baseValue).doubleValue();
            
            // 应用缩放系数 (默认1.0)
            if (mapping.getScaleFactor() != null) {
                numericValue = numericValue * mapping.getScaleFactor().doubleValue();
            }
            
            // 应用偏移量 (默认0.0)
            if (mapping.getOffset() != null) {
                numericValue = numericValue + mapping.getOffset().doubleValue();
            }
            
            // 根据数据类型返回
            if ("int".equalsIgnoreCase(dataType)) {
                return (int) Math.round(numericValue);
            } else {
                return numericValue;
            }
        }
        
        return baseValue;
    }

    /**
     * 解析值
     */
    private Object parseValue(int rawValue, String type) {
        switch (type) {
            case "bool":
            case "boolean":
                return rawValue != 0;
            case "float":
            case "double":
                return (double) rawValue;
            case "int":
            default:
                return rawValue;
        }
    }

    /**
     * 上报数据到前端
     */
    private void reportData(IotDevice device, Map<String, Object> data) {
        JSONObject dataJson = JSONUtil.createObj();
        dataJson.putAll(data);
        
        log.debug("Modbus轮询读取数据 - DeviceKey: {}, 解析数据: {}", device.getDeviceKey(), dataJson.toString());
        
        deviceDataHandler.handlePropertyData(device, dataJson);
        
        // 触发规则引擎
        ruleEngineService.triggerByDeviceData(device.getId(), dataJson);
        
        // 成功读取数据后,更新最后在线时间
        try {
            device.setLastOnlineTime(new java.util.Date());
            iotDeviceService.updateById(device);
        } catch (Exception e) {
            // 静默处理更新失败,不影响数据上报
        }
    }

    /**
     * 获取或创建连接
     */
    private ModbusMaster getOrConnect(IotDevice device) {
        ConnectionInfo info = deviceMasters.get(device.getId());
        if (info != null) {
            info.updateActiveTime();
            return info.master;
        }
        return connect(device);
    }

    /**
     * 关闭所有连接
     */
    @PreDestroy
    public void shutdown() {
        // 关闭清理线程池
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // 关闭所有Modbus连接
        deviceMasters.values().forEach(info -> {
            if (info.master != null) {
                info.master.destroy();
            }
        });
        deviceMasters.clear();
    }
}
