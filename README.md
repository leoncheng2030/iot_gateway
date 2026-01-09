<div align="center">
    <h1>IoT Gateway Platform</h1>
    <p>智能物联网网关管理平台</p>
</div>

## 平台简介

IoT Gateway Platform 是一款功能强大的智能物联网网关管理平台，致力于为企业提供统一的设备接入、数据采集、协议转换和设备管理解决方案。

### 核心优势

- **多协议支持** - 无缝对接 Modbus、MQTT、OPC UA 等主流工业协议，实现异构设备统一接入
- **可视化配置** - 直观的流程编排界面，无需编程即可完成设备配置和数据采集规则
- **实时监控** - 设备状态实时展示，数据采集日志全程可追溯，故障快速定位
- **灵活扩展** - 插件化架构设计，支持自定义协议和业务逻辑扩展
- **安全可靠** - 集成国密加密算法，符合等保要求，保障数据传输安全

### 适用场景

- 智能制造：生产设备数据采集与监控
- 智慧园区：能源管理与环境监测
- 智慧农业：农业传感器数据汇聚
- 智能楼宇：楼宇自控系统集成

---

## 致谢

本项目基于 [Snowy](https://gitee.com/xiaonuobase/snowy) 开源框架构建，在此特别感谢 Snowy 团队提供的优秀基础框架和完善的开发工具。Snowy 是国内首个国密前后端分离快速开发平台，其简洁的代码架构、丰富的功能模块和专业的技术支持为本项目的快速开发奠定了坚实基础。

**Snowy 框架相关资源：**
- Gitee 地址：[https://gitee.com/xiaonuobase/snowy](https://gitee.com/xiaonuobase/snowy)
- 官方文档：[https://xiaonuo.vip/doc](https://xiaonuo.vip/doc)
- 演示地址：[https://snowy.xiaonuo.vip](https://snowy.xiaonuo.vip)

## 快速启动

### 环境准备

| 组件 | 版本要求 | 用途 |
|------|----------|------|
| Node.js | ≥ 18 | 前端运行环境 |
| JDK | 17 | 后端运行环境 |
| Maven | 最新版 | 依赖管理工具 |
| Redis | 最新版 | 缓存服务 |
| MySQL | 8.0 / 5.7 | 数据存储 |

### 前端启动

进入前端目录：
```bash
cd snowy-admin-web
```

安装依赖：
```bash
npm install
```

启动开发服务：
```bash
npm run dev
```

### 后端启动

1. 配置数据库连接信息
2. 配置 Redis 连接信息
3. 在开发工具中启动主模块 `snowy-web-app`

## 项目结构

本项目采用插件化架构，实现模块分离和高度解耦：

```
iot_gateway
  ├─ snowy-admin-web          # 前端管理平台
  │   ├─ src
  │   │   ├─ api               # API 接口定义
  │   │   ├─ components        # 公共组件
  │   │   ├─ views             # 页面视图
  │   │   ├─ router            # 路由配置
  │   │   └─ utils             # 工具类
  ├─ snowy-common             # 基础公共模块
  ├─ snowy-plugin             # 功能插件模块
  │   ├─ snowy-plugin-auth    # 认证鉴权
  │   ├─ snowy-plugin-biz     # 业务功能
  │   ├─ snowy-plugin-dev     # 开发工具
  │   ├─ snowy-plugin-iot     # IoT 设备管理
  │   ├─ snowy-plugin-sys     # 系统管理
  │   └─ snowy-plugin-mobile  # 移动端管理
  ├─ snowy-plugin-api         # 插件 API 接口
  └─ snowy-web-app            # 主启动模块
```

## 功能特性

### 设备管理
- 支持多种设备类型的接入和管理
- 设备批量导入和配置
- 设备在线状态实时监控
- 设备报警和故障提醒

### 数据采集
- 可视化流程编排，配置数据采集规则
- 多协议支持：Modbus RTU/TCP、MQTT、OPC UA
- 数据预处理和格式转换
- 采集任务调度管理

### 数据处理
- 数据实时解析和验证
- 数据缓存和断线重传
- 数据过滤和聚合
- 支持数据上报到云端平台

### 运维管理
- 采集日志查询和追溯
- 系统运行状态监控
- 配置备份与恢复
- 用户权限管理

## 技术架构

### 后端技术栈
- Spring Boot 3.x - 应用框架
- MyBatis Plus - ORM 框架
- Redis - 缓存中间件
- MySQL - 关系型数据库
- SM2/SM3/SM4 - 国密加密算法

### 前端技术栈
- Vue 3 - 渐进式框架
- Ant Design Vue 4 - UI 组件库
- Vite 5 - 构建工具
- Pinia - 状态管理
- Axios - HTTP 客户端

## 版本说明

当前版本基于 Snowy 3.0 框架开发，持续迭代中...

---

## 开源协议

本项目采用 Apache License 2.0 开源协议，免费供个人学习和企业使用。
