# 多阶段构建 - 后端应用
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

# 设置工作目录
WORKDIR /build

# 复制项目文件
COPY pom.xml .
COPY snowy-common ./snowy-common
COPY snowy-plugin ./snowy-plugin
COPY snowy-plugin-api ./snowy-plugin-api
COPY snowy-web-app ./snowy-web-app

# 构建项目
RUN mvn clean package -DskipTests -Dmaven.test.skip=true

# 运行阶段
FROM eclipse-temurin:17-jre-alpine

# 安装必要的工具
RUN apk add --no-cache tzdata curl && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apk del tzdata

# 创建应用目录
WORKDIR /app

# 从构建阶段复制 jar 文件
COPY --from=builder /build/snowy-web-app/target/snowy-web-app.jar /app/app.jar

# 创建日志目录
RUN mkdir -p /app/logs

# 暴露端口
EXPOSE 82

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:82/actuator/health || exit 1

# 设置 JVM 参数
ENV JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs/heapdump.hprof"

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
