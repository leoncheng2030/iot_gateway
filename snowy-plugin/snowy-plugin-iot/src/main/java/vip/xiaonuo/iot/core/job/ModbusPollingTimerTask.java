package vip.xiaonuo.iot.core.job;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vip.xiaonuo.common.timer.CommonTimerTaskRunner;
import vip.xiaonuo.iot.core.message.DeviceDataHandler;
import vip.xiaonuo.iot.core.protocol.modbus.Modbus4jTcpClient;
import vip.xiaonuo.iot.core.util.DriverConfigUtil;
import vip.xiaonuo.iot.modular.device.entity.IotDevice;
import vip.xiaonuo.iot.modular.device.service.IotDeviceService;
import vip.xiaonuo.iot.modular.devicedriver.entity.IotDeviceDriver;
import vip.xiaonuo.iot.modular.devicedriver.service.IotDeviceDriverService;
import vip.xiaonuo.iot.modular.devicedriverrel.entity.IotDeviceDriverRel;
import vip.xiaonuo.iot.modular.devicedriverrel.service.IotDeviceDriverRelService;
import vip.xiaonuo.iot.modular.device.entity.IotDeviceAddressConfig;
import vip.xiaonuo.iot.modular.device.service.IotDevicePropertyMappingService;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Modbus设备轮询定时任务（优化版 - 并发轮询）
 * 定时轮询所有在线的Modbus设备，读取寄存器数据
 * 性能优化：
 * - 使用线程池并发轮询，提升10倍+性能
 * - 智能重连策略：OFFLINE设备降低轮询频率
 * - 异常隔离：单个设备失败不影响其他设备
 *
 * @author jetox
 * &#064;date  2025/12/11
 */
@Slf4j
@Component
public class ModbusPollingTimerTask implements CommonTimerTaskRunner {

    @Resource
    private IotDeviceService iotDeviceService;

    @Resource
    private IotDeviceDriverService iotDeviceDriverService;

    @Resource
    private IotDeviceDriverRelService iotDeviceDriverRelService;

    @Resource
    private Modbus4jTcpClient modbusTcpClient;

    @Resource
    private IotDevicePropertyMappingService iotDevicePropertyMappingService;
    
    @Resource
    private DeviceDataHandler deviceDataHandler;
    
    /**
     * 轮询线程池
     * 核心线程数：10（根据CPU核心数调整）
     * 最大线程数：50（支持高并发轮询）
     * 队列容量：500（缓冲待轮询设备）
     */
    private ExecutorService pollingExecutor;
    
    /**
     * OFFLINE设备重连计数器（用于降低离线设备轮询频率）
     */
    private final Map<String, Integer> offlineRetryCount = new ConcurrentHashMap<>();
    
    /**
     * 初始化线程池
     */
    @PostConstruct
    public void init() {
        pollingExecutor = new ThreadPoolExecutor(
            10,  // 核心线程数
            50,  // 最大线程数
            60L, TimeUnit.SECONDS, // 空闲线程存活时间
            new LinkedBlockingQueue<>(500), // 任务队列
            new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, "modbus-polling-" + counter.getAndIncrement());
                    thread.setDaemon(true); // 设置为守护线程
                    return thread;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：调用者线程执行
        );
        log.info("Modbus轮询线程池初始化完成 - 核心线程数: 10, 最大线程数: 50");
    }
    
    /**
     * 销毁线程池
     */
    @PreDestroy
    public void destroy() {
        if (pollingExecutor != null) {
            pollingExecutor.shutdown();
            try {
                if (!pollingExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    pollingExecutor.shutdownNow();
                }
                log.info("Modbus轮询线程池已关闭");
            } catch (InterruptedException e) {
                pollingExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void action(String extJson) {
        try {
            // 1. 查询所有 MODBUS_TCP 类型且运行中的驱动
            LambdaQueryWrapper<IotDeviceDriver> driverQuery = new LambdaQueryWrapper<>();
            driverQuery.eq(IotDeviceDriver::getDriverType, "MODBUS_TCP")
                       .eq(IotDeviceDriver::getStatus, "RUNNING");
            List<IotDeviceDriver> modbusDrivers = iotDeviceDriverService.list(driverQuery);

            if (modbusDrivers.isEmpty()) {
                log.debug("没有运行中的Modbus驱动，跳过轮询");
                return;
            }
            
            // 2. 提取驱动ID列表
            List<String> driverIds = modbusDrivers.stream()
                .map(IotDeviceDriver::getId)
                .toList();
            
            // 3. 查询绑定了这些驱动的设备关联
            LambdaQueryWrapper<IotDeviceDriverRel> relQuery = new LambdaQueryWrapper<>();
            relQuery.in(IotDeviceDriverRel::getDriverId, driverIds);
            List<IotDeviceDriverRel> driverRels = iotDeviceDriverRelService.list(relQuery);
            
            if (driverRels.isEmpty()) {
                log.debug("没有设备绑定Modbus驱动，跳过轮询");
                return;
            }
            
            // 4. 构建设备ID到驱动关联的映射表
            Map<String, IotDeviceDriverRel> deviceDriverRelMap = driverRels.stream()
                .collect(Collectors.toMap(
                    IotDeviceDriverRel::getDeviceId,
                    rel -> rel,
                    (existing, replacement) -> existing  // 如果设备绑定多个驱动，取第一个
                ));
            
            // 5. 提取设备ID列表（去重）
            List<String> deviceIds = new ArrayList<>(deviceDriverRelMap.keySet());
            
            // 6. 查询这些设备（只轮询有效状态的设备）
            LambdaQueryWrapper<IotDevice> deviceQuery = new LambdaQueryWrapper<>();
            deviceQuery.in(IotDevice::getId, deviceIds)
                       .in(IotDevice::getDeviceStatus, "ONLINE", "INACTIVE", "OFFLINE");
            
            List<IotDevice> devices = iotDeviceService.list(deviceQuery);
            
            if (devices.isEmpty()) {
                return;
            }
            long startTime = System.currentTimeMillis();
            
            // 使用CompletableFuture并发轮询
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            
            for (IotDevice device : devices) {
                // 智能重连策略：OFFLINE设备降低轮询频率
                if ("OFFLINE".equals(device.getDeviceStatus())) {
                    Integer retryCount = offlineRetryCount.getOrDefault(device.getId(), 0);
                    // 每6次轮询（30秒）尝试一次重连
                    if (retryCount % 6 != 0) {
                        offlineRetryCount.put(device.getId(), retryCount + 1);
                        continue;
                    }
                    offlineRetryCount.put(device.getId(), retryCount + 1);
                }
                
                // 提交异步任务
                IotDeviceDriverRel driverRel = deviceDriverRelMap.get(device.getId());
                CompletableFuture<Void> future = CompletableFuture.runAsync(
                    () -> pollDevice(device, driverRel), 
                    pollingExecutor
                ).exceptionally(ex -> {
                    log.error("设备轮询任务执行失败 - DeviceKey: {}", device.getDeviceKey(), ex);
                    return null;
                });
                
                futures.add(future);
            }
            
            // 等待所有任务完成（最多等待15秒，适配慢速网络设备）
            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(15, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                log.warn("部分设备轮询超时（等待时间>15秒）");
            } catch (Exception e) {
                log.error("等待轮询任务完成异常", e);
            }
            
            long elapsed = System.currentTimeMillis() - startTime;
            log.debug("轮询完成 - 设备数: {}, 耗时: {}ms", devices.size(), elapsed);
            
        } catch (Exception e) {
            log.error("Modbus轮询任务异常", e);
        }
    }
    
    /**
     * 轮询单个设备
     * 
     * @param device 设备对象
     * @param driverRel 驱动关联对象（包含设备级配置）
     */
    private void pollDevice(IotDevice device, IotDeviceDriverRel driverRel) {
        try {
            // 获取设备的属性映射（优先设备级，无则使用产品级）
            List<Map<String, Object>> mappings = iotDevicePropertyMappingService.getDevicePropertyMappingsWithAddress(device.getId());
            
            // 按功能码分组寄存器地址
            Map<String, List<Integer>> functionCodeGroups = new HashMap<>();
            
            for (Map<String, Object> item : mappings) {
                IotDeviceAddressConfig config = (IotDeviceAddressConfig) item.get("addressConfig");
                if (config != null && config.getEnabled() && config.getExtConfig() != null) {
                    try {
                        cn.hutool.json.JSONObject extConfig = cn.hutool.json.JSONUtil.parseObj(config.getExtConfig());
                        Integer registerAddress = extConfig.getInt("registerAddress");
                        String functionCode = extConfig.getStr("functionCode");
                        
                        if (registerAddress != null && cn.hutool.core.util.StrUtil.isNotBlank(functionCode)) {
                            functionCodeGroups
                                .computeIfAbsent(functionCode, k -> new ArrayList<>())
                                .add(registerAddress);
                        }
                    } catch (Exception e) {
                        log.warn("解析地址配置失败: {}", config.getExtConfig());
                    }
                }
            }
            
            // 如果没有配置，使用默认方案（读取16个保持寄存器）
            if (functionCodeGroups.isEmpty()) {
                modbusTcpClient.readHoldingRegisters(device, driverRel, 0, 16);
                // 读取成功，清除离线重试计数
                offlineRetryCount.remove(device.getId());
                return;
            }
            
            // 按功能码分组读取
            for (Map.Entry<String, List<Integer>> entry : functionCodeGroups.entrySet()) {
                String functionCode = entry.getKey();
                List<Integer> addresses = entry.getValue();
                
                if (addresses.isEmpty()) continue;
                
                // 计算连续地址范围
                Collections.sort(addresses);
                int startAddr = addresses.get(0);
                int endAddr = addresses.get(addresses.size() - 1);
                int quantity = endAddr - startAddr + 1;
                
                // 根据功能码选择读取方法
                switch (functionCode) {
                    case "0x01":
                        modbusTcpClient.readCoils(device, driverRel, startAddr, quantity);
                        break;
                    case "0x02":
                        modbusTcpClient.readDiscreteInputs(device, driverRel, startAddr, quantity);
                        break;
                    case "0x03":
                        modbusTcpClient.readHoldingRegisters(device, driverRel, startAddr, quantity);
                        break;
                    case "0x04":
                        modbusTcpClient.readInputRegisters(device, driverRel, startAddr, quantity);
                        break;
                    default:
                        log.warn("跳过不支持的读取功能码: {} - DeviceKey: {}", functionCode, device.getDeviceKey());
                }
            }
            
            // 读取成功，清除离线重试计数
            offlineRetryCount.remove(device.getId());
            
        } catch (Exception e) {
            // 读取失败，将设备状态更新为OFFLINE（仅在状态变化时更新）
            if ("ONLINE".equals(device.getDeviceStatus())) {
                device.setDeviceStatus("OFFLINE");
                iotDeviceService.updateById(device);
                
                // 推送SSE消息到前端
                deviceDataHandler.pushDeviceStatus(device, "OFFLINE");
                
                log.warn("Modbus设备离线 - DeviceKey: {}, IP: {}, 错误: {}", 
                    device.getDeviceKey(), DriverConfigUtil.getIpAddress(driverRel, device), e.getMessage());
            } else {
                // 设备已经是离线状态，只记录DEBUG日志
                log.debug("轮询Modbus设备失败(设备已离线) - DeviceKey: {}", device.getDeviceKey());
            }
        }
    }
}
