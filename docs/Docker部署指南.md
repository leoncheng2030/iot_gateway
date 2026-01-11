# WQS IoT Gateway Docker 部署指南

## 简介

本指南介绍如何使用 Docker 和 Docker Compose 快速部署 WQS IoT Gateway 物联网网关管理平台。

**Docker 部署的优势**
- 一键启动所有服务（前端、后端、数据库、缓存）
- 环境隔离，避免依赖冲突
- 快速部署，5-10 分钟即可完成
- 易于迁移和扩展
- 生产级别的容器编排

## 架构说明

Docker Compose 将启动以下容器：

| 容器名称 | 服务 | 端口 | 说明 |
|---------|------|------|------|
| iot-gateway-frontend | Nginx + Vue | 80 | 前端应用 |
| iot-gateway-backend | Spring Boot | 82 | 后端应用 |
| iot-gateway-mysql | MySQL 8.0 | 3306 | 数据存储 |
| iot-gateway-redis | Redis 7 | 6379 | 缓存服务 |
| iot-gateway-influxdb | InfluxDB 2.7 | 8086 | 时序数据库 |

**网络架构**
- 所有容器在同一个 Docker 网络中互联
- 前端通过 Nginx 反向代理访问后端
- 后端通过容器名称访问数据库和缓存

## 快速开始

### 1. 环境准备

**Linux/macOS**
```bash
# 安装 Docker
curl -fsSL https://get.docker.com | sh

# 安装 Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 启动 Docker 服务
sudo systemctl start docker
sudo systemctl enable docker

# 将当前用户加入 docker 组（可选）
sudo usermod -aG docker $USER
```

**Windows**
1. 下载并安装 [Docker Desktop for Windows](https://www.docker.com/products/docker-desktop/)
2. 启动 Docker Desktop
3. 确保 WSL 2 已启用（推荐）

### 2. 克隆代码

```bash
git clone <repository-url>
cd iot_gateway
```

### 3. 配置环境变量

```bash
# 复制环境变量模板
cp .env.example .env

# 编辑配置文件
vim .env  # Linux/macOS
notepad .env  # Windows
```

**必须修改的配置**

```properties
# MySQL 数据库配置（请使用强密码）
MYSQL_ROOT_PASSWORD=your_secure_root_password
MYSQL_PASSWORD=your_secure_database_password

# InfluxDB 配置
INFLUXDB_PASSWORD=your_secure_influxdb_password
INFLUXDB_TOKEN=your_secure_token_at_least_32_characters

# Redis 密码（可选）
REDIS_PASSWORD=
```

### 4. 一键启动

**Linux/macOS**
```bash
# 赋予执行权限
chmod +x deploy.sh

# 运行部署脚本
./deploy.sh
```

**Windows (PowerShell)**
```powershell
# 运行部署脚本
.\deploy.ps1
```

**手动启动**
```bash
# 构建并启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

### 5. 访问应用

启动成功后，通过浏览器访问：

- **前端管理界面**: http://localhost
- **后端API文档**: http://localhost:82/doc.html
- **InfluxDB控制台**: http://localhost:8086

**默认登录账号**
- 用户名: `superAdmin`
- 密码: `123456`

首次登录后请立即修改密码。

## 服务管理

### 启动服务

```bash
# 启动所有服务
docker-compose start

# 启动指定服务
docker-compose start backend
docker-compose start frontend
```

### 停止服务

```bash
# 停止所有服务
docker-compose stop

# 停止指定服务
docker-compose stop backend
```

### 重启服务

```bash
# 重启所有服务
docker-compose restart

# 重启指定服务
docker-compose restart backend
```

### 查看日志

```bash
# 查看所有服务日志（实时）
docker-compose logs -f

# 查看指定服务日志
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f mysql

# 查看最近 100 行日志
docker-compose logs --tail=100 backend

# 查看特定时间的日志
docker-compose logs --since="2024-01-01T00:00:00" backend
```

### 查看服务状态

```bash
# 查看所有服务状态
docker-compose ps

# 查看资源占用
docker stats
```

### 进入容器调试

```bash
# 进入后端容器
docker exec -it iot-gateway-backend sh

# 进入数据库容器
docker exec -it iot-gateway-mysql bash

# 进入前端容器
docker exec -it iot-gateway-frontend sh
```

### 清理和重置

```bash
# 停止并删除容器（保留数据）
docker-compose down

# 停止并删除容器和数据卷（谨慎操作！）
docker-compose down -v

# 清理未使用的镜像
docker image prune -a

# 清理未使用的卷
docker volume prune
```

## 数据管理

### 数据持久化

数据通过 Docker Volume 持久化存储：

```bash
# 查看所有卷
docker volume ls | grep iot-gateway

# 数据卷列表
# - iot_gateway_mysql-data: MySQL 数据
# - iot_gateway_redis-data: Redis 数据
# - iot_gateway_influxdb-data: InfluxDB 数据
# - iot_gateway_influxdb-config: InfluxDB 配置
```

### 数据备份

**MySQL 数据库备份**
```bash
# 备份数据库
docker exec iot-gateway-mysql mysqldump -uroot -p${MYSQL_ROOT_PASSWORD} iot_gateway > backup_$(date +%Y%m%d_%H%M%S).sql

# 备份到容器内
docker exec iot-gateway-mysql sh -c 'mysqldump -uroot -p${MYSQL_ROOT_PASSWORD} iot_gateway > /tmp/backup.sql'
docker cp iot-gateway-mysql:/tmp/backup.sql ./backup.sql
```

**备份所有数据卷**
```bash
# 创建备份目录
mkdir -p backups

# 备份 MySQL 数据卷
docker run --rm -v iot_gateway_mysql-data:/data -v $(pwd)/backups:/backup alpine tar czf /backup/mysql-data-$(date +%Y%m%d).tar.gz -C /data .

# 备份 Redis 数据卷
docker run --rm -v iot_gateway_redis-data:/data -v $(pwd)/backups:/backup alpine tar czf /backup/redis-data-$(date +%Y%m%d).tar.gz -C /data .

# 备份 InfluxDB 数据卷
docker run --rm -v iot_gateway_influxdb-data:/data -v $(pwd)/backups:/backup alpine tar czf /backup/influxdb-data-$(date +%Y%m%d).tar.gz -C /data .
```

### 数据恢复

**MySQL 数据库恢复**
```bash
# 从 SQL 文件恢复
docker exec -i iot-gateway-mysql mysql -uroot -p${MYSQL_ROOT_PASSWORD} iot_gateway < backup.sql

# 或者
cat backup.sql | docker exec -i iot-gateway-mysql mysql -uroot -p${MYSQL_ROOT_PASSWORD} iot_gateway
```

**恢复数据卷**
```bash
# 恢复 MySQL 数据卷
docker run --rm -v iot_gateway_mysql-data:/data -v $(pwd)/backups:/backup alpine sh -c "cd /data && tar xzf /backup/mysql-data-20240101.tar.gz"
```

## 更新与升级

### 更新应用

```bash
# 1. 备份数据
./backup.sh  # 或手动备份

# 2. 拉取最新代码
git pull origin main

# 3. 重新构建镜像
docker-compose build

# 4. 停止旧容器
docker-compose down

# 5. 启动新容器
docker-compose up -d

# 6. 查看日志确认启动成功
docker-compose logs -f backend
```

### 更新单个服务

```bash
# 只更新后端服务
docker-compose build backend
docker-compose up -d --no-deps backend

# 只更新前端服务
docker-compose build frontend
docker-compose up -d --no-deps frontend
```

## 性能优化

### 资源限制

编辑 `docker-compose.yml`，为每个服务添加资源限制：

```yaml
services:
  backend:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 4G
        reservations:
          cpus: '1'
          memory: 2G
```

### JVM 调优

在 `.env` 文件中调整 Java 堆内存：

```properties
# 根据服务器内存调整
JAVA_OPTS=-Xms1g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

### MySQL 优化

在 `docker-compose.yml` 中调整 MySQL 配置：

```yaml
mysql:
  command:
    - --max_connections=2000
    - --innodb_buffer_pool_size=2G
    - --query_cache_size=128M
```

### Redis 优化

```yaml
redis:
  command: >
    redis-server
    --requirepass ${REDIS_PASSWORD:-}
    --appendonly yes
    --maxmemory 2gb
    --maxmemory-policy allkeys-lru
```

## 监控和日志

### 日志收集

配置日志驱动将日志发送到外部系统：

```yaml
services:
  backend:
    logging:
      driver: "json-file"
      options:
        max-size: "100m"
        max-file: "10"
```

### 健康检查

所有服务都配置了健康检查：

```bash
# 查看服务健康状态
docker inspect --format='{{.State.Health.Status}}' iot-gateway-backend
docker inspect --format='{{.State.Health.Status}}' iot-gateway-mysql
```

### Prometheus 监控（可选）

添加 Prometheus 和 Grafana 服务：

```yaml
services:
  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus-data:/prometheus
    networks:
      - iot-network

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    volumes:
      - grafana-data:/var/lib/grafana
    networks:
      - iot-network
```

## 生产环境部署

### 安全配置

1. **修改所有默认密码**
   ```bash
   # 在 .env 文件中设置强密码
   MYSQL_ROOT_PASSWORD=$(openssl rand -base64 32)
   MYSQL_PASSWORD=$(openssl rand -base64 32)
   INFLUXDB_TOKEN=$(openssl rand -base64 32)
   ```

2. **启用 HTTPS**
   
   使用 Nginx 反向代理或 Traefik 配置 SSL 证书：
   ```yaml
   services:
     nginx:
       image: nginx:alpine
       ports:
         - "443:443"
       volumes:
         - ./nginx.conf:/etc/nginx/nginx.conf
         - ./certs:/etc/nginx/certs
   ```

3. **配置防火墙**
   ```bash
   # 只开放必要的端口
   sudo ufw allow 80/tcp
   sudo ufw allow 443/tcp
   sudo ufw enable
   ```

4. **定期备份**
   
   设置 cron 定时任务：
   ```bash
   # 编辑 crontab
   crontab -e
   
   # 每天凌晨 2 点备份
   0 2 * * * cd /path/to/iot_gateway && ./backup.sh
   ```

### 高可用部署

使用 Docker Swarm 或 Kubernetes 部署高可用集群：

```bash
# Docker Swarm 初始化
docker swarm init

# 部署服务栈
docker stack deploy -c docker-compose.yml iot-gateway
```

## 故障排查

### 常见问题

**1. 容器无法启动**
```bash
# 查看容器日志
docker-compose logs backend

# 检查端口占用
netstat -tulpn | grep 82
lsof -i:82  # macOS
```

**2. 数据库连接失败**
```bash
# 检查 MySQL 容器状态
docker-compose ps mysql

# 测试数据库连接
docker exec -it iot-gateway-mysql mysql -uroot -p
```

**3. 前端无法访问后端**
```bash
# 检查网络配置
docker network inspect iot_gateway_iot-network

# 测试容器间连接
docker exec iot-gateway-frontend ping backend
```

**4. 内存不足**
```bash
# 查看容器资源占用
docker stats

# 增加 Docker 内存限制（Docker Desktop）
# Settings -> Resources -> Memory
```

### 日志分析

```bash
# 查看错误日志
docker-compose logs backend | grep ERROR

# 查看数据库慢查询
docker exec iot-gateway-mysql mysql -uroot -p -e "SHOW VARIABLES LIKE 'slow_query_log%';"

# 查看 Redis 日志
docker-compose logs redis
```

### 性能分析

```bash
# 查看容器资源使用
docker stats --no-stream

# 查看磁盘使用
docker system df

# 清理无用资源
docker system prune -a
```

## 附录

### 完整的 docker-compose.yml 配置

参考项目根目录的 `docker-compose.yml` 文件。

### 环境变量说明

参考 `.env.example` 文件中的注释。

### 端口映射表

| 服务 | 容器端口 | 宿主机端口 | 说明 |
|-----|---------|-----------|------|
| 前端 | 80 | 80 | HTTP 访问 |
| 后端 | 82 | 82 | API 服务 |
| MySQL | 3306 | 3306 | 数据库 |
| Redis | 6379 | 6379 | 缓存 |
| InfluxDB | 8086 | 8086 | 时序数据库 |
| MQTT | 1883 | 1883 | MQTT 协议 |
| MQTT SSL | 8883 | 8883 | MQTT SSL |
| MQTT WS | 8083 | 8083 | MQTT WebSocket |
| WebSocket | 8084 | 8084 | WebSocket |

---

如有问题，请查看项目 [FAQ](FAQ.md) 或提交 Issue。
