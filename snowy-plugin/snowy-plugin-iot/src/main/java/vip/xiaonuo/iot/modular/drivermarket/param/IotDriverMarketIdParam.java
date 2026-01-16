package vip.xiaonuo.iot.modular.drivermarket.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 驱动市场ID参数
 *
 * @author wqs
 * @date 2026/01/15
 */
@Getter
@Setter
public class IotDriverMarketIdParam {

    /** 主键ID */
    @Schema(description = "主键ID", required = true)
    @NotBlank(message = "id不能为空")
    private String id;
}
