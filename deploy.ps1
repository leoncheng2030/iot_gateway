# WQS IoT Gateway Docker 部署脚本 (Windows)

Write-Host "======================================" -ForegroundColor Green
Write-Host "  WQS IoT Gateway 一键部署脚本" -ForegroundColor Green
Write-Host "======================================" -ForegroundColor Green
Write-Host ""

# 检查 Docker 是否安装
try {
    docker --version | Out-Null
} catch {
    Write-Host "错误: Docker 未安装，请先安装 Docker Desktop for Windows" -ForegroundColor Red
    Write-Host "下载地址: https://www.docker.com/products/docker-desktop/" -ForegroundColor Yellow
    exit 1
}

# 检查 Docker Compose 是否安装
try {
    docker-compose --version | Out-Null
} catch {
    Write-Host "错误: Docker Compose 未安装" -ForegroundColor Red
    exit 1
}

Write-Host "检查环境配置文件..." -ForegroundColor Cyan
if (-not (Test-Path .env)) {
    Write-Host "未找到 .env 文件，从模板创建..." -ForegroundColor Yellow
    Copy-Item .env.example .env
    Write-Host ""
    Write-Host "警告: 请编辑 .env 文件，修改数据库密码等敏感信息！" -ForegroundColor Yellow
    Write-Host "编辑完成后，再次运行此脚本。" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "使用命令编辑: notepad .env" -ForegroundColor Cyan
    exit 0
}

Write-Host ""
Write-Host "开始部署服务..." -ForegroundColor Cyan
Write-Host ""

# 停止旧的容器
Write-Host "停止旧的容器..." -ForegroundColor Yellow
docker-compose down

# 构建并启动服务
Write-Host ""
Write-Host "构建镜像并启动服务（首次启动需要 5-10 分钟）..." -ForegroundColor Yellow
docker-compose up -d --build

# 等待服务启动
Write-Host ""
Write-Host "等待服务启动..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# 显示服务状态
Write-Host ""
Write-Host "======================================" -ForegroundColor Green
Write-Host "  服务状态" -ForegroundColor Green
Write-Host "======================================" -ForegroundColor Green
docker-compose ps

Write-Host ""
Write-Host "======================================" -ForegroundColor Green
Write-Host "  部署完成" -ForegroundColor Green
Write-Host "======================================" -ForegroundColor Green
Write-Host ""
Write-Host "前端地址: http://localhost" -ForegroundColor Cyan
Write-Host "后端API文档: http://localhost:82/doc.html" -ForegroundColor Cyan
Write-Host "InfluxDB控制台: http://localhost:8086" -ForegroundColor Cyan
Write-Host ""
Write-Host "默认账号: superAdmin" -ForegroundColor Yellow
Write-Host "默认密码: 123456" -ForegroundColor Yellow
Write-Host ""
Write-Host "查看日志: docker-compose logs -f" -ForegroundColor Cyan
Write-Host "停止服务: docker-compose stop" -ForegroundColor Cyan
Write-Host "重启服务: docker-compose restart" -ForegroundColor Cyan
Write-Host ""
