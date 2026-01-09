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
package vip.xiaonuo.iot.modular.driverlog.param;

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
 * 运行日志编辑参数
 *
 * @author jetox
 * @date  2025/12/13 09:46
 **/
@Getter
@Setter
public class IotDriverLogEditParam {

    /** 主键 */
    @ExcelProperty("主键")
    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "id不能为空")
    private String id;

    /** 驱动ID */
    @ExcelProperty("驱动ID")
    @Schema(description = "驱动ID")
    private String driverId;

    /** 驱动名称 */
    @ExcelProperty("驱动名称")
    @Schema(description = "驱动名称")
    private String driverName;

    /** 日志类型 */
    @ExcelProperty("日志类型")
    @Schema(description = "日志类型")
    private String logType;

    /** 日志内容 */
    @ExcelProperty("日志内容")
    @Schema(description = "日志内容")
    private String logContent;

    /** 关联设备标识 */
    @ExcelProperty("关联设备标识")
    @Schema(description = "关联设备标识")
    private String deviceKey;

    /** 错误信息 */
    @ExcelProperty("错误信息")
    @Schema(description = "错误信息")
    private String errorMsg;

    /** 扩展信息 */
    @ExcelProperty("扩展信息")
    @Schema(description = "扩展信息")
    private String extJson;

}
