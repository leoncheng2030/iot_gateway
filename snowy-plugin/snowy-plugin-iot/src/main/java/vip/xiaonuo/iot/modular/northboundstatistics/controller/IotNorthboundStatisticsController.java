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
package vip.xiaonuo.iot.modular.northboundstatistics.controller;

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
import vip.xiaonuo.iot.modular.northboundstatistics.entity.IotNorthboundStatistics;
import vip.xiaonuo.iot.modular.northboundstatistics.param.IotNorthboundStatisticsAddParam;
import vip.xiaonuo.iot.modular.northboundstatistics.param.IotNorthboundStatisticsEditParam;
import vip.xiaonuo.iot.modular.northboundstatistics.param.IotNorthboundStatisticsIdParam;
import vip.xiaonuo.iot.modular.northboundstatistics.param.IotNorthboundStatisticsPageParam;
import vip.xiaonuo.iot.modular.northboundstatistics.service.IotNorthboundStatisticsService;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.io.IOException;
import java.util.List;

/**
 * 北向推送统计表控制器
 *
 * @author jetox
 * @date  2026/01/08 10:26
 */
@Tag(name = "北向推送统计表控制器")
@RestController
@Validated
public class IotNorthboundStatisticsController {

    @Resource
    private IotNorthboundStatisticsService iotNorthboundStatisticsService;

    /**
     * 获取北向推送统计表分页
     *
     * @author jetox
     * @date  2026/01/08 10:26
     */
    @Operation(summary = "获取北向推送统计表分页")
    @SaCheckPermission("/iot/northboundstatistics/page")
    @GetMapping("/iot/northboundstatistics/page")
    public CommonResult<Page<IotNorthboundStatistics>> page(IotNorthboundStatisticsPageParam iotNorthboundStatisticsPageParam) {
        return CommonResult.data(iotNorthboundStatisticsService.page(iotNorthboundStatisticsPageParam));
    }

    /**
     * 添加北向推送统计表
     *
     * @author jetox
     * @date  2026/01/08 10:26
     */
    @Operation(summary = "添加北向推送统计表")
    @CommonLog("添加北向推送统计表")
    @SaCheckPermission("/iot/northboundstatistics/add")
    @PostMapping("/iot/northboundstatistics/add")
    public CommonResult<String> add(@RequestBody @Valid IotNorthboundStatisticsAddParam iotNorthboundStatisticsAddParam) {
        iotNorthboundStatisticsService.add(iotNorthboundStatisticsAddParam);
        return CommonResult.ok();
    }

    /**
     * 编辑北向推送统计表
     *
     * @author jetox
     * @date  2026/01/08 10:26
     */
    @Operation(summary = "编辑北向推送统计表")
    @CommonLog("编辑北向推送统计表")
    @SaCheckPermission("/iot/northboundstatistics/edit")
    @PostMapping("/iot/northboundstatistics/edit")
    public CommonResult<String> edit(@RequestBody @Valid IotNorthboundStatisticsEditParam iotNorthboundStatisticsEditParam) {
        iotNorthboundStatisticsService.edit(iotNorthboundStatisticsEditParam);
        return CommonResult.ok();
    }

    /**
     * 删除北向推送统计表
     *
     * @author jetox
     * @date  2026/01/08 10:26
     */
    @Operation(summary = "删除北向推送统计表")
    @CommonLog("删除北向推送统计表")
    @SaCheckPermission("/iot/northboundstatistics/delete")
    @PostMapping("/iot/northboundstatistics/delete")
    public CommonResult<String> delete(@RequestBody @Valid @NotEmpty(message = "集合不能为空")
                                                   List<IotNorthboundStatisticsIdParam> iotNorthboundStatisticsIdParamList) {
        iotNorthboundStatisticsService.delete(iotNorthboundStatisticsIdParamList);
        return CommonResult.ok();
    }

    /**
     * 获取北向推送统计表详情
     *
     * @author jetox
     * @date  2026/01/08 10:26
     */
    @Operation(summary = "获取北向推送统计表详情")
    @SaCheckPermission("/iot/northboundstatistics/detail")
    @GetMapping("/iot/northboundstatistics/detail")
    public CommonResult<IotNorthboundStatistics> detail(@Valid IotNorthboundStatisticsIdParam iotNorthboundStatisticsIdParam) {
        return CommonResult.data(iotNorthboundStatisticsService.detail(iotNorthboundStatisticsIdParam));
    }

    /**
     * 下载北向推送统计表导入模板
     *
     * @author jetox
     * @date  2026/01/08 10:26
     */
    @Operation(summary = "下载北向推送统计表导入模板")
    @GetMapping(value = "/iot/northboundstatistics/downloadImportTemplate", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
        iotNorthboundStatisticsService.downloadImportTemplate(response);
    }

    /**
     * 导入北向推送统计表
     *
     * @author jetox
     * @date  2026/01/08 10:26
     */
    @Operation(summary = "导入北向推送统计表")
    @CommonLog("导入北向推送统计表")
    @SaCheckPermission("/iot/northboundstatistics/importData")
    @PostMapping("/iot/northboundstatistics/importData")
    public CommonResult<JSONObject> importData(@RequestPart("file") MultipartFile file) {
        return CommonResult.data(iotNorthboundStatisticsService.importData(file));
    }

    /**
     * 导出北向推送统计表
     *
     * @author jetox
     * @date  2026/01/08 10:26
     */
    @Operation(summary = "导出北向推送统计表")
    @SaCheckPermission("/iot/northboundstatistics/exportData")
    @PostMapping(value = "/iot/northboundstatistics/exportData", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void exportData(@RequestBody List<IotNorthboundStatisticsIdParam> iotNorthboundStatisticsIdParamList, HttpServletResponse response) throws IOException {
        iotNorthboundStatisticsService.exportData(iotNorthboundStatisticsIdParamList, response);
    }
}
