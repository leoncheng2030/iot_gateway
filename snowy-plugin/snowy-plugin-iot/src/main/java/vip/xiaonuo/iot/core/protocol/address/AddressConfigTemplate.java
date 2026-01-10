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
package vip.xiaonuo.iot.core.protocol.address;

import lombok.Data;
import java.util.List;

/**
 * 协议地址配置模板
 * 每个协议定义自己的地址配置方式
 *
 * @author jetox
 * @date 2026/01/10 17:30
 **/
@Data
public class AddressConfigTemplate {

    /**
     * 协议类型
     */
    private String protocolType;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 地址输入方式: SIMPLE(简单输入), BUILDER(构建器), HYBRID(混合模式)
     */
    private AddressInputMode inputMode;

    /**
     * 地址格式说明
     */
    private String formatDescription;

    /**
     * 地址示例
     */
    private List<String> examples;

    /**
     * 配置字段列表
     */
    private List<ConfigField> fields;

    /**
     * 地址验证正则表达式（可选）
     */
    private String validationRegex;

    /**
     * 配置字段定义
     */
    @Data
    public static class ConfigField {
        /**
         * 字段名称（用于存储）
         */
        private String name;

        /**
         * 字段标签（显示名称）
         */
        private String label;

        /**
         * 字段类型: TEXT, NUMBER, SELECT, RADIO
         */
        private FieldType type;

        /**
         * 字段描述/提示
         */
        private String description;

        /**
         * 是否必填
         */
        private Boolean required;

        /**
         * 默认值
         */
        private Object defaultValue;

        /**
         * 选项列表（仅 SELECT/RADIO 类型）
         */
        private List<Option> options;

        /**
         * 最小值（仅 NUMBER 类型）
         */
        private Integer min;

        /**
         * 最大值（仅 NUMBER 类型）
         */
        private Integer max;

        /**
         * 显示条件（动态显示/隐藏字段）
         * 例如: "area=DB" 表示当 area 字段值为 DB 时才显示
         */
        private String showWhen;
    }

    /**
     * 选项定义
     */
    @Data
    public static class Option {
        private String label;
        private Object value;
        private String description;
    }

    /**
     * 地址输入模式
     */
    public enum AddressInputMode {
        /**
         * 简单输入模式：单个文本框或数字框
         * 适用于：Modbus（数字地址）
         */
        SIMPLE,

        /**
         * 构建器模式：多个字段组合构建地址
         * 适用于：S7（区域+类型+偏移）、BACnet（对象类型+实例+属性）
         */
        BUILDER,

        /**
         * 混合模式：既可以构建器，也可以直接输入
         * 适用于：复杂协议
         */
        HYBRID
    }

    /**
     * 字段类型
     */
    public enum FieldType {
        /**
         * 文本输入框
         */
        TEXT,

        /**
         * 数字输入框
         */
        NUMBER,

        /**
         * 下拉选择框
         */
        SELECT,

        /**
         * 单选按钮
         */
        RADIO
    }
}
