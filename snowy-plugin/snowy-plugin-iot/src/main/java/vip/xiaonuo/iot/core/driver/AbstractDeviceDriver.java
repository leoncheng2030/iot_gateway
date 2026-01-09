/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 */
package vip.xiaonuo.iot.core.driver;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 设备驱动抽象基类
 * 
 * @author yubaoshan
 * @date 2025/12/13
 */
@Slf4j
public abstract class AbstractDeviceDriver implements DeviceDriver {

    /**
     * 驱动运行状态
     */
    protected final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 驱动配置
     */
    protected DriverConfig config;

    public AbstractDeviceDriver(DriverConfig config) {
        this.config = config;
    }

    @Override
    public void start() throws Exception {
        if (running.get()) {
            log.warn("驱动 [{}] 已在运行中", getDriverName());
            return;
        }
        
        log.info("启动驱动: {}", getDriverName());
        doStart();
        running.set(true);
        log.info("驱动 [{}] 启动成功", getDriverName());
    }

    @Override
    public void stop() throws Exception {
        if (!running.get()) {
            log.warn("驱动 [{}] 未运行", getDriverName());
            return;
        }
        
        log.info("停止驱动: {}", getDriverName());
        doStop();
        running.set(false);
        log.info("驱动 [{}] 已停止", getDriverName());
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    /**
     * 子类实现启动逻辑
     */
    protected abstract void doStart() throws Exception;

    /**
     * 子类实现停止逻辑
     */
    protected abstract void doStop() throws Exception;
}
