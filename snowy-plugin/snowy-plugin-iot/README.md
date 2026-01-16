# snowy-plugin-iot

物联网功能插件，提供设备管理、协议接入、规则引擎等功能。

## 功能模块

- 产品管理：定义设备产品模型
- 设备管理：设备注册、状态监控、远程控制
- 协议管理：MQTT、WebSocket、TCP-IP协议接入
- 规则引擎：设备联动、场景自动化
- 子设备管理：网关设备及其子设备管理

## JVM 参数配置

### 基础配置（推荐）

```bash
java -Xms512m -Xmx2g -jar snowy-web-app.jar
```

### Chronicle 产品参数（可选）

如果启用 Chronicle Queue/Map 高性能存储，Java 11+ 需要添加以下参数：

```bash
java -Xms512m -Xmx2g \
  --add-exports=java.base/jdk.internal.ref=ALL-UNNAMED \
  --add-exports=java.base/sun.nio.ch=ALL-UNNAMED \
  --add-exports=jdk.unsupported/sun.misc=ALL-UNNAMED \
  --add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED \
  --add-opens=jdk.compiler/com.sun.tools.javac=ALL-UNNAMED \
  --add-opens=java.base/java.lang=ALL-UNNAMED \
  --add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
  --add-opens=java.base/java.io=ALL-UNNAMED \
  --add-opens=java.base/java.util=ALL-UNNAMED \
  -jar snowy-web-app.jar
```

### Docker 部署

在 `docker-compose.yml` 中配置：

```yaml
services:
  iot-gateway:
    environment:
      - JAVA_OPTS=-Xms512m -Xmx2g
```

### 配置项说明

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `iot.storage.enabled` | true | 是否启用时序数据存储 |
| `iot.storage.data-dir` | data | 数据存储目录 |
| `iot.storage.hot-data-entries` | 100000 | 热数据最大条目数 |
| `iot.storage.hot-threshold-hours` | 24 | 热数据阈值（小时） |
| `iot.storage.warm-threshold-days` | 7 | 温数据阈值（天） |
| `iot.mq.data-dir` | data/queue | 消息队列数据目录 |
