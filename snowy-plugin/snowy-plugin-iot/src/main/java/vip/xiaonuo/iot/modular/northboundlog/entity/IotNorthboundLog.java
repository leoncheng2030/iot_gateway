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
package vip.xiaonuo.iot.modular.northboundlog.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fhs.core.trans.anno.Trans;
import com.fhs.core.trans.constant.TransType;
import com.fhs.core.trans.vo.TransPojo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 北向推送日志表实体
 *
 * @author jetox
 * @date  2026/01/08 10:25
 **/
@Getter
@Setter
@TableName("iot_northbound_log")
public class IotNorthboundLog implements TransPojo {

    /** 主键ID */
    @TableId
    @Schema(description = "主键ID")
    private String id;

    /** 推送配置ID */
    @Schema(description = "推送配置ID")
    @Trans(type = TransType.RPC, targetClassName = "vip.xiaonuo.iot.modular.northboundconfig.entity.IotNorthboundConfig", fields = "name", alias = "config", ref = "configName")
    private String configId;

    /** 配置名称(自动翻译) */
    @Schema(description = "配置名称")
    @TableField(exist = false)
    private String configName;

    /** 设备ID */
    @Schema(description = "设备ID")
    @Trans(type = TransType.RPC, targetClassName = "vip.xiaonuo.iot.modular.device.entity.IotDevice", fields = "deviceName", alias = "device", ref = "deviceName")
    private String deviceId;

    /** 设备名称(自动翻译) */
    @Schema(description = "设备名称")
    @TableField(exist = false)
    private String deviceName;

    /** 设备标识(冗余) */
    @Schema(description = "设备标识(冗余)")
    private String deviceKey;

    /** 推送类型 */
    @Schema(description = "推送类型")
    private String pushType;

    /** 目标地址 */
    @Schema(description = "目标地址")
    private String targetUrl;

    /** 推送数据内容 */
    @Schema(description = "推送数据内容")
    private String payload;

    /** 推送状态：SUCCESS/FAILED/RETRY */
    @Schema(description = "推送状态：SUCCESS/FAILED/RETRY")
    private String status;

    /** 响应状态码 */
    @Schema(description = "响应状态码")
    private Integer responseCode;

    /** 响应内容 */
    @Schema(description = "响应内容")
    private String responseBody;

    /** 错误信息 */
    @Schema(description = "错误信息")
    private String errorMessage;

    /** 重试次数 */
    @Schema(description = "重试次数")
    private Integer retryCount;

    /** 耗时(毫秒) */
    @Schema(description = "耗时(毫秒)")
    private Integer costTime;

    /** 推送时间 */
    @Schema(description = "推送时间")
    private Date pushTime;

    /** 创建时间 */
    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
