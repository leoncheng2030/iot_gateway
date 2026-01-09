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
package vip.xiaonuo.iot.modular.devicegrouprel.controller;

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
import vip.xiaonuo.iot.modular.devicegrouprel.entity.IotDeviceGroupRel;
import vip.xiaonuo.iot.modular.devicegrouprel.param.IotDeviceGroupRelAddParam;
import vip.xiaonuo.iot.modular.devicegrouprel.param.IotDeviceGroupRelEditParam;
import vip.xiaonuo.iot.modular.devicegrouprel.param.IotDeviceGroupRelIdParam;
import vip.xiaonuo.iot.modular.devicegrouprel.param.IotDeviceGroupRelPageParam;
import vip.xiaonuo.iot.modular.devicegrouprel.service.IotDeviceGroupRelService;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.io.IOException;
import java.util.List;

/**
 * 设备分组关联表控制器
 *
 * @author jetox
 * @date  2025/12/13 18:31
 */
@Tag(name = "设备分组关联表控制器")
@RestController
@Validated
public class IotDeviceGroupRelController {

    @Resource
    private IotDeviceGroupRelService iotDeviceGroupRelService;

    /**
     * 获取设备分组关联表分页
     *
     * @author jetox
     * @date  2025/12/13 18:31
     */
    @Operation(summary = "获取设备分组关联表分页")
    @SaCheckPermission("/iot/devicegrouprel/page")
    @GetMapping("/iot/devicegrouprel/page")
    public CommonResult<Page<IotDeviceGroupRel>> page(IotDeviceGroupRelPageParam iotDeviceGroupRelPageParam) {
        return CommonResult.data(iotDeviceGroupRelService.page(iotDeviceGroupRelPageParam));
    }

    /**
     * 添加设备分组关联表
     *
     * @author jetox
     * @date  2025/12/13 18:31
     */
    @Operation(summary = "添加设备分组关联表")
    @CommonLog("添加设备分组关联表")
    @SaCheckPermission("/iot/devicegrouprel/add")
    @PostMapping("/iot/devicegrouprel/add")
    public CommonResult<String> add(@RequestBody @Valid IotDeviceGroupRelAddParam iotDeviceGroupRelAddParam) {
        iotDeviceGroupRelService.add(iotDeviceGroupRelAddParam);
        return CommonResult.ok();
    }

    /**
     * 编辑设备分组关联表
     *
     * @author jetox
     * @date  2025/12/13 18:31
     */
    @Operation(summary = "编辑设备分组关联表")
    @CommonLog("编辑设备分组关联表")
    @SaCheckPermission("/iot/devicegrouprel/edit")
    @PostMapping("/iot/devicegrouprel/edit")
    public CommonResult<String> edit(@RequestBody @Valid IotDeviceGroupRelEditParam iotDeviceGroupRelEditParam) {
        iotDeviceGroupRelService.edit(iotDeviceGroupRelEditParam);
        return CommonResult.ok();
    }

    /**
     * 删除设备分组关联表
     *
     * @author jetox
     * @date  2025/12/13 18:31
     */
    @Operation(summary = "删除设备分组关联表")
    @CommonLog("删除设备分组关联表")
    @SaCheckPermission("/iot/devicegrouprel/delete")
    @PostMapping("/iot/devicegrouprel/delete")
    public CommonResult<String> delete(@RequestBody @Valid @NotEmpty(message = "集合不能为空")
                                                   List<IotDeviceGroupRelIdParam> iotDeviceGroupRelIdParamList) {
        iotDeviceGroupRelService.delete(iotDeviceGroupRelIdParamList);
        return CommonResult.ok();
    }

    /**
     * 获取设备分组关联表详情
     *
     * @author jetox
     * @date  2025/12/13 18:31
     */
    @Operation(summary = "获取设备分组关联表详情")
    @SaCheckPermission("/iot/devicegrouprel/detail")
    @GetMapping("/iot/devicegrouprel/detail")
    public CommonResult<IotDeviceGroupRel> detail(@Valid IotDeviceGroupRelIdParam iotDeviceGroupRelIdParam) {
        return CommonResult.data(iotDeviceGroupRelService.detail(iotDeviceGroupRelIdParam));
    }

    /**
     * 下载设备分组关联表导入模板
     *
     * @author jetox
     * @date  2025/12/13 18:31
     */
    @Operation(summary = "下载设备分组关联表导入模板")
    @GetMapping(value = "/iot/devicegrouprel/downloadImportTemplate", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
        iotDeviceGroupRelService.downloadImportTemplate(response);
    }

    /**
     * 导入设备分组关联表
     *
     * @author jetox
     * @date  2025/12/13 18:31
     */
    @Operation(summary = "导入设备分组关联表")
    @CommonLog("导入设备分组关联表")
    @SaCheckPermission("/iot/devicegrouprel/importData")
    @PostMapping("/iot/devicegrouprel/importData")
    public CommonResult<JSONObject> importData(@RequestPart("file") MultipartFile file) {
        return CommonResult.data(iotDeviceGroupRelService.importData(file));
    }

    /**
     * 导出设备分组关联表
     *
     * @author jetox
     * @date  2025/12/13 18:31
     */
    @Operation(summary = "导出设备分组关联表")
    @SaCheckPermission("/iot/devicegrouprel/exportData")
    @PostMapping(value = "/iot/devicegrouprel/exportData", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void exportData(@RequestBody List<IotDeviceGroupRelIdParam> iotDeviceGroupRelIdParamList, HttpServletResponse response) throws IOException {
        iotDeviceGroupRelService.exportData(iotDeviceGroupRelIdParamList, response);
    }
}
