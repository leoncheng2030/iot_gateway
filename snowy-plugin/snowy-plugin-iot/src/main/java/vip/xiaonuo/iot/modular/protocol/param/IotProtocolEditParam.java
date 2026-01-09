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
package vip.xiaonuo.iot.modular.protocol.param;

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
 * 协议配置编辑参数
 *
 * @author jetox
 * @date  2025/12/11 07:09
 **/
@Getter
@Setter
public class IotProtocolEditParam {

    /** 主键ID */
    @ExcelProperty("主键ID")
    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "id不能为空")
    private String id;

    /** 协议名称 */
    @ExcelProperty("协议名称")
    @Schema(description = "协议名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "protocolName不能为空")
    private String protocolName;

    /** 协议类型 */
    @ExcelProperty("协议类型")
    @Schema(description = "协议类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "protocolType不能为空")
    private String protocolType;

    /** 协议端口 */
    @ExcelProperty("协议端口")
    @Schema(description = "协议端口", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "protocolPort不能为空")
    private Integer protocolPort;

    /** 协议配置 */
    @ExcelProperty("协议配置")
    @Schema(description = "协议配置", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "configJson不能为空")
    private String configJson;

    /** 状态 */
    @ExcelProperty("状态")
    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "status不能为空")
    private String status;

    /** 备注 */
    @ExcelProperty("备注")
    @Schema(description = "备注")
    private String remark;

    /** 排序码 */
    @ExcelProperty("排序码")
    @Schema(description = "排序码")
    private Integer sortCode;

    /** 扩展信息 */
    @ExcelProperty("扩展信息")
    @Schema(description = "扩展信息")
    private String extJson;

}
