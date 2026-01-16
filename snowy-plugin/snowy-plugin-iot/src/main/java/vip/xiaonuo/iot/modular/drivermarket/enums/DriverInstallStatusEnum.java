package vip.xiaonuo.iot.modular.drivermarket.enums;

import lombok.Getter;

/**
 * 驱动安装状态枚举
 *
 * @author wqs
 * @date 2026/01/15
 */
@Getter
public enum DriverInstallStatusEnum {

    UNINSTALLED("UNINSTALLED", "未安装"),
    INSTALLED("INSTALLED", "已安装"),
    ENABLED("ENABLED", "已启用"),
    DISABLED("DISABLED", "已禁用");

    private final String value;
    private final String desc;

    DriverInstallStatusEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
