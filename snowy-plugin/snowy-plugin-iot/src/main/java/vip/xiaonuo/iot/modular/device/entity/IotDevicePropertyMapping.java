package vip.xiaonuo.iot.modular.device.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 设备属性映射表（物模型关联）
 *
 * @author gtc
 * @date 2026/01/10
 **/
@Getter
@Setter
@TableName("iot_device_property_mapping")
@Schema(description = "设备属性映射")
public class IotDevicePropertyMapping {

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    /** 设备ID */
    @Schema(description = "设备ID")
    private String deviceId;

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
