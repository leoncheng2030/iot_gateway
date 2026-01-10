package vip.xiaonuo.iot.core.protocol.s7;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vip.xiaonuo.iot.core.message.DeviceDataHandler;
import vip.xiaonuo.iot.core.message.DeviceMessageService;
import vip.xiaonuo.iot.core.protocol.ProtocolServer;
import vip.xiaonuo.iot.core.protocol.annotation.Protocol;
import vip.xiaonuo.iot.core.protocol.address.AddressConfigProvider;
import vip.xiaonuo.iot.core.protocol.address.AddressConfigTemplate;
import vip.xiaonuo.iot.core.protocol.address.AddressConfigTemplate.ConfigField;
import vip.xiaonuo.iot.core.protocol.address.AddressConfigTemplate.Option;
import vip.xiaonuo.iot.modular.device.entity.IotDevice;
import vip.xiaonuo.iot.modular.device.service.IotDeviceService;
import vip.xiaonuo.iot.modular.devicedriverrel.entity.IotDeviceDriverRel;
import vip.xiaonuo.iot.modular.register.entity.IotDeviceRegisterMapping;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * S7协议服务端实现
 * 用于S7-200/300/400/1200/1500等西门子PLC的数据采集
 *
 * @author xiaonuo
 * &#064;date  2026/01/10
 */
@Slf4j
@Protocol(type = "S7", name = "S7协议", description = "西门子S7系列PLC协议，支持S7-200/300/400/1200/1500")
@Service("s7ProtocolServer")
public class S7ProtocolServer implements ProtocolServer, AddressConfigProvider {

    @Resource
    private S7Client s7Client;

    @Resource
    private DeviceMessageService deviceMessageService;
    
    @Resource
    private IotDeviceService iotDeviceService;
    
    @Resource
    private DeviceDataHandler deviceDataHandler;

    /**
     * 定时采集任务执行器
     */
    private ScheduledExecutorService scheduler;

    /**
     * 设备采集任务映射 - Key: deviceId, Value: ScheduledFuture
     */
    private final Map<String, ScheduledFuture<?>> deviceTasks = new ConcurrentHashMap<>();
    
    /**
     * 设备连续失败次数 - Key: deviceId, Value: failCount
     */
    private final Map<String, Integer> deviceFailCounts = new ConcurrentHashMap<>();
    
    /**
     * 连续失败多少次后判定为离线
     */
    private static final int MAX_FAIL_COUNT = 3;
    
    /**
     * 端口号（S7协议主动连接，不监听端口）
     */
    private Integer port = null;

    @Override
    public void start(Integer port, Map<String, Object> config) {
        this.port = port; // S7主动连接，不需要监听端口
        start();
    }
    
    public void start() {
        log.info("S7协议服务启动");
        scheduler = Executors.newScheduledThreadPool(10, r -> {
            Thread thread = new Thread(r);
            thread.setName("S7-Collector-" + thread.getId());
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public void stop() {
        log.info("S7协议服务停止");
        // 停止所有采集任务
        deviceTasks.forEach((deviceId, future) -> future.cancel(true));
        deviceTasks.clear();

        // 关闭线程池
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // 关闭所有S7连接
        s7Client.closeAll();
    }

    /**
     * 添加S7设备
     *
     * @param device           设备信息
     * @param driverRel        驱动关联关系
     * @param registerMappings 寄存器映射列表
     */
    public void addDevice(IotDevice device, IotDeviceDriverRel driverRel, 
                         List<IotDeviceRegisterMapping> registerMappings) {
        try {
            String deviceId = device.getId();
            
            // 解析驱动配置
            JSONObject config = parseConfig(driverRel.getDeviceConfig());
            if (config == null) {
                log.error("S7驱动配置解析失败 - DeviceId: {}", deviceId);
                return;
            }

            // 连接到PLC
            if (!s7Client.connect(deviceId, config)) {
                log.error("S7设备连接失败 - DeviceId: {}", deviceId);
                return;
            }

            // 连接成功后，更新设备状态为在线
            log.info("准备更新设备状态 - DeviceId: {}, 当前状态: {}", deviceId, device.getDeviceStatus());
            
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
                    log.info("S7设备重连成功 - DeviceKey: {}", device.getDeviceKey());
                } else {
                    log.info("S7设备首次连接成功 - DeviceKey: {}", device.getDeviceKey());
                }
            } else {
                log.info("S7设备状态无需更新 - DeviceKey: {}, 当前状态: {}", device.getDeviceKey(), device.getDeviceStatus());
            }

            // 获取采集间隔（默认5秒）
            int interval = config.getInt("interval", 5000);

            // 启动定时采集任务
            ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                    () -> collectData(device, registerMappings),
                    0, // 初始延迟
                    interval, // 采集间隔
                    TimeUnit.MILLISECONDS
            );

            deviceTasks.put(deviceId, future);
            log.info("S7设备添加成功 - DeviceId: {}, 采集间隔: {}ms", deviceId, interval);

        } catch (Exception e) {
            log.error("S7设备添加异常 - DeviceId: {}", device.getId(), e);
        }
    }

    /**
     * 移除S7设备
     *
     * @param deviceId 设备ID
     */
    public void removeDevice(String deviceId) {
        try {
            // 停止采集任务
            ScheduledFuture<?> future = deviceTasks.remove(deviceId);
            if (future != null) {
                future.cancel(true);
            }
            
            // 清理失败计数
            deviceFailCounts.remove(deviceId);

            // 断开连接
            s7Client.disconnect(deviceId);

            log.info("S7设备移除成功 - DeviceId: {}", deviceId);
        } catch (Exception e) {
            log.error("S7设备移除异常 - DeviceId: {}", deviceId, e);
        }
    }

    /**
     * 采集设备数据
     *
     * @param device           设备
     * @param registerMappings 寄存器映射列表
     */
    private void collectData(IotDevice device, List<IotDeviceRegisterMapping> registerMappings) {
        String deviceId = device.getId();
        try {
            JSONObject data = JSONUtil.createObj();

            // 遍历寄存器映射，读取数据
            for (IotDeviceRegisterMapping mapping : registerMappings) {
                try {
                    Object value = readRegister(deviceId, mapping);
                    if (value != null) {
                        data.set(mapping.getIdentifier(), value);
                    }
                } catch (Exception e) {
                    log.debug("S7读取寄存器失败 - DeviceId: {}, Property: {}", 
                            deviceId, mapping.getIdentifier());
                    // 读取失败，抛出异常让上层处理
                    throw e;
                }
            }

            // 发送数据到消息服务
            if (!data.isEmpty()) {
                // 构建标准Topic格式: /{productId}/{deviceKey}/property/post
                String topic = String.format("/%s/%s/property/post", 
                        device.getProductId(), device.getDeviceKey());
                deviceMessageService.handleDeviceMessage(topic, data.toString());
            }
            
            // 采集成功，重置失败计数
            deviceFailCounts.put(deviceId, 0);
            
            // 如果设备之前是离线，更新为在线
            if ("OFFLINE".equals(device.getDeviceStatus())) {
                device.setDeviceStatus("ONLINE");
                device.setLastOnlineTime(new java.util.Date());
                iotDeviceService.updateById(device);
                deviceDataHandler.pushDeviceStatus(device, "ONLINE");
                log.info("S7设备重连成功 - DeviceKey: {}", device.getDeviceKey());
            }

        } catch (Exception e) {
            // 采集失败，增加失败计数
            int failCount = deviceFailCounts.getOrDefault(deviceId, 0) + 1;
            deviceFailCounts.put(deviceId, failCount);
            
            log.debug("S7采集数据失败 - DeviceId: {}, 连续失败: {}/{}次", 
                    deviceId, failCount, MAX_FAIL_COUNT);
            
            // 连续失败达到阈值，判定为离线
            if (failCount >= MAX_FAIL_COUNT && "ONLINE".equals(device.getDeviceStatus())) {
                device.setDeviceStatus("OFFLINE");
                iotDeviceService.updateById(device);
                deviceDataHandler.pushDeviceStatus(device, "OFFLINE");
                log.warn("S7设备离线 - DeviceKey: {}, 连续失败{}次", device.getDeviceKey(), failCount);
            }
        }
    }

    /**
     * 读取寄存器数据
     *
     * @param deviceId 设备ID
     * @param mapping  寄存器映射
     * @return 读取的值
     */
    private Object readRegister(String deviceId, IotDeviceRegisterMapping mapping) {
        Integer address = mapping.getRegisterAddress();
        if (address == null) {
            return null;
        }

        // 解析地址格式（例如：DB1.DBW100, MW10, M0.0）
        String addressStr = mapping.getIdentifier();
        
        // DB块：DBx.DBWy, DBx.DBDy, DBx.DBBy, DBx.DBXy.z
        if (addressStr.startsWith("DB")) {
            return readDBArea(deviceId, addressStr, mapping.getDataType());
        }
        // M区：MWx, MDx, MBx, Mx.y
        else if (addressStr.startsWith("M")) {
            return readMArea(deviceId, address, mapping.getDataType());
        }
        // V区：VWx, VDx, VBx, Vx.y (S7-200)
        else if (addressStr.startsWith("V")) {
            return readVArea(deviceId, address, mapping.getDataType());
        }

        return null;
    }

    /**
     * 读取DB区数据
     */
    private Object readDBArea(String deviceId, String address, String valueType) {
        // 解析地址：DB1.DBW100
        String[] parts = address.split("\\.");
        if (parts.length < 2) {
            return null;
        }

        int dbNumber = Integer.parseInt(parts[0].substring(2));
        String dataType = parts[1].substring(0, 3); // DBW, DBD, DBB, DBX
        int offset = Integer.parseInt(parts[1].substring(3));

        int size = getSizeByType(valueType);
        byte[] buffer = s7Client.readDB(deviceId, dbNumber, offset, size);
        
        return convertValue(buffer, 0, valueType);
    }

    /**
     * 读取M区数据
     */
    private Object readMArea(String deviceId, int address, String valueType) {
        int size = getSizeByType(valueType);
        byte[] buffer = s7Client.readMerker(deviceId, address, size);
        return convertValue(buffer, 0, valueType);
    }

    /**
     * 读取V区数据（S7-200）
     */
    private Object readVArea(String deviceId, int address, String valueType) {
        // V区实际上是DB1
        int size = getSizeByType(valueType);
        byte[] buffer = s7Client.readDB(deviceId, 1, address, size);
        return convertValue(buffer, 0, valueType);
    }

    /**
     * 根据数据类型转换值
     */
    private Object convertValue(byte[] buffer, int offset, String valueType) {
        if (buffer == null) {
            return null;
        }

        switch (valueType.toLowerCase()) {
            case "bool":
                return S7DataUtil.getBoolean(buffer, offset, 0);
            case "int":
                return S7DataUtil.getInt(buffer, offset);
            case "dint":
                return S7DataUtil.getDInt(buffer, offset);
            case "float":
            case "real":
                return S7DataUtil.getReal(buffer, offset);
            case "double":
            case "lreal":
                return S7DataUtil.getLReal(buffer, offset);
            case "byte":
                return S7DataUtil.getByte(buffer, offset);
            case "word":
                return S7DataUtil.getWord(buffer, offset);
            default:
                return null;
        }
    }

    /**
     * 根据数据类型获取字节大小
     */
    private int getSizeByType(String valueType) {
        switch (valueType.toLowerCase()) {
            case "bool":
            case "byte":
                return 1;
            case "int":
            case "word":
                return 2;
            case "dint":
            case "float":
            case "real":
                return 4;
            case "double":
            case "lreal":
                return 8;
            default:
                return 2;
        }
    }

    /**
     * 解析驱动配置
     */
    private JSONObject parseConfig(String configStr) {
        try {
            if (StrUtil.isBlank(configStr)) {
                return null;
            }
            return JSONUtil.parseObj(configStr);
        } catch (Exception e) {
            log.error("解析驱动配置失败", e);
            return null;
        }
    }

    @PreDestroy
    public void destroy() {
        stop();
    }
    
    @Override
    public Integer getPort() {
        return port;
    }
    
    @Override
    public String getProtocolType() {
        return "S7";
    }
    
    /**
     * 获取S7客户端
     */
    public S7Client getS7Client() {
        return s7Client;
    }

    /**
     * 实现 AddressConfigProvider 接口
     * 提供S7协议的地址配置模板
     */
    @Override
    public AddressConfigTemplate getAddressConfigTemplate() {
        AddressConfigTemplate template = new AddressConfigTemplate();
        template.setProtocolType("S7");
        template.setTemplateName("S7 PLC 地址配置");
        template.setInputMode(AddressConfigTemplate.AddressInputMode.BUILDER);
        template.setFormatDescription("西门子 S7 PLC 地址格式：<区域><数据类型><偏移量>，如 DB1.DBW100、MW10、VW100");
        template.setExamples(Arrays.asList(
            "DB1.DBW100 - DB1块的第100字节，读取1个Word(2字节)",
            "DB1.DBD200 - DB1块的第200字节，读取1个DWord(4字节)",
            "MW10 - 标记区第10字节，读取1个Word",
            "VW100 - V区第100字节(S7-200)",
            "IB5 - 输入区第5字节",
            "QB10 - 输出区第10字节"
        ));
        template.setValidationRegex("^(DB\\d+\\.DB[BWDX]\\d+(?:\\.\\d+)?|[MVIQ][BWDX]\\d+(?:\\.\\d+)?)$");

        // 配置字段列表
        ConfigField areaField = new ConfigField();
        areaField.setName("area");
        areaField.setLabel("存储区域");
        areaField.setType(AddressConfigTemplate.FieldType.SELECT);
        areaField.setDescription("选择PLC存储区域");
        areaField.setRequired(true);
        areaField.setDefaultValue("DB");
        areaField.setOptions(Arrays.asList(
            createOption("DB块（数据块）", "DB", "用户数据块，最常用"),
            createOption("M区（标记区）", "M", "标记寄存器，全局变量"),
            createOption("V区（变量区）", "V", "S7-200特有区域"),
            createOption("I区（输入区）", "I", "输入过程映像"),
            createOption("Q区（输出区）", "Q", "输出过程映像")
        ));

        ConfigField dbNumberField = new ConfigField();
        dbNumberField.setName("dbNumber");
        dbNumberField.setLabel("DB块号");
        dbNumberField.setType(AddressConfigTemplate.FieldType.NUMBER);
        dbNumberField.setDescription("数据块编号（1-65535）");
        dbNumberField.setRequired(true);
        dbNumberField.setDefaultValue(1);
        dbNumberField.setMin(1);
        dbNumberField.setMax(65535);
        dbNumberField.setShowWhen("area=DB"); // 仅DB区显示

        ConfigField dataTypeField = new ConfigField();
        dataTypeField.setName("dataTypePrefix");
        dataTypeField.setLabel("数据类型");
        dataTypeField.setType(AddressConfigTemplate.FieldType.SELECT);
        dataTypeField.setDescription("选择读取的数据类型");
        dataTypeField.setRequired(true);
        dataTypeField.setDefaultValue("W");
        dataTypeField.setOptions(Arrays.asList(
            createOption("Byte（字节，1字节）", "B", "读取1个字节"),
            createOption("Word（字，2字节）", "W", "读取2个字节"),
            createOption("DWord（双字，4字节）", "D", "读取4个字节"),
            createOption("Bit（位）", "X", "读取单个位")
        ));

        ConfigField offsetField = new ConfigField();
        offsetField.setName("offset");
        offsetField.setLabel("字节偏移量");
        offsetField.setType(AddressConfigTemplate.FieldType.NUMBER);
        offsetField.setDescription("从起始地址开始的字节偏移");
        offsetField.setRequired(true);
        offsetField.setDefaultValue(0);
        offsetField.setMin(0);
        offsetField.setMax(65535);

        ConfigField bitIndexField = new ConfigField();
        bitIndexField.setName("bitIndex");
        bitIndexField.setLabel("位索引");
        bitIndexField.setType(AddressConfigTemplate.FieldType.NUMBER);
        bitIndexField.setDescription("指定读取的位(0-7)");
        bitIndexField.setRequired(false);
        bitIndexField.setDefaultValue(0);
        bitIndexField.setMin(0);
        bitIndexField.setMax(7);
        bitIndexField.setShowWhen("dataTypePrefix=X"); // 仅Bit类型显示

        template.setFields(Arrays.asList(areaField, dbNumberField, dataTypeField, offsetField, bitIndexField));

        return template;
    }

    /**
     * 创建选项对象
     */
    private Option createOption(String label, Object value, String description) {
        Option option = new Option();
        option.setLabel(label);
        option.setValue(value);
        option.setDescription(description);
        return option;
    }
}
