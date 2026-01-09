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
package vip.xiaonuo.iot.modular.register.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import vip.xiaonuo.common.pojo.CommonEntity;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 设备寄存器映射实体
 *
 * @author jetox
 * @date  2025/12/12 12:33
 **/
@Getter
@Setter
@TableName("iot_device_register_mapping")
public class IotDeviceRegisterMapping extends CommonEntity {

    /** 主键ID */
    @TableId
    @Schema(description = "主键ID")
    private String id;

    /** 设备ID */
    @Schema(description = "设备ID")
    private String deviceId;

    /** 物模型ID */
    @Schema(description = "物模型ID")
    private String thingModelId;

    /** 属性标识符 */
    @Schema(description = "属性标识符")
    private String identifier;

    /** 寄存器地址 */
    @Schema(description = "寄存器地址")
    private Integer registerAddress;

    /** Modbus功能码 */
    @Schema(description = "Modbus功能码")
    private String functionCode;

    /** 数据类型 */
    @Schema(description = "数据类型：bool,int,float")
    private String dataType;

    /** 缩放系数 */
    @Schema(description = "缩放系数（用于单位转换）")
    private BigDecimal scaleFactor;

    /** 偏移量 */
    @Schema(description = "偏移量")
    private BigDecimal offset;

    /** 位索引 */
    @Schema(description = "位索引（用于位操作，0-15）")
    private Integer bitIndex;

    /** 字节序 */
    @Schema(description = "字节序：BIG_ENDIAN,LITTLE_ENDIAN")
    private String byteOrder;

    /** 是否启用 */
    @Schema(description = "是否启用：0-禁用，1-启用")
    private Boolean enabled;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;

    /** 排序码 */
    @Schema(description = "排序码")
    private Integer sortCode;

    /** 扩展信息 */
    @Schema(description = "扩展信息")
    private String extJson;
}
