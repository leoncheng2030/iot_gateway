/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 */
package vip.xiaonuo.iot.core.driver;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.iot.modular.devicedriver.entity.IotDeviceDriver;
import vip.xiaonuo.iot.modular.devicedriver.enums.IotDeviceDriverEnum;
import vip.xiaonuo.iot.modular.devicedriver.service.IotDeviceDriverService;
import vip.xiaonuo.iot.modular.driverlog.entity.IotDriverLog;
import vip.xiaonuo.iot.modular.driverlog.service.IotDriverLogService;
import vip.xiaonuo.iot.core.driver.impl.DtuGatewayDriver;
import vip.xiaonuo.iot.core.driver.impl.TcpDirectDriver;
import vip.xiaonuo.iot.core.driver.impl.UdpDirectDriver;
import vip.xiaonuo.iot.core.driver.impl.ModbusTcpDriver;
import vip.xiaonuo.iot.core.driver.impl.MqttDriver;
import vip.xiaonuo.iot.core.driver.impl.HttpDriver;
import vip.xiaonuo.iot.core.driver.impl.LoraGatewayDriver;
import vip.xiaonuo.iot.core.driver.impl.ZigbeeGatewayDriver;
import vip.xiaonuo.iot.core.driver.impl.OpcUaDriver;
import vip.xiaonuo.iot.core.driver.impl.CustomDriver;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

/**
 * 设备驱动管理器
 * 
 * 功能：
 * 1. 驱动注册与卸载
 * 2. 驱动启动与停止
 * 3. 驱动状态监控
 * 4. 与数据库集成,实现持久化管理
 *
 * @author yubaoshan
 * @date 2025/12/13
 */
@Slf4j
@Component
public class DriverManager {

    @Resource
    private IotDeviceDriverService iotDeviceDriverService;
    
    @Resource
    private IotDriverLogService iotDriverLogService;
    
    @Autowired
    private AutowireCapableBeanFactory autowireCapableBeanFactory;

    /** 启动时是否自动加载驱动 */
    @Value("${iot.driver.auto-start:true}")
    private boolean autoStart;

    /** 已注册的驱动 driverId -> DeviceDriver */
    private final Map<String, DeviceDriver> drivers = new ConcurrentHashMap<>();
    
    /** 驱动启动时间 driverId -> startTime */
    private final Map<String, Long> driverStartTimes = new ConcurrentHashMap<>();

    /**
     * 系统启动时自动加载并启动所有启用的驱动
     */
    @PostConstruct
    public void init() {
        // 检查是否开启自动启动
        if (!autoStart) {
            log.info("驱动自动启动已禁用，跳过加载（可通过前端手动启动）");
            return;
        }
        
        log.info("开始加载驱动配置...");
        try {
            // 查询所有运行中的驱动
            iotDeviceDriverService.lambdaQuery()
                .eq(IotDeviceDriver::getStatus, IotDeviceDriverEnum.RUNNING.getValue())
                .list()
                .forEach(driver -> {
                    try {
                        loadAndStartDriver(driver);
                        // 记录启动成功日志
                        recordLog(driver.getId(), driver.getDriverName(), "START", "系统启动时自动加载驱动成功", null, null);
                    } catch (Exception e) {
                        log.error("驱动 [{}] 启动失败: {}", driver.getDriverName(), e.getMessage());
                        // 更新数据库状态为ERROR
                        updateDriverStatus(driver.getId(), IotDeviceDriverEnum.ERROR.getValue());
                        // 记录错误日志
                        recordLog(driver.getId(), driver.getDriverName(), "ERROR", "系统启动时自动加载驱动失败", null, e.getMessage());
                    }
                });
            
            log.info("驱动配置加载完成，已启动 {} 个驱动服务", drivers.size());
        } catch (Exception e) {
            log.error("驱动配置加载失败", e);
        }
    }

    /**
     * 加载并启动驱动
     */
    private void loadAndStartDriver(IotDeviceDriver driverEntity) throws Exception {
        // 解析配置
        DriverConfig config = parseConfig(driverEntity);
        
        // 根据驱动类型创建驱动实例
        DeviceDriver driver = createDriver(driverEntity.getDriverType(), config);
        
        // 启动驱动
        long startTime = System.currentTimeMillis();
        driver.start();
        
        // 缓存实例和启动时间
        drivers.put(driverEntity.getId(), driver);
        driverStartTimes.put(driverEntity.getId(), startTime);
        
        log.info("驱动 [{}] 启动成功 - 类型: {}", 
                driverEntity.getDriverName(), 
                driverEntity.getDriverType());
    }

    /**
     * 根据驱动类型创建驱动实例
     */
    private DeviceDriver createDriver(String driverType, DriverConfig config) {
        DeviceDriver driver = switch (driverType) {
            case "DTU_GATEWAY" -> new DtuGatewayDriver(config);
            case "TCP_DIRECT" -> new TcpDirectDriver(config);
            case "UDP_DIRECT" -> new UdpDirectDriver(config);
            case "MODBUS_TCP" -> new ModbusTcpDriver(config);
            case "MQTT" -> new MqttDriver(config);
            case "HTTP" -> new HttpDriver(config);
            case "LORA_GATEWAY" -> new LoraGatewayDriver(config);
            case "ZIGBEE_GATEWAY" -> new ZigbeeGatewayDriver(config);
            case "OPCUA" -> new OpcUaDriver(config);
            case "CUSTOM" -> new CustomDriver(config);
            default -> throw new CommonException("不支持的驱动类型: {}", driverType);
        };
        
        // 手动注入Spring依赖
        autowireCapableBeanFactory.autowireBean(driver);
        
        return driver;
    }

    /**
     * 解析驱动配置
     */
    private DriverConfig parseConfig(IotDeviceDriver driverEntity) {
        DriverConfig config = new DriverConfig();
        config.setDriverId(driverEntity.getId());
        config.setDriverName(driverEntity.getDriverName());
        config.setDriverType(driverEntity.getDriverType());
        
        // 解析JSON配置
        if (ObjectUtil.isNotEmpty(driverEntity.getConfigJson())) {
            Map<String, Object> configMap = JSONUtil.toBean(driverEntity.getConfigJson(), Map.class);
            config.setConfigMap(configMap);
        }
        
        return config;
    }

    /**
     * 更新驱动状态到数据库
     */
    private void updateDriverStatus(String driverId, String status) {
        try {
            IotDeviceDriver driver = new IotDeviceDriver();
            driver.setId(driverId);
            driver.setStatus(status);
            iotDeviceDriverService.updateById(driver);
        } catch (Exception e) {
            log.error("更新驱动状态失败", e);
        }
    }

    /**
     * 启动驱动（对外接口）
     */
    public void startDriver(String driverId) throws Exception {
        // 查询驱动配置
        IotDeviceDriver driverEntity = iotDeviceDriverService.queryEntity(driverId);
        if (ObjectUtil.isEmpty(driverEntity)) {
            throw new CommonException("驱动不存在");
        }

        // 检查是否已经启动
        if (drivers.containsKey(driverId)) {
            log.warn("驱动 [{}] 已在运行中", driverEntity.getDriverName());
            throw new CommonException("驱动服务已在运行中");
        }

        try {
            // 加载并启动
            loadAndStartDriver(driverEntity);
            
            // 更新数据库状态
            updateDriverStatus(driverId, IotDeviceDriverEnum.RUNNING.getValue());
            
            // 记录启动日志
            recordLog(driverId, driverEntity.getDriverName(), "START", "手动启动驱动成功", null, null);
            
        } catch (Exception e) {
            // 启动失败，更新状态为ERROR
            updateDriverStatus(driverId, IotDeviceDriverEnum.ERROR.getValue());
            // 记录错误日志
            recordLog(driverId, driverEntity.getDriverName(), "ERROR", "驱动启动失败", null, e.getMessage());
            throw new CommonException("驱动启动失败: {}", e.getMessage());
        }
    }

    /**
     * 停止驱动（对外接口）
     */
    public void stopDriver(String driverId) throws Exception {
        DeviceDriver driver = drivers.get(driverId);
        if (ObjectUtil.isEmpty(driver)) {
            throw new CommonException("驱动服务未运行");
        }

        IotDeviceDriver driverEntity = iotDeviceDriverService.queryEntity(driverId);
        
        try {
            driver.stop();
            drivers.remove(driverId);
            driverStartTimes.remove(driverId);

            // 更新数据库状态
            updateDriverStatus(driverId, IotDeviceDriverEnum.STOPPED.getValue());
            
            // 记录停止日志
            recordLog(driverId, driverEntity.getDriverName(), "STOP", "手动停止驱动", null, null);
            
            log.info("驱动 [{}] 已停止", driverEntity.getDriverName());

        } catch (Exception e) {
            log.error("驱动停止失败", e);
            // 记录错误日志
            recordLog(driverId, driverEntity.getDriverName(), "ERROR", "驱动停止失败", null, e.getMessage());
            throw new CommonException("驱动停止失败: {}", e.getMessage());
        }
    }

    /**
     * 重启驱动
     */
    public void restartDriver(String driverId) throws Exception {
        if (drivers.containsKey(driverId)) {
            stopDriver(driverId);
        }
        startDriver(driverId);
    }

    /**
     * 注册驱动
     */
    public void registerDriver(String driverId, DeviceDriver driver) {
        if (drivers.containsKey(driverId)) {
            log.warn("驱动 [{}] 已存在，将被覆盖", driverId);
        }
        drivers.put(driverId, driver);
        log.info("驱动 [{}] 注册成功: {}", driverId, driver.getDriverName());
    }

    /**
     * 卸载驱动
     */
    public void unregisterDriver(String driverId) throws Exception {
        DeviceDriver driver = drivers.remove(driverId);
        if (driver != null) {
            if (driver.isRunning()) {
                driver.stop();
            }
            log.info("驱动 [{}] 已卸载", driverId);
        }
    }

    /**
     * 获取驱动
     */
    public DeviceDriver getDriver(String driverId) {
        return drivers.get(driverId);
    }

    /**
     * 获取所有驱动
     */
    public Map<String, DeviceDriver> getAllDrivers() {
        return new ConcurrentHashMap<>(drivers);
    }

    /**
     * 检查驱动是否运行
     */
    public boolean isDriverRunning(String driverId) {
        DeviceDriver driver = drivers.get(driverId);
        return driver != null && driver.isRunning();
    }

    /**
     * 获取驱动实例
     */
    public DeviceDriver getDriverInstance(String driverId) {
        return drivers.get(driverId);
    }

    /**
     * 停止所有驱动
     */
    public void stopAllDrivers() {
        for (Map.Entry<String, DeviceDriver> entry : drivers.entrySet()) {
            try {
                if (entry.getValue().isRunning()) {
                    entry.getValue().stop();
                }
            } catch (Exception e) {
                log.error("停止驱动 [{}] 失败", entry.getKey(), e);
            }
        }
    }
    
    /**
     * 记录驱动运行日志
     */
    private void recordLog(String driverId, String driverName, String logType, 
                           String logContent, String deviceKey, String errorMsg) {
        try {
            IotDriverLog driverLog = new IotDriverLog();
            driverLog.setDriverId(driverId);
            driverLog.setDriverName(driverName);
            driverLog.setLogType(logType);
            driverLog.setLogContent(logContent);
            driverLog.setDeviceKey(deviceKey);
            driverLog.setErrorMsg(errorMsg);
            
            // 性能监控数据
            if ("START".equals(logType) || "STOP".equals(logType)) {
                Long startTime = driverStartTimes.get(driverId);
                if (startTime != null) {
                    long uptime = System.currentTimeMillis() - startTime;
                    Map<String, Object> extData = new HashMap<>();
                    extData.put("uptime", uptime);
                    extData.put("startTime", startTime);
                    driverLog.setExtJson(JSONUtil.toJsonStr(extData));
                }
            }
            
            iotDriverLogService.save(driverLog);
        } catch (Exception e) {
            log.error("记录驱动日志失败", e);
        }
    }
    
    /**
     * 获取驱动运行时长(毫秒)
     */
    public long getDriverUptime(String driverId) {
        Long startTime = driverStartTimes.get(driverId);
        if (startTime == null) {
            return 0;
        }
        return System.currentTimeMillis() - startTime;
    }
}
