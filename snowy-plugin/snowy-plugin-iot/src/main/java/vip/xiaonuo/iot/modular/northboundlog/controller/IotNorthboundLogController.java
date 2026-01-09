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
package vip.xiaonuo.iot.modular.northboundlog.controller;

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
import vip.xiaonuo.iot.modular.northboundlog.entity.IotNorthboundLog;
import vip.xiaonuo.iot.modular.northboundlog.param.IotNorthboundLogAddParam;
import vip.xiaonuo.iot.modular.northboundlog.param.IotNorthboundLogEditParam;
import vip.xiaonuo.iot.modular.northboundlog.param.IotNorthboundLogIdParam;
import vip.xiaonuo.iot.modular.northboundlog.param.IotNorthboundLogPageParam;
import vip.xiaonuo.iot.modular.northboundlog.service.IotNorthboundLogService;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.io.IOException;
import java.util.List;

/**
 * 北向推送日志表控制器
 *
 * @author jetox
 * @date  2026/01/08 10:25
 */
@Tag(name = "北向推送日志表控制器")
@RestController
@Validated
public class IotNorthboundLogController {

    @Resource
    private IotNorthboundLogService iotNorthboundLogService;

    /**
     * 获取北向推送日志表分页
     *
     * @author jetox
     * @date  2026/01/08 10:25
     */
    @Operation(summary = "获取北向推送日志表分页")
    @SaCheckPermission("/iot/northboundlog/page")
    @GetMapping("/iot/northboundlog/page")
    public CommonResult<Page<IotNorthboundLog>> page(IotNorthboundLogPageParam iotNorthboundLogPageParam) {
        return CommonResult.data(iotNorthboundLogService.page(iotNorthboundLogPageParam));
    }

    /**
     * 添加北向推送日志表
     *
     * @author jetox
     * @date  2026/01/08 10:25
     */
    @Operation(summary = "添加北向推送日志表")
    @CommonLog("添加北向推送日志表")
    @SaCheckPermission("/iot/northboundlog/add")
    @PostMapping("/iot/northboundlog/add")
    public CommonResult<String> add(@RequestBody @Valid IotNorthboundLogAddParam iotNorthboundLogAddParam) {
        iotNorthboundLogService.add(iotNorthboundLogAddParam);
        return CommonResult.ok();
    }

    /**
     * 编辑北向推送日志表
     *
     * @author jetox
     * @date  2026/01/08 10:25
     */
    @Operation(summary = "编辑北向推送日志表")
    @CommonLog("编辑北向推送日志表")
    @SaCheckPermission("/iot/northboundlog/edit")
    @PostMapping("/iot/northboundlog/edit")
    public CommonResult<String> edit(@RequestBody @Valid IotNorthboundLogEditParam iotNorthboundLogEditParam) {
        iotNorthboundLogService.edit(iotNorthboundLogEditParam);
        return CommonResult.ok();
    }

    /**
     * 删除北向推送日志表
     *
     * @author jetox
     * @date  2026/01/08 10:25
     */
    @Operation(summary = "删除北向推送日志表")
    @CommonLog("删除北向推送日志表")
    @SaCheckPermission("/iot/northboundlog/delete")
    @PostMapping("/iot/northboundlog/delete")
    public CommonResult<String> delete(@RequestBody @Valid @NotEmpty(message = "集合不能为空")
                                                   List<IotNorthboundLogIdParam> iotNorthboundLogIdParamList) {
        iotNorthboundLogService.delete(iotNorthboundLogIdParamList);
        return CommonResult.ok();
    }

    /**
     * 获取北向推送日志表详情
     *
     * @author jetox
     * @date  2026/01/08 10:25
     */
    @Operation(summary = "获取北向推送日志表详情")
    @SaCheckPermission("/iot/northboundlog/detail")
    @GetMapping("/iot/northboundlog/detail")
    public CommonResult<IotNorthboundLog> detail(@Valid IotNorthboundLogIdParam iotNorthboundLogIdParam) {
        return CommonResult.data(iotNorthboundLogService.detail(iotNorthboundLogIdParam));
    }

    /**
     * 下载北向推送日志表导入模板
     *
     * @author jetox
     * @date  2026/01/08 10:25
     */
    @Operation(summary = "下载北向推送日志表导入模板")
    @GetMapping(value = "/iot/northboundlog/downloadImportTemplate", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
        iotNorthboundLogService.downloadImportTemplate(response);
    }

    /**
     * 导入北向推送日志表
     *
     * @author jetox
     * @date  2026/01/08 10:25
     */
    @Operation(summary = "导入北向推送日志表")
    @CommonLog("导入北向推送日志表")
    @SaCheckPermission("/iot/northboundlog/importData")
    @PostMapping("/iot/northboundlog/importData")
    public CommonResult<JSONObject> importData(@RequestPart("file") MultipartFile file) {
        return CommonResult.data(iotNorthboundLogService.importData(file));
    }

    /**
     * 导出北向推送日志表
     *
     * @author jetox
     * @date  2026/01/08 10:25
     */
    @Operation(summary = "导出北向推送日志表")
    @SaCheckPermission("/iot/northboundlog/exportData")
    @PostMapping(value = "/iot/northboundlog/exportData", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void exportData(@RequestBody List<IotNorthboundLogIdParam> iotNorthboundLogIdParamList, HttpServletResponse response) throws IOException {
        iotNorthboundLogService.exportData(iotNorthboundLogIdParamList, response);
    }
}
