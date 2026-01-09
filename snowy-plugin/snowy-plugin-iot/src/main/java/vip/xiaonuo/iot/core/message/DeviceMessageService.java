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
package vip.xiaonuo.iot.core.message;

/**
 * 设备消息服务接口
 *
 * @author yubaoshan
 * @date 2024/12/11 07:00
 **/
public interface DeviceMessageService {

    /**
     * 设备认证
     */
    boolean authenticate(String clientId, String username, String password);

    /**
     * 设备上线
     */
    void deviceOnline(String clientId, String ipAddress);

    /**
     * 设备离线
     */
    void deviceOffline(String clientId);

    /**
     * 处理设备消息
     */
    void handleDeviceMessage(String topic, String message);

    /**
     * 向设备下发消息
     */
    boolean sendToDevice(String deviceKey, String topic, String message);
}
