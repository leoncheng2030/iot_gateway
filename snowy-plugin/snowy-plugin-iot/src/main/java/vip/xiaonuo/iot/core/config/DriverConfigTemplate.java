package vip.xiaonuo.iot.core.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 驱动配置模板
 * 区分驱动级和设备级配置字段
 *
 * @author jetox
 * @date 2026/01/08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "驱动配置模板")
public class DriverConfigTemplate {

    /** 驱动级配置字段 */
    @Schema(description = "驱动级配置字段")
    private List<DriverConfigField> driverFields;

    /** 设备级配置字段 */
    @Schema(description = "设备级配置字段")
    private List<DriverConfigField> deviceFields;
}
