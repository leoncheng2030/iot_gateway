package vip.xiaonuo.iot.modular.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 产品属性映射表（物模型关联）
 *
 * @author gtc
 * @date 2026/01/11
 **/
@Getter
@Setter
@TableName("iot_product_property_mapping")
@Schema(description = "产品属性映射")
public class IotProductPropertyMapping {

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    /** 产品ID */
    @Schema(description = "产品ID")
    private String productId;

    /** 物模型属性ID */
    @Schema(description = "物模型属性ID")
    private String thingModelId;

    /** 属性标识符（冗余字段，便于查询） */
    @Schema(description = "属性标识符")
    private String identifier;

    /** 是否启用 */
    @Schema(description = "是否启用")
    private Boolean enabled;

    /** 排序 */
    @Schema(description = "排序")
    private Integer sortCode;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private Date createTime;

    /** 更新时间 */
    @Schema(description = "更新时间")
    private Date updateTime;
}
