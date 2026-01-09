/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 */
package vip.xiaonuo.iot.modular.scada.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 组态分页查询参数
 *
 * @author jetox
 * @date 2025/12/14
 */
@Getter
@Setter
public class IotScadaPageParam {

    /** 当前页 */
    @Schema(description = "当前页")
    private Integer current;

    /** 每页条数 */
    @Schema(description = "每页条数")
    private Integer size;

    /** 组态名称 */
    @Schema(description = "组态名称")
    private String name;
}
