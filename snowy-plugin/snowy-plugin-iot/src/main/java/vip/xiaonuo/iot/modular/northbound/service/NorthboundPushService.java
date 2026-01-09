/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 *
 * Snowy采用APACHE LICENSE 2.0开源协议，您在使用过程中，需要注意以下几点：
 *
 * 1.请不要删除和修改根目录下的LICENSE文件。
 * 2.请不要删除和修改Snowy源码头部的版权声明。
 * 3.本项目代码可免费商业使用，商业使用请保留源码和相关描述文件的项目出处，作者声明等。
 * 4.分发源码时候，请注明软件出处 https://www.xiaonuo.vip
 * 5.不可二次分发开源参与同类竞品，如有想法可联系团队xiaonuobase@qq.com商议合作。
 * 6.若您的项目无法满足以上几点，需要更多功能代码，获取Snowy商业授权许可，请在官网购买授权，地址为 https://www.xiaonuo.vip
 */
package vip.xiaonuo.iot.modular.northbound.service;

import cn.hutool.json.JSONObject;
import vip.xiaonuo.iot.modular.device.entity.IotDevice;

/**
 * 北向推送服务接口
 *
 * @author yubaoshan
 * @date 2026/01/08
 */
public interface NorthboundPushService {

    /**
     * 推送设备数据到外部系统
     * @param device 设备信息
     * @param data 设备数据
     */
    void pushDeviceData(IotDevice device, JSONObject data);

    /**
     * 推送设备事件到外部系统
     * @param device 设备信息
     * @param eventData 事件数据
     */
    void pushDeviceEvent(IotDevice device, JSONObject eventData);

    /**
     * 推送设备状态变化到外部系统
     * @param device 设备信息
     * @param status 设备状态
     */
    void pushDeviceStatus(IotDevice device, String status);

    /**
     * 测试北向推送配置连接
     * @param configId 配置ID
     * @return 测试结果
     */
    JSONObject testConnection(String configId);
}
