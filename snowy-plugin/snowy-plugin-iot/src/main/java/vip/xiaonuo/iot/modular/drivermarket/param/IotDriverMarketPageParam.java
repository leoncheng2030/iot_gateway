package vip.xiaonuo.iot.modular.drivermarket.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 驱动市场分页参数
 *
 * @author wqs
 * @date 2026/01/15
 */
@Getter
@Setter
public class IotDriverMarketPageParam {

    /** 当前页 */
    @Schema(description = "当前页码")
    private Integer current;

    /** 每页条数 */
    @Schema(description = "每页条数")
    private Integer size;

    /** 排序字段 */
    @Schema(description = "排序字段")
    private String sortField;

    /** 排序顺序 */
    @Schema(description = "排序顺序")
    private String sortOrder;

    /** 搜索关键字 */
    @Schema(description = "搜索关键字")
    private String searchKey;

    /** 来源类型 */
    @Schema(description = "来源类型")
    private String sourceType;

    /** 安装状态 */
    @Schema(description = "安装状态")
    private String installStatus;

    /** 分类 */
    @Schema(description = "分类")
    private String category;
}
