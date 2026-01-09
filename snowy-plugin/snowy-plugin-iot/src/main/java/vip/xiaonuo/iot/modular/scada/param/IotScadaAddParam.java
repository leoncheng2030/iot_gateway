/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 */
package vip.xiaonuo.iot.modular.scada.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;

/**
 * 组态添加参数
 *
 * @author jetox
 * @date 2025/12/14
 */
@Getter
@Setter
public class IotScadaAddParam {

    /** 组态名称 */
    @Schema(description = "组态名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "name不能为空")
    private String name;

    /** 组态配置JSON */
    @Schema(description = "组态配置JSON")
    private String config;
}
