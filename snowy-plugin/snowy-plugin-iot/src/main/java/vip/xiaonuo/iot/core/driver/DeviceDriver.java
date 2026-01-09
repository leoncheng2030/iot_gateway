/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 */
package vip.xiaonuo.iot.core.driver;

import cn.hutool.json.JSONObject;
import vip.xiaonuo.iot.core.config.DriverConfigField;

import java.util.List;

/**
 * 设备驱动抽象接口 - 对齐IBOOT设备驱动层
 * 
 * 支持协议类型：
 * - 直连设备驱动 (TCP/UDP)
 * - DTU网关驱动 (串口转网络)
 * - MQTT网关驱动
 * - Modbus网关驱动
 * - 其他网关驱动 (LoRa/Zigbee等)
 *
 * @author yubaoshan
 * @date 2025/12/13
 */
public interface DeviceDriver {

    /**
     * 驱动类型
     */
    String getDriverType();

    /**
     * 驱动名称
     */
    String getDriverName();

    /**
     * 启动驱动
     */
    void start() throws Exception;

    /**
     * 停止驱动
     */
    void stop() throws Exception;

    /**
     * 驱动状态
     */
    boolean isRunning();

    /**
     * 读取设备数据
     * @param deviceKey 设备标识
     * @param params 读取参数
     * @return 设备数据
     */
    JSONObject readData(String deviceKey, JSONObject params) throws Exception;

    /**
     * 写入设备数据
     * @param deviceKey 设备标识
     * @param data 写入数据
     * @return 是否成功
     */
    boolean writeData(String deviceKey, JSONObject data) throws Exception;

    /**
     * 设备连接状态检测
     * @param deviceKey 设备标识
     * @return 是否在线
     */
    boolean isDeviceOnline(String deviceKey);

    /**
     * 获取驱动配置字段定义
     * 用于前端动态生成配置表单
     * @return 配置字段列表
     */
    default List<DriverConfigField> getConfigFields() {
        return List.of();
    }
}
