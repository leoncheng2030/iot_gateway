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
package vip.xiaonuo.iot.modular.devicedata.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 设备数据添加参数
 *
 * @author jetox
 * @date  2025/12/11 07:27
 **/
@Getter
@Setter
public class IotDeviceDataAddParam {

    /** 设备ID */
    @Schema(description = "设备ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "deviceId不能为空")
    private String deviceId;

    /** 数据类型 */
    @Schema(description = "数据类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "dataType不能为空")
    private String dataType;

    /** 数据标识 */
    @Schema(description = "数据标识", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "dataKey不能为空")
    private String dataKey;

    /** 数据值 */
    @Schema(description = "数据值", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "dataValue不能为空")
    private String dataValue;

    /** 数据时间 */
    @Schema(description = "数据时间")
    private Date dataTime;

}
