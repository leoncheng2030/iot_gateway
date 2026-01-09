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
package vip.xiaonuo.iot.modular.northboundconfig.param;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 北向推送配置表编辑参数
 *
 * @author jetox
 * @date  2026/01/08 10:20
 **/
@Getter
@Setter
public class IotNorthboundConfigEditParam {

    /** 主键ID */
    @ExcelProperty("主键ID")
    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "id不能为空")
    private String id;

    /** 配置名称 */
    @ExcelProperty("配置名称")
    @Schema(description = "配置名称")
    private String name;

    /** 推送类型 */
    @ExcelProperty("推送类型")
    @Schema(description = "推送类型")
    private String pushType;

    /** 目标地址 */
    @ExcelProperty("目标地址")
    @Schema(description = "目标地址")
    private String targetUrl;

    /** 目标Topic(MQTT/Kafka使用) */
    @ExcelProperty("目标Topic(MQTT/Kafka使用)")
    @Schema(description = "目标Topic(MQTT/Kafka使用)")
    private String targetTopic;

    /** 认证方式 */
    @ExcelProperty("认证方式")
    @Schema(description = "认证方式")
    private String authType;

    /** 认证用户名 */
    @ExcelProperty("认证用户名")
    @Schema(description = "认证用户名")
    private String authUsername;

    /** 认证密码(加密) */
    @ExcelProperty("认证密码(加密)")
    @Schema(description = "认证密码(加密)")
    private String authPassword;

    /** 认证Token */
    @ExcelProperty("认证Token")
    @Schema(description = "认证Token")
    private String authToken;

    /** 自定义HTTP请求头(JSON) */
    @ExcelProperty("自定义HTTP请求头(JSON)")
    @Schema(description = "自定义HTTP请求头(JSON)")
    private String customHeaders;

    /** 数据过滤条件(JSON) */
    @ExcelProperty("数据过滤条件(JSON)")
    @Schema(description = "数据过滤条件(JSON)")
    private String dataFilter;

    /** 数据转换规则(JSON) */
    @ExcelProperty("数据转换规则(JSON)")
    @Schema(description = "数据转换规则(JSON)")
    private String dataTransform;

    /** 状态 */
    @ExcelProperty("状态")
    @Schema(description = "状态")
    private String enabled;

    /** 重试次数 */
    @ExcelProperty("重试次数")
    @Schema(description = "重试次数")
    private Integer retryTimes;

    /** 超时时间(毫秒) */
    @ExcelProperty("超时时间(毫秒)")
    @Schema(description = "超时时间(毫秒)")
    private Integer timeout;

    /** MQTT QoS等级(0/1/2) */
    @ExcelProperty("MQTT QoS等级(0/1/2)")
    @Schema(description = "MQTT QoS等级(0/1/2)")
    private Integer qos;

    /** 排序码 */
    @ExcelProperty("排序码")
    @Schema(description = "排序码")
    private Integer sortCode;

    /** 备注 */
    @ExcelProperty("备注")
    @Schema(description = "备注")
    private String remark;

    /** 扩展信息 */
    @ExcelProperty("扩展信息")
    @Schema(description = "扩展信息")
    private String extJson;

}
