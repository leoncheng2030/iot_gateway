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
package vip.xiaonuo.iot.modular.deviceshadow.controller;

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
import vip.xiaonuo.iot.modular.deviceshadow.entity.IotDeviceShadow;
import vip.xiaonuo.iot.modular.deviceshadow.param.IotDeviceShadowAddParam;
import vip.xiaonuo.iot.modular.deviceshadow.param.IotDeviceShadowEditParam;
import vip.xiaonuo.iot.modular.deviceshadow.param.IotDeviceShadowIdParam;
import vip.xiaonuo.iot.modular.deviceshadow.param.IotDeviceShadowPageParam;
import vip.xiaonuo.iot.modular.deviceshadow.service.IotDeviceShadowService;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.io.IOException;
import java.util.List;

/**
 * 设备影子表控制器
 *
 * @author jetox
 * @date  2025/12/11 07:28
 */
@Tag(name = "设备影子表控制器")
@RestController
@Validated
public class IotDeviceShadowController {

    @Resource
    private IotDeviceShadowService iotDeviceShadowService;

    /**
     * 获取设备影子表分页
     *
     * @author jetox
     * @date  2025/12/11 07:28
     */
    @Operation(summary = "获取设备影子表分页")
    @SaCheckPermission("/iot/deviceshadow/page")
    @GetMapping("/iot/deviceshadow/page")
    public CommonResult<Page<IotDeviceShadow>> page(IotDeviceShadowPageParam iotDeviceShadowPageParam) {
        return CommonResult.data(iotDeviceShadowService.page(iotDeviceShadowPageParam));
    }

    /**
     * 添加设备影子表
     *
     * @author jetox
     * @date  2025/12/11 07:28
     */
    @Operation(summary = "添加设备影子表")
    @CommonLog("添加设备影子表")
    @SaCheckPermission("/iot/deviceshadow/add")
    @PostMapping("/iot/deviceshadow/add")
    public CommonResult<String> add(@RequestBody @Valid IotDeviceShadowAddParam iotDeviceShadowAddParam) {
        iotDeviceShadowService.add(iotDeviceShadowAddParam);
        return CommonResult.ok();
    }

    /**
     * 编辑设备影子表
     *
     * @author jetox
     * @date  2025/12/11 07:28
     */
    @Operation(summary = "编辑设备影子表")
    @CommonLog("编辑设备影子表")
    @SaCheckPermission("/iot/deviceshadow/edit")
    @PostMapping("/iot/deviceshadow/edit")
    public CommonResult<String> edit(@RequestBody @Valid IotDeviceShadowEditParam iotDeviceShadowEditParam) {
        iotDeviceShadowService.edit(iotDeviceShadowEditParam);
        return CommonResult.ok();
    }

    /**
     * 删除设备影子表
     *
     * @author jetox
     * @date  2025/12/11 07:28
     */
    @Operation(summary = "删除设备影子表")
    @CommonLog("删除设备影子表")
    @SaCheckPermission("/iot/deviceshadow/delete")
    @PostMapping("/iot/deviceshadow/delete")
    public CommonResult<String> delete(@RequestBody @Valid @NotEmpty(message = "集合不能为空")
                                                   List<IotDeviceShadowIdParam> iotDeviceShadowIdParamList) {
        iotDeviceShadowService.delete(iotDeviceShadowIdParamList);
        return CommonResult.ok();
    }

    /**
     * 获取设备影子表详情
     *
     * @author jetox
     * @date  2025/12/11 07:28
     */
    @Operation(summary = "获取设备影子表详情")
    @SaCheckPermission("/iot/deviceshadow/detail")
    @GetMapping("/iot/deviceshadow/detail")
    public CommonResult<IotDeviceShadow> detail(@Valid IotDeviceShadowIdParam iotDeviceShadowIdParam) {
        return CommonResult.data(iotDeviceShadowService.detail(iotDeviceShadowIdParam));
    }

    /**
     * 下载设备影子表导入模板
     *
     * @author jetox
     * @date  2025/12/11 07:28
     */
    @Operation(summary = "下载设备影子表导入模板")
    @GetMapping(value = "/iot/deviceshadow/downloadImportTemplate", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
        iotDeviceShadowService.downloadImportTemplate(response);
    }

    /**
     * 导入设备影子表
     *
     * @author jetox
     * @date  2025/12/11 07:28
     */
    @Operation(summary = "导入设备影子表")
    @CommonLog("导入设备影子表")
    @SaCheckPermission("/iot/deviceshadow/importData")
    @PostMapping("/iot/deviceshadow/importData")
    public CommonResult<JSONObject> importData(@RequestPart("file") MultipartFile file) {
        return CommonResult.data(iotDeviceShadowService.importData(file));
    }

    /**
     * 导出设备影子表
     *
     * @author jetox
     * @date  2025/12/11 07:28
     */
    @Operation(summary = "导出设备影子表")
    @SaCheckPermission("/iot/deviceshadow/exportData")
    @PostMapping(value = "/iot/deviceshadow/exportData", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void exportData(@RequestBody List<IotDeviceShadowIdParam> iotDeviceShadowIdParamList, HttpServletResponse response) throws IOException {
        iotDeviceShadowService.exportData(iotDeviceShadowIdParamList, response);
    }
}
