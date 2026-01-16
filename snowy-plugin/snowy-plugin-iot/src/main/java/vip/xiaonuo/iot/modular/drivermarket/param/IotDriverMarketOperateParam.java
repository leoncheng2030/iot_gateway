package vip.xiaonuo.iot.modular.drivermarket.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 驱动市场操作参数
 *
 * @author wqs
 * @date 2026/01/15
 */
@Getter
@Setter
public class IotDriverMarketOperateParam {

    /** 驱动类型 */
    @Schema(description = "驱动类型", required = true)
    @NotBlank(message = "驱动类型不能为空")
    private String driverType;
}
