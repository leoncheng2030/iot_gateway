# 协议管理模块说明

## 📁 目录结构

```
protocol/
├── ProtocolServer.java          # 协议服务抽象接口
├── ProtocolServerFactory.java   # 协议服务工厂
├── ProtocolManager.java          # 协议管理器（统一管理所有协议实例）
├── impl/                         # 协议实现
│   ├── DynamicMqttServer.java   # MQTT服务器实现
│   ├── DynamicWebSocketServer.java  # WebSocket服务器实现
│   ├── DynamicTcpServer.java    # TCP服务器实现
│   └── DynamicModbusServer.java # Modbus TCP服务器实现（从站模式）
└── modbus/                       # Modbus专用模块
    ├── ModbusTcpClient.java     # Modbus TCP客户端（主站模式）⭐
    ├── ModbusTcpFrame.java      # Modbus帧结构
    ├── ModbusTcpDecoder.java    # Modbus解码器
    ├── ModbusTcpEncoder.java    # Modbus编码器
    ├── ModbusDataParser.java    # Modbus数据解析器
    ├── ModbusDeviceManager.java # Modbus设备管理器
    └── ModbusServerHandler.java # Modbus服务器处理器（从站模式）
```

## 🔍 协议角色说明

### **服务器模式（等待设备连接）**
- **MQTT**: 云平台作为Broker，设备作为Client连接
- **WebSocket**: 云平台作为服务器，设备主动连接
- **TCP**: 云平台作为服务器，设备主动连接  
- **Modbus TCP (从站)**: 云平台作为从站，SCADA/组态软件作为主站读取数据

### **客户端模式（主动连接设备）**
- **Modbus TCP (主站)**: ⭐ **当前使用** - 云平台作为主站，主动连接PLC/DTU设备轮询数据

## 📋 协议配置管理

协议通过`iot_protocol`表进行配置，`ProtocolManager`统一管理启动和停止。

### **配置示例：**

```sql
-- MQTT协议（启用）
INSERT INTO `IOT_PROTOCOL` VALUES ('1', 'MQTT协议', 'MQTT', 1883, '{"qos":1,"keepAlive":60}', 'ENABLE', ...);

-- Modbus TCP协议（已禁用，如需对接SCADA可启用）
-- INSERT INTO `IOT_PROTOCOL` VALUES ('2', 'Modbus TCP协议', 'MODBUS_TCP', 502, '{"readTimeout":60,...}', 'DISABLE', ...);

-- WebSocket协议（默认禁用）
INSERT INTO `IOT_PROTOCOL` VALUES ('3', 'WebSocket协议', 'WEBSOCKET', 8083, '{"path":"/iot/websocket",...}', 'DISABLE', ...);

-- TCP协议（默认禁用）
INSERT INTO `IOT_PROTOCOL` VALUES ('4', 'TCP协议', 'TCP', 8084, '{"maxFrameLength":1024,...}', 'DISABLE', ...);
```

## 🚀 启动流程

1. **系统启动时**: `ProtocolManager.init()` 自动加载状态为`ENABLE`的协议
2. **动态启动**: 通过协议配置管理界面启动/停止协议
3. **Modbus轮询**: 通过定时任务`ModbusPollingTimerTask`定时轮询PLC设备

## ⚙️ 扩展新协议

### **步骤：**

1. **实现ProtocolServer接口**
   ```java
   @Component
   public class DynamicXxxServer implements ProtocolServer {
       @Override
       public void start(Integer port, Map<String, Object> config) {
           // 启动逻辑
       }
       
       @Override
       public void stop() {
           // 停止逻辑
       }
   }
   ```

2. **注册到ProtocolServerFactory**
   ```java
   case "XXX" -> xxxServer;
   ```

3. **添加协议配置**
   ```sql
   INSERT INTO `IOT_PROTOCOL` VALUES (...);
   ```

4. **在协议配置管理中启用**

## 📌 注意事项

1. **Modbus双模式**：
   - `DynamicModbusServer`（从站）- 用于SCADA对接，当前禁用
   - `ModbusTcpClient`（主站）- **当前使用**，主动轮询PLC

2. **端口管理**：`ProtocolManager`会检查端口冲突，避免重复启动

3. **线程池**：每个协议服务器独立管理EventLoopGroup

4. **优雅停止**：停止时会等待任务完成，最长10秒

---

*更新时间：2025-12-11*
*维护者：IoT Team*
