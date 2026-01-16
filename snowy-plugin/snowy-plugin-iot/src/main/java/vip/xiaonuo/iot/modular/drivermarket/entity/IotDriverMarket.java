package vip.xiaonuo.iot.modular.drivermarket.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import vip.xiaonuo.common.pojo.CommonEntity;

import java.util.Date;

/**
 * 驱动市场实体
 *
 * @author wqs
 * @date 2026/01/15
 */
@Getter
@Setter
@TableName("iot_driver_market")
public class IotDriverMarket extends CommonEntity {

    /** 主键ID */
    @TableId
    @Schema(description = "主键ID")
    private String id;

    /** 驱动类型标识 */
    @Schema(description = "驱动类型标识")
    private String driverType;

    /** 驱动名称 */
    @Schema(description = "驱动名称")
    private String driverName;

    /** 驱动版本号 */
    @Schema(description = "驱动版本号")
    private String driverVersion;

    /** 来源类型：BUILTIN-内置/UPLOADED-上传 */
    @Schema(description = "来源类型")
    private String sourceType;

    /** 安装状态：UNINSTALLED/INSTALLED/ENABLED/DISABLED */
    @Schema(description = "安装状态")
    private String installStatus;

    /** 驱动描述 */
    @Schema(description = "驱动描述")
    private String description;

    /** 协议版本 */
    @Schema(description = "协议版本")
    private String protocolVersion;

    /** 供应商 */
    @Schema(description = "供应商")
    private String vendor;

    /** 图标URL */
    @Schema(description = "图标URL")
    private String iconUrl;

    /** 分类 */
    @Schema(description = "分类")
    private String category;

    /** JAR文件ID */
    @Schema(description = "JAR文件ID")
    private String jarFileId;

    /** JAR文件路径 */
    @Schema(description = "JAR文件路径")
    private String jarFilePath;

    /** 提供者类名 */
    @Schema(description = "提供者类名")
    private String providerClass;

    /** 配置字段JSON */
    @Schema(description = "配置字段JSON")
    private String configFields;

    /** 是否支持热更新 */
    @Schema(description = "是否支持热更新")
    private Boolean supportsHotUpdate;

    /** 安装次数 */
    @Schema(description = "安装次数")
    private Integer downloadCount;

    /** 安装时间 */
    @Schema(description = "安装时间")
    private Date installedTime;

    /** 启用时间 */
    @Schema(description = "启用时间")
    private Date enabledTime;

    /** 排序码 */
    @Schema(description = "排序码")
    private Integer sortCode;

    /** 扩展信息 */
    @Schema(description = "扩展信息")
    private String extJson;
}
