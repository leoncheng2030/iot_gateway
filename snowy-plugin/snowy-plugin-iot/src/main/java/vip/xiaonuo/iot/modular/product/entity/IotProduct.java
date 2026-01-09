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
package vip.xiaonuo.iot.modular.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import vip.xiaonuo.common.pojo.CommonEntity;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 产品实体
 *
 * @author jetox
 * @date  2025/12/11 06:57
 **/
@Getter
@Setter
@TableName("iot_product")
public class IotProduct extends CommonEntity {

    /** 主键ID */
    @TableId
    @Schema(description = "主键ID")
    private String id;

    /** 产品名称 */
    @Schema(description = "产品名称",requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "产品名称不能为空")
    private String productName;

    /** 产品标识 */
    @Schema(description = "产品标识",requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "产品标识不能为空")
    private String productKey;

    /** 产品类型 */
    @Schema(description = "产品类型",requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "产品类型不能为空")
    private String productType;

    /** 接入协议 */
    @Schema(description = "接入协议",requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "接入协议不能为空")
    private String protocolType;

    /** 数据格式 */
    @Schema(description = "数据格式",requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "数据格式不能为空")
    private String dataFormat;

    /** 产品描述 */
    @Schema(description = "产品描述")
    private String productDesc;

    /** 状态 */
    @Schema(description = "状态",requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "状态不能为空")
    private String status;

    /** 排序码 */
    @Schema(description = "排序码")
    private Integer sortCode;

    /** 扩展信息 */
    @Schema(description = "扩展信息")
    private String extJson;
}
