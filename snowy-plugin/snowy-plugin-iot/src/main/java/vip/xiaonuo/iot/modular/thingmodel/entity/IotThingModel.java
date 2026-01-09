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
package vip.xiaonuo.iot.modular.thingmodel.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import vip.xiaonuo.common.pojo.CommonEntity;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 物模型实体
 *
 * @author jetox
 * @date  2025/12/11 09:08
 **/
@Getter
@Setter
@TableName("iot_thing_model")
public class IotThingModel extends CommonEntity {

    /** 主键ID */
    @TableId
    @Schema(description = "主键ID")
    private String id;

    /** 产品ID */
    @Schema(description = "产品ID")
    private String productId;

    /** 功能类型 */
    @Schema(description = "功能类型")
    private String modelType;

    /** 标识符 */
    @Schema(description = "标识符")
    private String identifier;

    /** 功能名称 */
    @Schema(description = "功能名称")
    private String name;

    /** 值类型 */
    @Schema(description = "值类型")
    private String valueType;

    /** 值定义 */
    @Schema(description = "值定义")
    private String valueSpecs;

    /** 读写类型 */
    @Schema(description = "读写类型")
    private String accessMode;

    /** 是否必须 */
    @Schema(description = "是否必须")
    private Boolean required;

    /** 功能描述 */
    @Schema(description = "功能描述")
    private String description;

    /** 排序码 */
    @Schema(description = "排序码")
    private Integer sortCode;

    /** 扩展信息 */
    @Schema(description = "扩展信息")
    private String extJson;
}
