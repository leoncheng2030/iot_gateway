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
package vip.xiaonuo.iot.modular.northbounddevicerel.controller;

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
import vip.xiaonuo.iot.modular.northbounddevicerel.entity.IotNorthboundDeviceRel;
import vip.xiaonuo.iot.modular.northbounddevicerel.param.IotNorthboundDeviceRelAddParam;
import vip.xiaonuo.iot.modular.northbounddevicerel.param.IotNorthboundDeviceRelEditParam;
import vip.xiaonuo.iot.modular.northbounddevicerel.param.IotNorthboundDeviceRelIdParam;
import vip.xiaonuo.iot.modular.northbounddevicerel.param.IotNorthboundDeviceRelPageParam;
import vip.xiaonuo.iot.modular.northbounddevicerel.service.IotNorthboundDeviceRelService;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.io.IOException;
import java.util.List;

/**
 * 北向推送设备关联表控制器
 *
 * @author jetox
 * @date  2026/01/08 10:25
 */
@Tag(name = "北向推送设备关联表控制器")
@RestController
@Validated
public class IotNorthboundDeviceRelController {

    @Resource
    private IotNorthboundDeviceRelService iotNorthboundDeviceRelService;

    /**
     * 获取北向推送设备关联表分页
     *
     * @author jetox
     * @date  2026/01/08 10:25
     */
    @Operation(summary = "获取北向推送设备关联表分页")
    @SaCheckPermission("/iot/northbounddevicerel/page")
    @GetMapping("/iot/northbounddevicerel/page")
    public CommonResult<Page<IotNorthboundDeviceRel>> page(IotNorthboundDeviceRelPageParam iotNorthboundDeviceRelPageParam) {
        return CommonResult.data(iotNorthboundDeviceRelService.page(iotNorthboundDeviceRelPageParam));
    }

    /**
     * 添加北向推送设备关联表
     *
     * @author jetox
     * @date  2026/01/08 10:25
     */
    @Operation(summary = "添加北向推送设备关联表")
    @CommonLog("添加北向推送设备关联表")
    @SaCheckPermission("/iot/northbounddevicerel/add")
    @PostMapping("/iot/northbounddevicerel/add")
    public CommonResult<String> add(@RequestBody @Valid IotNorthboundDeviceRelAddParam iotNorthboundDeviceRelAddParam) {
        iotNorthboundDeviceRelService.add(iotNorthboundDeviceRelAddParam);
        return CommonResult.ok();
    }

    /**
     * 编辑北向推送设备关联表
     *
     * @author jetox
     * @date  2026/01/08 10:25
     */
    @Operation(summary = "编辑北向推送设备关联表")
    @CommonLog("编辑北向推送设备关联表")
    @SaCheckPermission("/iot/northbounddevicerel/edit")
    @PostMapping("/iot/northbounddevicerel/edit")
    public CommonResult<String> edit(@RequestBody @Valid IotNorthboundDeviceRelEditParam iotNorthboundDeviceRelEditParam) {
        iotNorthboundDeviceRelService.edit(iotNorthboundDeviceRelEditParam);
        return CommonResult.ok();
    }

    /**
     * 删除北向推送设备关联表
     *
     * @author jetox
     * @date  2026/01/08 10:25
     */
    @Operation(summary = "删除北向推送设备关联表")
    @CommonLog("删除北向推送设备关联表")
    @SaCheckPermission("/iot/northbounddevicerel/delete")
    @PostMapping("/iot/northbounddevicerel/delete")
    public CommonResult<String> delete(@RequestBody @Valid @NotEmpty(message = "集合不能为空")
                                                   List<IotNorthboundDeviceRelIdParam> iotNorthboundDeviceRelIdParamList) {
        iotNorthboundDeviceRelService.delete(iotNorthboundDeviceRelIdParamList);
        return CommonResult.ok();
    }

    /**
     * 获取北向推送设备关联表详情
     *
     * @author jetox
     * @date  2026/01/08 10:25
     */
    @Operation(summary = "获取北向推送设备关联表详情")
    @SaCheckPermission("/iot/northbounddevicerel/detail")
    @GetMapping("/iot/northbounddevicerel/detail")
    public CommonResult<IotNorthboundDeviceRel> detail(@Valid IotNorthboundDeviceRelIdParam iotNorthboundDeviceRelIdParam) {
        return CommonResult.data(iotNorthboundDeviceRelService.detail(iotNorthboundDeviceRelIdParam));
    }

    /**
     * 下载北向推送设备关联表导入模板
     *
     * @author jetox
     * @date  2026/01/08 10:25
     */
    @Operation(summary = "下载北向推送设备关联表导入模板")
    @GetMapping(value = "/iot/northbounddevicerel/downloadImportTemplate", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
        iotNorthboundDeviceRelService.downloadImportTemplate(response);
    }

    /**
     * 导入北向推送设备关联表
     *
     * @author jetox
     * @date  2026/01/08 10:25
     */
    @Operation(summary = "导入北向推送设备关联表")
    @CommonLog("导入北向推送设备关联表")
    @SaCheckPermission("/iot/northbounddevicerel/importData")
    @PostMapping("/iot/northbounddevicerel/importData")
    public CommonResult<JSONObject> importData(@RequestPart("file") MultipartFile file) {
        return CommonResult.data(iotNorthboundDeviceRelService.importData(file));
    }

    /**
     * 导出北向推送设备关联表
     *
     * @author jetox
     * @date  2026/01/08 10:25
     */
    @Operation(summary = "导出北向推送设备关联表")
    @SaCheckPermission("/iot/northbounddevicerel/exportData")
    @PostMapping(value = "/iot/northbounddevicerel/exportData", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void exportData(@RequestBody List<IotNorthboundDeviceRelIdParam> iotNorthboundDeviceRelIdParamList, HttpServletResponse response) throws IOException {
        iotNorthboundDeviceRelService.exportData(iotNorthboundDeviceRelIdParamList, response);
    }

    /**
     * 设备绑定推送配置
     *
     * @author jetox
     * @date  2026/01/08
     */
    @Operation(summary = "设备绑定推送配置")
    @CommonLog("设备绑定推送配置")
    @PostMapping("/iot/northbounddevicerel/bind")
    public CommonResult<String> bind(@RequestBody JSONObject params) {
        iotNorthboundDeviceRelService.bind(params);
        return CommonResult.ok();
    }
}
