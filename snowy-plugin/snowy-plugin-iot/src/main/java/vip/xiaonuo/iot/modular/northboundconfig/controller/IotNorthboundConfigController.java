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
package vip.xiaonuo.iot.modular.northboundconfig.controller;

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
import vip.xiaonuo.iot.modular.northboundconfig.entity.IotNorthboundConfig;
import vip.xiaonuo.iot.modular.northboundconfig.param.IotNorthboundConfigAddParam;
import vip.xiaonuo.iot.modular.northboundconfig.param.IotNorthboundConfigEditParam;
import vip.xiaonuo.iot.modular.northboundconfig.param.IotNorthboundConfigIdParam;
import vip.xiaonuo.iot.modular.northboundconfig.param.IotNorthboundConfigPageParam;
import vip.xiaonuo.iot.modular.northboundconfig.service.IotNorthboundConfigService;
import vip.xiaonuo.iot.modular.northbound.service.NorthboundPushService;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.io.IOException;
import java.util.List;

/**
 * 北向推送配置表控制器
 *
 * @author jetox
 * @date  2026/01/08 10:20
 */
@Tag(name = "北向推送配置表控制器")
@RestController
@Validated
public class IotNorthboundConfigController {

    @Resource
    private IotNorthboundConfigService iotNorthboundConfigService;

    @Resource
    private NorthboundPushService northboundPushService;

    /**
     * 获取北向推送配置表分页
     *
     * @author jetox
     * @date  2026/01/08 10:20
     */
    @Operation(summary = "获取北向推送配置表分页")
    @SaCheckPermission("/iot/northboundconfig/page")
    @GetMapping("/iot/northboundconfig/page")
    public CommonResult<Page<IotNorthboundConfig>> page(IotNorthboundConfigPageParam iotNorthboundConfigPageParam) {
        return CommonResult.data(iotNorthboundConfigService.page(iotNorthboundConfigPageParam));
    }

    /**
     * 添加北向推送配置表
     *
     * @author jetox
     * @date  2026/01/08 10:20
     */
    @Operation(summary = "添加北向推送配置表")
    @CommonLog("添加北向推送配置表")
    @SaCheckPermission("/iot/northboundconfig/add")
    @PostMapping("/iot/northboundconfig/add")
    public CommonResult<String> add(@RequestBody @Valid IotNorthboundConfigAddParam iotNorthboundConfigAddParam) {
        iotNorthboundConfigService.add(iotNorthboundConfigAddParam);
        return CommonResult.ok();
    }

    /**
     * 编辑北向推送配置表
     *
     * @author jetox
     * @date  2026/01/08 10:20
     */
    @Operation(summary = "编辑北向推送配置表")
    @CommonLog("编辑北向推送配置表")
    @SaCheckPermission("/iot/northboundconfig/edit")
    @PostMapping("/iot/northboundconfig/edit")
    public CommonResult<String> edit(@RequestBody @Valid IotNorthboundConfigEditParam iotNorthboundConfigEditParam) {
        iotNorthboundConfigService.edit(iotNorthboundConfigEditParam);
        return CommonResult.ok();
    }

    /**
     * 删除北向推送配置表
     *
     * @author jetox
     * @date  2026/01/08 10:20
     */
    @Operation(summary = "删除北向推送配置表")
    @CommonLog("删除北向推送配置表")
    @SaCheckPermission("/iot/northboundconfig/delete")
    @PostMapping("/iot/northboundconfig/delete")
    public CommonResult<String> delete(@RequestBody @Valid @NotEmpty(message = "集合不能为空")
                                                   List<IotNorthboundConfigIdParam> iotNorthboundConfigIdParamList) {
        iotNorthboundConfigService.delete(iotNorthboundConfigIdParamList);
        return CommonResult.ok();
    }

    /**
     * 获取北向推送配置表详情
     *
     * @author jetox
     * @date  2026/01/08 10:20
     */
    @Operation(summary = "获取北向推送配置表详情")
    @SaCheckPermission("/iot/northboundconfig/detail")
    @GetMapping("/iot/northboundconfig/detail")
    public CommonResult<IotNorthboundConfig> detail(@Valid IotNorthboundConfigIdParam iotNorthboundConfigIdParam) {
        return CommonResult.data(iotNorthboundConfigService.detail(iotNorthboundConfigIdParam));
    }

    /**
     * 下载北向推送配置表导入模板
     *
     * @author jetox
     * @date  2026/01/08 10:20
     */
    @Operation(summary = "下载北向推送配置表导入模板")
    @GetMapping(value = "/iot/northboundconfig/downloadImportTemplate", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
        iotNorthboundConfigService.downloadImportTemplate(response);
    }

    /**
     * 导入北向推送配置表
     *
     * @author jetox
     * @date  2026/01/08 10:20
     */
    @Operation(summary = "导入北向推送配置表")
    @CommonLog("导入北向推送配置表")
    @SaCheckPermission("/iot/northboundconfig/importData")
    @PostMapping("/iot/northboundconfig/importData")
    public CommonResult<JSONObject> importData(@RequestPart("file") MultipartFile file) {
        return CommonResult.data(iotNorthboundConfigService.importData(file));
    }

    /**
     * 导出北向推送配置表
     *
     * @author jetox
     * @date  2026/01/08 10:20
     */
    @Operation(summary = "导出北向推送配置表")
    @SaCheckPermission("/iot/northboundconfig/exportData")
    @PostMapping(value = "/iot/northboundconfig/exportData", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void exportData(@RequestBody List<IotNorthboundConfigIdParam> iotNorthboundConfigIdParamList, HttpServletResponse response) throws IOException {
        iotNorthboundConfigService.exportData(iotNorthboundConfigIdParamList, response);
    }

    /**
     * 测试北向推送连接
     *
     * @author jetox
     * @date  2026/01/08 10:30
     */
    @Operation(summary = "测试北向推送连接")
    @CommonLog("测试北向推送连接")
    @SaCheckPermission("/iot/northboundconfig/testConnection")
    @GetMapping("/iot/northboundconfig/testConnection")
    public CommonResult<JSONObject> testConnection(@Valid IotNorthboundConfigIdParam iotNorthboundConfigIdParam) {
        return CommonResult.data(northboundPushService.testConnection(iotNorthboundConfigIdParam.getId()));
    }

    /**
     * 启用/禁用北向推送配置
     *
     * @author jetox
     * @date  2026/01/08 10:30
     */
    @Operation(summary = "启用/禁用北向推送配置")
    @CommonLog("启用/禁用北向推送配置")
    @SaCheckPermission("/iot/northboundconfig/toggleEnable")
    @PostMapping("/iot/northboundconfig/toggleEnable")
    public CommonResult<String> toggleEnable(@RequestBody @Valid IotNorthboundConfigIdParam iotNorthboundConfigIdParam) {
        IotNorthboundConfig config = iotNorthboundConfigService.getById(iotNorthboundConfigIdParam.getId());
        if (config != null) {
            // 切换状态
            config.setEnabled("ENABLE".equals(config.getEnabled()) ? "DISABLE" : "ENABLE");
            iotNorthboundConfigService.updateById(config);
        }
        return CommonResult.ok();
    }
}
