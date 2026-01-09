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
package vip.xiaonuo.iot.modular.device.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 设备添加参数
 *
 * @author jetox
 * @date  2025/12/11 07:24
 **/
@Getter
@Setter
public class IotDeviceAddParam {

    /** 设备名称 */
    @Schema(description = "设备名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "deviceName不能为空")
    private String deviceName;

    /** 设备标识 */
    @Schema(description = "设备标识", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "deviceKey不能为空")
    private String deviceKey;

    /** 设备密钥 */
    @Schema(description = "设备密钥", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "deviceSecret不能为空")
    private String deviceSecret;

    /** 产品ID */
    @Schema(description = "产品ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "productId不能为空")
    private String productId;

    /** 网关设备ID */
    @Schema(description = "网关设备ID")
    private String gatewayId;

    /** 设备状态 */
    @Schema(description = "设备状态", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "deviceStatus不能为空")
    private String deviceStatus;

    /** 激活时间 */
    @Schema(description = "激活时间")
    private Date activeTime;

    /** 最后在线时间 */
    @Schema(description = "最后在线时间")
    private Date lastOnlineTime;

    /** IP地址 */
    @Schema(description = "IP地址")
    private String ipAddress;

    /** 固件版本 */
    @Schema(description = "固件版本")
    private String firmwareVersion;

    /** 经度 */
    @Schema(description = "经度")
    private BigDecimal longitude;

    /** 纬度 */
    @Schema(description = "纬度")
    private BigDecimal latitude;

    /** 位置描述 */
    @Schema(description = "位置描述")
    private String location;

    /** 标签 */
    @Schema(description = "标签")
    private String tags;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;

    /** 排序码 */
    @Schema(description = "排序码")
    private Integer sortCode;

    /** Modbus从站地址 */
    @Schema(description = "Modbus从站地址(1-247)")
    private Integer modbusSlaveAddress;

}
