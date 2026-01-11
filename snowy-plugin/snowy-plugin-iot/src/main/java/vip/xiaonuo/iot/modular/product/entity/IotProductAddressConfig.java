package vip.xiaonuo.iot.modular.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 产品级地址配置表（协议特定配置）
 *
 * @author gtc
 * @date 2026/01/11
 **/
@Getter
@Setter
@TableName("iot_product_address_config")
@Schema(description = "产品级地址配置")
public class IotProductAddressConfig {

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    /** 关联产品属性映射表ID */
    @Schema(description = "关联产品属性映射表ID")
    private String mappingId;

    /** 协议类型 */
    @Schema(description = "协议类型：S7/MODBUS_TCP/OPC_UA/BACNET/FINS/MC")
    private String protocolType;

    /** 设备地址（协议特定格式） */
    @Schema(description = "设备地址（协议特定格式，如：DB1.DBW0、40001、ns=2;s=Temp）")
    private String deviceAddress;

    /** 数据类型 */
    @Schema(description = "数据类型：int/float/bool/string/word/dword/real")
    private String dataType;

    /** 数值倍率（缩放系数） */
    @Schema(description = "数值倍率（缩放系数）")
    private BigDecimal valueMultiplier;

    /** 数值偏移 */
    @Schema(description = "数值偏移")
    private BigDecimal valueOffset;

    /** 字节序 */
    @Schema(description = "字节序：BIG_ENDIAN/LITTLE_ENDIAN")
    private String byteOrder;

    /** 协议特有参数配置（JSON格式）
     * Modbus示例: {"functionCode":"03","registerAddress":40001,"slaveAddress":1,"bitIndex":0}
     * S7示例: {"dbNumber":1,"byteOffset":0,"bitOffset":0,"plcDataType":"REAL"}
     * OPC UA示例: {"namespaceIndex":2,"browseName":"Temperature","nodeClass":"Variable"}
     */
    @Schema(description = "扩展配置（协议特定参数，JSON格式）")
    private String extConfig;

    /** 采集间隔(ms)，0表示使用设备默认 */
    @Schema(description = "采集间隔(ms)")
    private Integer pollingInterval;

    /** 超时时间(ms) */
    @Schema(description = "超时时间(ms)")
    private Integer timeout;

    /** 重试次数 */
    @Schema(description = "重试次数")
    private Integer retryCount;

    /** 是否启用 */
    @Schema(description = "是否启用")
    private Boolean enabled;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private Date createTime;

    /** 更新时间 */
    @Schema(description = "更新时间")
    private Date updateTime;
}
