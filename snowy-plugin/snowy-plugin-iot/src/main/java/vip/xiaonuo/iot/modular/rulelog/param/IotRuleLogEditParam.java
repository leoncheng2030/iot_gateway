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
package vip.xiaonuo.iot.modular.rulelog.param;

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
 * 规则执行日志表编辑参数
 *
 * @author jetox
 * @date  2025/12/11 07:39
 **/
@Getter
@Setter
public class IotRuleLogEditParam {

    /** 主键ID */
    @ExcelProperty("主键ID")
    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "id不能为空")
    private String id;

    /** 规则ID */
    @ExcelProperty("规则ID")
    @Schema(description = "规则ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "ruleId不能为空")
    private String ruleId;

    /** 规则名称 */
    @ExcelProperty("规则名称")
    @Schema(description = "规则名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "ruleName不能为空")
    private String ruleName;

    /** 触发数据 */
    @ExcelProperty("触发数据")
    @Schema(description = "触发数据", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "triggerData不能为空")
    private String triggerData;

    /** 执行结果 */
    @ExcelProperty("执行结果")
    @Schema(description = "执行结果")
    private String executeResult;

    /** 错误信息 */
    @ExcelProperty("错误信息")
    @Schema(description = "错误信息")
    private String errorMsg;

    /** 执行时间 */
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ExcelProperty("执行时间")
    @Schema(description = "执行时间")
    private Date executeTime;

}
