/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 */
package vip.xiaonuo.iot.modular.scada.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vip.xiaonuo.common.annotation.CommonLog;
import vip.xiaonuo.common.pojo.CommonResult;
import vip.xiaonuo.iot.modular.scada.entity.IotScada;
import vip.xiaonuo.iot.modular.scada.param.IotScadaAddParam;
import vip.xiaonuo.iot.modular.scada.param.IotScadaEditParam;
import vip.xiaonuo.iot.modular.scada.param.IotScadaIdParam;
import vip.xiaonuo.iot.modular.scada.param.IotScadaPageParam;
import vip.xiaonuo.iot.modular.scada.service.IotScadaService;

import java.util.List;

/**
 * 组态控制器
 *
 * @author jetox
 * @date 2025/12/14
 */
@Tag(name = "组态控制器")
@RestController
@Validated
public class IotScadaController {

    @Resource
    private IotScadaService iotScadaService;

    /**
     * 获取组态分页
     */
    @Operation(summary = "获取组态分页")
    @SaCheckPermission("/iot/scada/page")
    @GetMapping("/iot/scada/page")
    public CommonResult<Page<IotScada>> page(IotScadaPageParam iotScadaPageParam) {
        return CommonResult.data(iotScadaService.page(iotScadaPageParam));
    }

    /**
     * 添加组态
     */
    @Operation(summary = "添加组态")
    @CommonLog("添加组态")
    @SaCheckPermission("/iot/scada/add")
    @PostMapping("/iot/scada/add")
    public CommonResult<String> add(@RequestBody @Valid IotScadaAddParam iotScadaAddParam) {
        iotScadaService.add(iotScadaAddParam);
        return CommonResult.ok();
    }

    /**
     * 编辑组态
     */
    @Operation(summary = "编辑组态")
    @CommonLog("编辑组态")
    @SaCheckPermission("/iot/scada/edit")
    @PostMapping("/iot/scada/edit")
    public CommonResult<String> edit(@RequestBody @Valid IotScadaEditParam iotScadaEditParam) {
        iotScadaService.edit(iotScadaEditParam);
        return CommonResult.ok();
    }

    /**
     * 删除组态
     */
    @Operation(summary = "删除组态")
    @CommonLog("删除组态")
    @SaCheckPermission("/iot/scada/delete")
    @PostMapping("/iot/scada/delete")
    public CommonResult<String> delete(@RequestBody @Valid @NotEmpty(message = "集合不能为空")
                                                    List<IotScadaIdParam> iotScadaIdParamList) {
        iotScadaService.delete(iotScadaIdParamList);
        return CommonResult.ok();
    }

    /**
     * 获取组态详情
     */
    @Operation(summary = "获取组态详情")
    @SaCheckPermission("/iot/scada/detail")
    @GetMapping("/iot/scada/detail")
    public CommonResult<IotScada> detail(@Valid IotScadaIdParam iotScadaIdParam) {
        return CommonResult.data(iotScadaService.detail(iotScadaIdParam));
    }
}
