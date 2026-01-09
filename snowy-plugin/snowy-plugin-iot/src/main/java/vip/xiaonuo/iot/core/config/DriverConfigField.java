package vip.xiaonuo.iot.core.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 驱动配置字段定义
 * 用于动态生成驱动配置表单
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "驱动配置字段定义")
public class DriverConfigField {

    /** 字段key */
    @Schema(description = "字段key")
    private String key;

    /** 显示标签 */
    @Schema(description = "显示标签")
    private String label;

    /** 字段类型: text/number/select/switch */
    @Schema(description = "字段类型")
    private String type;

    /** 默认值 */
    @Schema(description = "默认值")
    private Object defaultValue;

    /** 最小值（数字类型） */
    @Schema(description = "最小值")
    private Integer min;

    /** 最大值（数字类型） */
    @Schema(description = "最大值")
    private Integer max;

    /** 选项（下拉类型） */
    @Schema(description = "选项")
    private List<FieldOption> options;

    /** 占位符 */
    @Schema(description = "占位符")
    private String placeholder;

    /** 提示文本 */
    @Schema(description = "提示文本")
    private String tip;

    /** 布局占比（12=半宽，24=全宽） */
    @Schema(description = "布局占比")
    private Integer span;

    /** 配置层级: driver(驱动级) / device(设备级) */
    @Schema(description = "配置层级")
    private String level;

    /** 是否必填（仅设备级字段有效） */
    @Schema(description = "是否必填")
    private Boolean required;

    /**
     * 下拉选项定义
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldOption {
        /** 选项值 */
        private Object value;
        /** 选项标签 */
        private String label;
    }
}
