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
package vip.xiaonuo.iot.modular.rulelog.controller;

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
import vip.xiaonuo.iot.modular.rulelog.entity.IotRuleLog;
import vip.xiaonuo.iot.modular.rulelog.param.IotRuleLogAddParam;
import vip.xiaonuo.iot.modular.rulelog.param.IotRuleLogEditParam;
import vip.xiaonuo.iot.modular.rulelog.param.IotRuleLogIdParam;
import vip.xiaonuo.iot.modular.rulelog.param.IotRuleLogPageParam;
import vip.xiaonuo.iot.modular.rulelog.service.IotRuleLogService;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.io.IOException;
import java.util.List;

/**
 * 规则执行日志表控制器
 *
 * @author jetox
 * @date  2025/12/11 07:39
 */
@Tag(name = "规则执行日志表控制器")
@RestController
@Validated
public class IotRuleLogController {

    @Resource
    private IotRuleLogService iotRuleLogService;

    /**
     * 获取规则执行日志表分页
     *
     * @author jetox
     * @date  2025/12/11 07:39
     */
    @Operation(summary = "获取规则执行日志表分页")
    @SaCheckPermission("/iot/rulelog/page")
    @GetMapping("/iot/rulelog/page")
    public CommonResult<Page<IotRuleLog>> page(IotRuleLogPageParam iotRuleLogPageParam) {
        return CommonResult.data(iotRuleLogService.page(iotRuleLogPageParam));
    }

    /**
     * 添加规则执行日志表
     *
     * @author jetox
     * @date  2025/12/11 07:39
     */
    @Operation(summary = "添加规则执行日志表")
    @CommonLog("添加规则执行日志表")
    @SaCheckPermission("/iot/rulelog/add")
    @PostMapping("/iot/rulelog/add")
    public CommonResult<String> add(@RequestBody @Valid IotRuleLogAddParam iotRuleLogAddParam) {
        iotRuleLogService.add(iotRuleLogAddParam);
        return CommonResult.ok();
    }

    /**
     * 编辑规则执行日志表
     *
     * @author jetox
     * @date  2025/12/11 07:39
     */
    @Operation(summary = "编辑规则执行日志表")
    @CommonLog("编辑规则执行日志表")
    @SaCheckPermission("/iot/rulelog/edit")
    @PostMapping("/iot/rulelog/edit")
    public CommonResult<String> edit(@RequestBody @Valid IotRuleLogEditParam iotRuleLogEditParam) {
        iotRuleLogService.edit(iotRuleLogEditParam);
        return CommonResult.ok();
    }

    /**
     * 删除规则执行日志表
     *
     * @author jetox
     * @date  2025/12/11 07:39
     */
    @Operation(summary = "删除规则执行日志表")
    @CommonLog("删除规则执行日志表")
    @SaCheckPermission("/iot/rulelog/delete")
    @PostMapping("/iot/rulelog/delete")
    public CommonResult<String> delete(@RequestBody @Valid @NotEmpty(message = "集合不能为空")
                                                   List<IotRuleLogIdParam> iotRuleLogIdParamList) {
        iotRuleLogService.delete(iotRuleLogIdParamList);
        return CommonResult.ok();
    }

    /**
     * 获取规则执行日志表详情
     *
     * @author jetox
     * @date  2025/12/11 07:39
     */
    @Operation(summary = "获取规则执行日志表详情")
    @SaCheckPermission("/iot/rulelog/detail")
    @GetMapping("/iot/rulelog/detail")
    public CommonResult<IotRuleLog> detail(@Valid IotRuleLogIdParam iotRuleLogIdParam) {
        return CommonResult.data(iotRuleLogService.detail(iotRuleLogIdParam));
    }

    /**
     * 下载规则执行日志表导入模板
     *
     * @author jetox
     * @date  2025/12/11 07:39
     */
    @Operation(summary = "下载规则执行日志表导入模板")
    @GetMapping(value = "/iot/rulelog/downloadImportTemplate", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
        iotRuleLogService.downloadImportTemplate(response);
    }

    /**
     * 导入规则执行日志表
     *
     * @author jetox
     * @date  2025/12/11 07:39
     */
    @Operation(summary = "导入规则执行日志表")
    @CommonLog("导入规则执行日志表")
    @SaCheckPermission("/iot/rulelog/importData")
    @PostMapping("/iot/rulelog/importData")
    public CommonResult<JSONObject> importData(@RequestPart("file") MultipartFile file) {
        return CommonResult.data(iotRuleLogService.importData(file));
    }

    /**
     * 导出规则执行日志表
     *
     * @author jetox
     * @date  2025/12/11 07:39
     */
    @Operation(summary = "导出规则执行日志表")
    @SaCheckPermission("/iot/rulelog/exportData")
    @PostMapping(value = "/iot/rulelog/exportData", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void exportData(@RequestBody List<IotRuleLogIdParam> iotRuleLogIdParamList, HttpServletResponse response) throws IOException {
        iotRuleLogService.exportData(iotRuleLogIdParamList, response);
    }
}
