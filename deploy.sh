#!/bin/bash

# WQS IoT Gateway Docker 部署脚本

set -e

echo "======================================"
echo "  WQS IoT Gateway 一键部署脚本"
echo "======================================"
echo ""

# 检查 Docker 是否安装
if ! command -v docker &> /dev/null; then
    echo "错误: Docker 未安装，请先安装 Docker"
    echo "安装命令: curl -fsSL https://get.docker.com | sh"
    exit 1
fi

# 检查 Docker Compose 是否安装
if ! command -v docker-compose &> /dev/null; then
    echo "错误: Docker Compose 未安装，请先安装 Docker Compose"
    exit 1
fi

echo "检查环境配置文件..."
if [ ! -f .env ]; then
    echo "未找到 .env 文件，从模板创建..."
    cp .env.example .env
    echo ""
    echo "警告: 请编辑 .env 文件，修改数据库密码等敏感信息！"
    echo "编辑完成后，再次运行此脚本。"
    echo ""
    echo "使用命令编辑: vim .env 或 nano .env"
    exit 0
fi

echo ""
echo "开始部署服务..."
echo ""

# 停止旧的容器
echo "停止旧的容器..."
docker-compose down

# 构建并启动服务
echo ""
echo "构建镜像并启动服务（首次启动需要 5-10 分钟）..."
docker-compose up -d --build

# 等待服务启动
echo ""
echo "等待服务启动..."
sleep 10

# 显示服务状态
echo ""
echo "======================================"
echo "  服务状态"
echo "======================================"
docker-compose ps

echo ""
echo "======================================"
echo "  部署完成"
echo "======================================"
echo ""
echo "前端地址: http://localhost"
echo "后端API文档: http://localhost:82/doc.html"
echo "InfluxDB控制台: http://localhost:8086"
echo ""
echo "默认账号: superAdmin"
echo "默认密码: 123456"
echo ""
echo "查看日志: docker-compose logs -f"
echo "停止服务: docker-compose stop"
echo "重启服务: docker-compose restart"
echo ""
