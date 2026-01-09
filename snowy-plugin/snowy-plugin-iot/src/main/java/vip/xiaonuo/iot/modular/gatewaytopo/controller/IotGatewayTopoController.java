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
package vip.xiaonuo.iot.modular.gatewaytopo.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vip.xiaonuo.common.annotation.CommonLog;
import vip.xiaonuo.common.pojo.CommonResult;
import vip.xiaonuo.iot.modular.gatewaytopo.entity.IotGatewayTopo;
import vip.xiaonuo.iot.modular.gatewaytopo.param.IotGatewayTopoAddParam;
import vip.xiaonuo.iot.modular.gatewaytopo.param.IotGatewayTopoEditParam;
import vip.xiaonuo.iot.modular.gatewaytopo.param.IotGatewayTopoIdParam;
import vip.xiaonuo.iot.modular.gatewaytopo.param.IotGatewayTopoPageParam;
import vip.xiaonuo.iot.modular.gatewaytopo.service.IotGatewayTopoService;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.io.IOException;
import java.util.List;

/**
 * 网关拓扑控制器
 *
 * @author jetox
 * @date  2025/12/11 07:29
 */
@Tag(name = "网关拓扑控制器")
@RestController
@Validated
public class IotGatewayTopoController {

    @Resource
    private IotGatewayTopoService iotGatewayTopoService;

    /**
     * 获取网关拓扑分页
     *
     * @author jetox
     * @date  2025/12/11 07:29
     */
    @Operation(summary = "获取网关拓扑分页")
    @SaCheckPermission("/iot/gatewaytopo/page")
    @GetMapping("/iot/gatewaytopo/page")
    public CommonResult<Page<IotGatewayTopo>> page(IotGatewayTopoPageParam iotGatewayTopoPageParam) {
        return CommonResult.data(iotGatewayTopoService.page(iotGatewayTopoPageParam));
    }

    /**
     * 添加网关拓扑
     *
     * @author jetox
     * @date  2025/12/11 07:29
     */
    @Operation(summary = "添加网关拓扑")
    @CommonLog("添加网关拓扑")
    @SaCheckPermission("/iot/gatewaytopo/add")
    @PostMapping("/iot/gatewaytopo/add")
    public CommonResult<String> add(@RequestBody @Valid IotGatewayTopoAddParam iotGatewayTopoAddParam) {
        iotGatewayTopoService.add(iotGatewayTopoAddParam);
        return CommonResult.ok();
    }

    /**
     * 编辑网关拓扑
     *
     * @author jetox
     * @date  2025/12/11 07:29
     */
    @Operation(summary = "编辑网关拓扑")
    @CommonLog("编辑网关拓扑")
    @SaCheckPermission("/iot/gatewaytopo/edit")
    @PostMapping("/iot/gatewaytopo/edit")
    public CommonResult<String> edit(@RequestBody @Valid IotGatewayTopoEditParam iotGatewayTopoEditParam) {
        iotGatewayTopoService.edit(iotGatewayTopoEditParam);
        return CommonResult.ok();
    }

    /**
     * 删除网关拓扑
     *
     * @author jetox
     * @date  2025/12/11 07:29
     */
    @Operation(summary = "删除网关拓扑")
    @CommonLog("删除网关拓扑")
    @SaCheckPermission("/iot/gatewaytopo/delete")
    @PostMapping("/iot/gatewaytopo/delete")
    public CommonResult<String> delete(@RequestBody @Valid @NotEmpty(message = "集合不能为空")
                                                   List<IotGatewayTopoIdParam> iotGatewayTopoIdParamList) {
        iotGatewayTopoService.delete(iotGatewayTopoIdParamList);
        return CommonResult.ok();
    }

    /**
     * 获取网关拓扑详情
     *
     * @author jetox
     * @date  2025/12/11 07:29
     */
    @Operation(summary = "获取网关拓扑详情")
    @SaCheckPermission("/iot/gatewaytopo/detail")
    @GetMapping("/iot/gatewaytopo/detail")
    public CommonResult<IotGatewayTopo> detail(@Valid IotGatewayTopoIdParam iotGatewayTopoIdParam) {
        return CommonResult.data(iotGatewayTopoService.detail(iotGatewayTopoIdParam));
    }

    /**
     * 下载网关拓扑导入模板
     *
     * @author jetox
     * @date  2025/12/11 07:29
     */
    @Operation(summary = "下载网关拓扑导入模板")
    @GetMapping(value = "/iot/gatewaytopo/downloadImportTemplate", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
        iotGatewayTopoService.downloadImportTemplate(response);
    }

    /**
     * 导入网关拓扑
     *
     * @author jetox
     * @date  2025/12/11 07:29
     */
    @Operation(summary = "导入网关拓扑")
    @CommonLog("导入网关拓扑")
    @SaCheckPermission("/iot/gatewaytopo/importData")
    @PostMapping("/iot/gatewaytopo/importData")
    public CommonResult<JSONObject> importData(@RequestPart("file") MultipartFile file) {
        return CommonResult.data(iotGatewayTopoService.importData(file));
    }

    /**
     * 导出网关拓扑
     *
     * @author jetox
     * @date  2025/12/11 07:29
     */
    @Operation(summary = "导出网关拓扑")
    @SaCheckPermission("/iot/gatewaytopo/exportData")
    @PostMapping(value = "/iot/gatewaytopo/exportData", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void exportData(@RequestBody List<IotGatewayTopoIdParam> iotGatewayTopoIdParamList, HttpServletResponse response) throws IOException {
        iotGatewayTopoService.exportData(iotGatewayTopoIdParamList, response);
    }
}
