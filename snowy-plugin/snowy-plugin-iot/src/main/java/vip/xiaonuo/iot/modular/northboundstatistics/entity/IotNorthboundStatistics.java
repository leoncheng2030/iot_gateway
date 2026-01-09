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
package vip.xiaonuo.iot.modular.northboundstatistics.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 北向推送统计表实体
 *
 * @author jetox
 * @date  2026/01/08 10:26
 **/
@Getter
@Setter
@TableName("iot_northbound_statistics")
public class IotNorthboundStatistics {

    /** 主键ID */
    @TableId
    @Schema(description = "主键ID")
    private String id;

    /** 推送配置ID */
    @Schema(description = "推送配置ID")
    private String configId;

    /** 统计日期 */
    @Schema(description = "统计日期")
    private Date statDate;

    /** 总推送次数 */
    @Schema(description = "总推送次数")
    private Integer totalCount;

    /** 成功次数 */
    @Schema(description = "成功次数")
    private Integer successCount;

    /** 失败次数 */
    @Schema(description = "失败次数")
    private Integer failedCount;

    /** 平均耗时(毫秒) */
    @Schema(description = "平均耗时(毫秒)")
    private Integer avgCostTime;

    /** 最大耗时(毫秒) */
    @Schema(description = "最大耗时(毫秒)")
    private Integer maxCostTime;

    /** 创建时间 */
    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /** 更新时间 */
    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.UPDATE)
    private Date updateTime;
}
