# Handler抽象基类使用说明

## 📌 简介

`AbstractProtocolHandler` 是所有协议处理器的抽象基类，提供了通用的连接管理、异常处理、心跳检测等功能。

使用此基类可以：
- ✅ 减少重复代码
- ✅ 统一异常处理逻辑
- ✅ 简化心跳超时处理
- ✅ 标准化日志输出

---

## 🚀 快速开始

### **示例1：简单的协议处理器**

```java
@Slf4j
@Component
@ChannelHandler.Sharable
public class SimpleProtocolHandler extends AbstractProtocolHandler {

    @Override
    protected String getProtocolName() {
        return "SimpleProtocol";
    }

    @Override
    protected void handleData(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 处理业务数据
        String data = (String) msg;
        log.info("收到数据: {}", data);
        
        // 回复客户端
        ctx.writeAndFlush("收到: " + data);
    }
}
```

### **示例2：完整功能的处理器**

```java
@Slf4j
@Component
@ChannelHandler.Sharable
public class AdvancedProtocolHandler extends AbstractProtocolHandler {

    @Resource
    private DeviceService deviceService;

    @Override
    protected String getProtocolName() {
        return "AdvancedProtocol";
    }

    @Override
    protected void handleData(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 解析消息
        ProtocolMessage message = (ProtocolMessage) msg;
        
        // 处理业务逻辑
        processMessage(ctx, message);
    }

    @Override
    protected void onConnectionEstablished(ChannelHandlerContext ctx) {
        // 连接建立时的初始化
        log.info("新设备连接，等待认证...");
    }

    @Override
    protected void onConnectionClosed(ChannelHandlerContext ctx) {
        // 连接关闭时的清理
        String deviceId = getDeviceId(ctx);
        deviceService.updateOfflineStatus(deviceId);
        log.info("设备离线: {}", deviceId);
    }

    @Override
    protected void handleBusinessException(ChannelHandlerContext ctx, Object msg, Exception e) {
        // 业务异常处理
        log.error("业务处理失败，消息: {}", msg, e);
        
        // 返回错误响应
        ErrorResponse error = new ErrorResponse(e.getMessage());
        ctx.writeAndFlush(error);
    }

    @Override
    protected void onHeartbeatTimeout(ChannelHandlerContext ctx) {
        // 心跳超时，设备离线
        String deviceId = getDeviceId(ctx);
        log.warn("设备心跳超时，设备ID: {}", deviceId);
        deviceService.updateOfflineStatus(deviceId);
    }

    private void processMessage(ChannelHandlerContext ctx, ProtocolMessage message) {
        // 具体的业务逻辑
    }

    private String getDeviceId(ChannelHandlerContext ctx) {
        // 从通道获取设备ID
        return ctx.channel().attr(DEVICE_ID_KEY).get();
    }
}
```

---

## 📋 方法说明

### **抽象方法（必须实现）**

| 方法 | 说明 | 返回值 |
|------|------|--------|
| `getProtocolName()` | 返回协议名称，用于日志输出 | String |
| `handleData(ctx, msg)` | 处理业务数据 | void |

### **钩子方法（可选覆盖）**

| 方法 | 触发时机 | 用途 |
|------|---------|------|
| `onConnectionEstablished(ctx)` | 连接建立时 | 初始化、认证 |
| `onConnectionClosed(ctx)` | 连接断开时 | 清理资源、更新状态 |
| `handleBusinessException(ctx, msg, e)` | 业务异常时 | 异常处理、错误响应 |
| `onExceptionOccurred(ctx, cause)` | 发生异常时 | 异常记录、告警 |
| `onHeartbeatTimeout(ctx)` | 心跳超时时 | 设备离线处理 |
| `onWriteTimeout(ctx)` | 写超时时 | 写超时处理 |

---

## 🔄 现有Handler改造建议

### **改造前（MqttServerHandler）**

```java
public class MqttServerHandler extends ChannelInboundHandlerAdapter {
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info(">>> MQTT连接建立 - 远程地址: {}", ctx.channel().remoteAddress());
        // 初始化逻辑
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            MqttMessage mqttMessage = (MqttMessage) msg;
            // 处理MQTT消息
        } catch (Exception e) {
            log.error("处理MQTT消息异常", e);
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error(">>> MQTT发生异常", cause);
        ctx.close();
    }
    
    // ... 其他方法
}
```

### **改造后（推荐）**

```java
public class MqttServerHandler extends AbstractProtocolHandler {
    
    @Override
    protected String getProtocolName() {
        return "MQTT";
    }
    
    @Override
    protected void handleData(ChannelHandlerContext ctx, Object msg) throws Exception {
        MqttMessage mqttMessage = (MqttMessage) msg;
        // 处理MQTT消息（异常会自动捕获）
    }
    
    @Override
    protected void onConnectionEstablished(ChannelHandlerContext ctx) {
        // 初始化逻辑（连接建立日志已由基类输出）
    }
}
```

**优势：**
- ✅ 代码量减少30%+
- ✅ 异常处理自动化
- ✅ 日志输出统一
- ✅ 心跳超时自动处理

---

## ⚠️ 注意事项

1. **@ChannelHandler.Sharable注解**：如果Handler是无状态的，添加此注解可共享实例
2. **异常处理**：`handleData`方法抛出的异常会被基类自动捕获并调用`handleBusinessException`
3. **连接关闭**：发生异常时，基类会自动关闭连接
4. **心跳超时**：需要在Pipeline中添加`IdleStateHandler`才会触发

---

## 📚 相关文件

- **基类**: `vip.xiaonuo.iot.core.handler.AbstractProtocolHandler`
- **使用示例**: 
  - `vip.xiaonuo.iot.core.mqtt.MqttServerHandler` (可改造)
  - `vip.xiaonuo.iot.core.protocol.modbus.ModbusServerHandler` (可改造)

---

*更新时间：2025-12-11*
*维护者：IoT Team*
