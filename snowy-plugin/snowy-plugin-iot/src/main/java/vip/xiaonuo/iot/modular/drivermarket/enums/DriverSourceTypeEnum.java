package vip.xiaonuo.iot.modular.drivermarket.enums;

import lombok.Getter;

/**
 * 驱动来源类型枚举
 *
 * @author wqs
 * @date 2026/01/15
 */
@Getter
public enum DriverSourceTypeEnum {

    BUILTIN("BUILTIN", "内置驱动"),
    UPLOADED("UPLOADED", "上传驱动");

    private final String value;
    private final String desc;

    DriverSourceTypeEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
