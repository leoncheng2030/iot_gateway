package vip.xiaonuo.iot.core.config;

import lombok.extern.slf4j.Slf4j;

/**
 * Netty线程池配置
 * 
 * 统一管理所有协议服务的Netty线程池配置
 * 避免资源浪费和性能瓶颈
 *
 * @author jetox
 * @date 2025/12/11
 */
@Slf4j
public class NettyThreadPoolConfig {

    /**
     * Boss线程数（接受连接）
     * 固定为1，因为单线程足以处理新连接
     */
    public static final int BOSS_THREADS = 1;

    /**
     * Worker线程数（处理业务）
     * 根据CPU核心数动态调整，最少4个，最多不超过CPU核心数
     * 
     * 设计思路：
     * - 4核CPU -> 4线程
     * - 8核CPU -> 8线程
     * - 16核CPU -> 16线程
     */
    public static final int WORKER_THREADS = Math.max(4, Runtime.getRuntime().availableProcessors());

    /**
     * Modbus客户端线程数（主动连接设备）
     * 适用于ModbusTcpClient轮询场景
     * 
     * 设计思路：
     * - 每个线程可处理多个连接（通过NIO多路复用）
     * - 4-8线程足以支持数百台设备轮询
     */
    public static final int MODBUS_CLIENT_THREADS = Math.max(4, Runtime.getRuntime().availableProcessors());

    /**
     * 优雅停机超时时间（秒）
     * 所有协议服务器统一使用此配置
     * 
     * 设计思路：
     * - quietPeriod: 0秒（立即开始关闭）
     * - timeout: 10秒（最长等待时间）
     * - 超过10秒强制关闭，避免无限等待
     */
    public static final int SHUTDOWN_QUIET_PERIOD = 0;
    public static final int SHUTDOWN_TIMEOUT = 10;

    static {
        log.info(">>> Netty线程池配置初始化完成");
        log.info(">>> Boss线程数: {}", BOSS_THREADS);
        log.info(">>> Worker线程数: {}", WORKER_THREADS);
        log.info(">>> Modbus客户端线程数: {}", MODBUS_CLIENT_THREADS);
        log.info(">>> 优雅停机超时: {}秒", SHUTDOWN_TIMEOUT);
    }

    /**
     * 获取Boss线程数
     */
    public static int getBossThreads() {
        return BOSS_THREADS;
    }

    /**
     * 获取Worker线程数
     */
    public static int getWorkerThreads() {
        return WORKER_THREADS;
    }

    /**
     * 获取Modbus客户端线程数
     */
    public static int getModbusClientThreads() {
        return MODBUS_CLIENT_THREADS;
    }

    /**
     * 获取优雅停机静默期（秒）
     */
    public static int getShutdownQuietPeriod() {
        return SHUTDOWN_QUIET_PERIOD;
    }

    /**
     * 获取优雅停机超时时间（秒）
     */
    public static int getShutdownTimeout() {
        return SHUTDOWN_TIMEOUT;
    }
}
