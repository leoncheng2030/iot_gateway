package vip.xiaonuo.iot.modular.device.entity;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 设备地址配置表（协议特定配置）
 *
 * @author gtc
 * @date 2026/01/10
 **/
@Getter
@Setter
@TableName("iot_device_address_config")
@Schema(description = "设备地址配置")
public class IotDeviceAddressConfig {

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    /** 关联映射表ID */
    @Schema(description = "关联映射表ID")
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

    /** 最后成功采集时间 */
    @Schema(description = "最后成功采集时间")
    private Date lastSuccessTime;

    /** 最后错误信息 */
    @Schema(description = "最后错误信息")
    private String lastErrorMessage;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private Date createTime;

    /** 更新时间 */
    @Schema(description = "更新时间")
    private Date updateTime;

    // ==================== 辅助方法：从 extConfig JSON 读取协议特定字段 ====================

    /**
     * 从 extConfig 中获取 Modbus 寄存器地址
     * @return 寄存器地址，如果不存在返回 null
     */
    public Integer getRegisterAddress() {
        if (extConfig == null || extConfig.isEmpty()) {
            return null;
        }
        try {
            return JSONUtil.parseObj(extConfig).getInt("registerAddress");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从 extConfig 中获取 Modbus 功能码
     * @return 功能码，如果不存在返回 null
     */
    public String getFunctionCode() {
        if (extConfig == null || extConfig.isEmpty()) {
            return null;
        }
        try {
            return JSONUtil.parseObj(extConfig).getStr("functionCode");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从 extConfig 中获取 Modbus 从站地址
     * @return 从站地址，如果不存在返回 1（默认值）
     */
    public Integer getSlaveAddress() {
        if (extConfig == null || extConfig.isEmpty()) {
            return 1;
        }
        try {
            Integer slaveAddr = JSONUtil.parseObj(extConfig).getInt("slaveAddress");
            return slaveAddr != null ? slaveAddr : 1;
        } catch (Exception e) {
            return 1;
        }
    }

    /**
     * 从 extConfig 中获取位索引（用于线圈操作）
     * @return 位索引，如果不存在返回 null
     */
    public Integer getBitIndex() {
        if (extConfig == null || extConfig.isEmpty()) {
            return null;
        }
        try {
            return JSONUtil.parseObj(extConfig).getInt("bitIndex");
        } catch (Exception e) {
            return null;
        }
    }
}
