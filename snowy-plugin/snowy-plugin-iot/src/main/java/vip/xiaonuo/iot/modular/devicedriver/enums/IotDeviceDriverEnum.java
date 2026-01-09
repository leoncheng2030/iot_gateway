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
package vip.xiaonuo.iot.modular.devicedriver.enums;

import lombok.Getter;

/**
 * 设备驱动配置表枚举
 *
 * @author jetox
 * @date  2025/12/13 09:45
 **/
@Getter
public enum IotDeviceDriverEnum {

    /** 驱动类型 */
    DTU_GATEWAY("DTU网关驱动", "DTU_GATEWAY"),
    TCP_DIRECT("TCP直连驱动", "TCP_DIRECT"),
    UDP_DIRECT("UDP直连驱动", "UDP_DIRECT"),
    MODBUS_TCP("Modbus TCP驱动", "MODBUS_TCP"),
    MQTT("MQTT驱动", "MQTT"),
    HTTP("HTTP驱动", "HTTP"),
    LORA_GATEWAY("LoRa网关驱动", "LORA_GATEWAY"),
    ZIGBEE_GATEWAY("Zigbee网关驱动", "ZIGBEE_GATEWAY"),
    OPCUA("OPC UA驱动", "OPCUA"),
    CUSTOM("自定义驱动", "CUSTOM"),
    
    /** 驱动状态 */
    RUNNING("运行中", "RUNNING"),
    STOPPED("已停止", "STOPPED"),
    ERROR("错误", "ERROR");

    private final String name;
    private final String value;

    IotDeviceDriverEnum(String name, String value) {
        this.name = name;
        this.value = value;
    }
}
