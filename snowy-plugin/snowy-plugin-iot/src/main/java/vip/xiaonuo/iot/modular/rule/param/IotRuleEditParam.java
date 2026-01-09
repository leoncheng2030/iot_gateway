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
package vip.xiaonuo.iot.modular.rule.param;

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
 * 规则引擎编辑参数
 *
 * @author jetox
 * @date  2025/12/11 07:32
 **/
@Getter
@Setter
public class IotRuleEditParam {

    /** 主键ID */
    @ExcelProperty("主键ID")
    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "id不能为空")
    private String id;

    /** 规则名称 */
    @ExcelProperty("规则名称")
    @Schema(description = "规则名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "ruleName不能为空")
    private String ruleName;

    /** 规则描述 */
    @ExcelProperty("规则描述")
    @Schema(description = "规则描述")
    private String ruleDesc;

    /** 规则类型 */
    @ExcelProperty("规则类型")
    @Schema(description = "规则类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "ruleType不能为空")
    private String ruleType;

    /** 触发条件 */
    @ExcelProperty("触发条件")
    @Schema(description = "触发条件", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "triggerCondition不能为空")
    private String triggerCondition;

    /** 执行动作 */
    @ExcelProperty("执行动作")
    @Schema(description = "执行动作", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "actions不能为空")
    private String actions;

    /** 状态 */
    @ExcelProperty("状态")
    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "status不能为空")
    private String status;

    /** 排序码 */
    @ExcelProperty("排序码")
    @Schema(description = "排序码")
    private Integer sortCode;

    /** 扩展信息 */
    @ExcelProperty("扩展信息")
    @Schema(description = "扩展信息")
    private String extJson;

}
