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
package vip.xiaonuo.iot.modular.driverlog.controller;

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
import vip.xiaonuo.iot.modular.driverlog.entity.IotDriverLog;
import vip.xiaonuo.iot.modular.driverlog.param.IotDriverLogAddParam;
import vip.xiaonuo.iot.modular.driverlog.param.IotDriverLogEditParam;
import vip.xiaonuo.iot.modular.driverlog.param.IotDriverLogIdParam;
import vip.xiaonuo.iot.modular.driverlog.param.IotDriverLogPageParam;
import vip.xiaonuo.iot.modular.driverlog.service.IotDriverLogService;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.io.IOException;
import java.util.List;

/**
 * 运行日志控制器
 *
 * @author jetox
 * @date  2025/12/13 09:46
 */
@Tag(name = "运行日志控制器")
@RestController
@Validated
public class IotDriverLogController {

    @Resource
    private IotDriverLogService iotDriverLogService;

    /**
     * 获取运行日志分页
     *
     * @author jetox
     * @date  2025/12/13 09:46
     */
    @Operation(summary = "获取运行日志分页")
    @SaCheckPermission("/iot/driverlog/page")
    @GetMapping("/iot/driverlog/page")
    public CommonResult<Page<IotDriverLog>> page(IotDriverLogPageParam iotDriverLogPageParam) {
        return CommonResult.data(iotDriverLogService.page(iotDriverLogPageParam));
    }

    /**
     * 添加运行日志
     *
     * @author jetox
     * @date  2025/12/13 09:46
     */
    @Operation(summary = "添加运行日志")
    @CommonLog("添加运行日志")
    @SaCheckPermission("/iot/driverlog/add")
    @PostMapping("/iot/driverlog/add")
    public CommonResult<String> add(@RequestBody @Valid IotDriverLogAddParam iotDriverLogAddParam) {
        iotDriverLogService.add(iotDriverLogAddParam);
        return CommonResult.ok();
    }

    /**
     * 编辑运行日志
     *
     * @author jetox
     * @date  2025/12/13 09:46
     */
    @Operation(summary = "编辑运行日志")
    @CommonLog("编辑运行日志")
    @SaCheckPermission("/iot/driverlog/edit")
    @PostMapping("/iot/driverlog/edit")
    public CommonResult<String> edit(@RequestBody @Valid IotDriverLogEditParam iotDriverLogEditParam) {
        iotDriverLogService.edit(iotDriverLogEditParam);
        return CommonResult.ok();
    }

    /**
     * 删除运行日志
     *
     * @author jetox
     * @date  2025/12/13 09:46
     */
    @Operation(summary = "删除运行日志")
    @CommonLog("删除运行日志")
    @SaCheckPermission("/iot/driverlog/delete")
    @PostMapping("/iot/driverlog/delete")
    public CommonResult<String> delete(@RequestBody @Valid @NotEmpty(message = "集合不能为空")
                                                   List<IotDriverLogIdParam> iotDriverLogIdParamList) {
        iotDriverLogService.delete(iotDriverLogIdParamList);
        return CommonResult.ok();
    }

    /**
     * 获取运行日志详情
     *
     * @author jetox
     * @date  2025/12/13 09:46
     */
    @Operation(summary = "获取运行日志详情")
    @SaCheckPermission("/iot/driverlog/detail")
    @GetMapping("/iot/driverlog/detail")
    public CommonResult<IotDriverLog> detail(@Valid IotDriverLogIdParam iotDriverLogIdParam) {
        return CommonResult.data(iotDriverLogService.detail(iotDriverLogIdParam));
    }

    /**
     * 下载运行日志导入模板
     *
     * @author jetox
     * @date  2025/12/13 09:46
     */
    @Operation(summary = "下载运行日志导入模板")
    @GetMapping(value = "/iot/driverlog/downloadImportTemplate", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
        iotDriverLogService.downloadImportTemplate(response);
    }

    /**
     * 导入运行日志
     *
     * @author jetox
     * @date  2025/12/13 09:46
     */
    @Operation(summary = "导入运行日志")
    @CommonLog("导入运行日志")
    @SaCheckPermission("/iot/driverlog/importData")
    @PostMapping("/iot/driverlog/importData")
    public CommonResult<JSONObject> importData(@RequestPart("file") MultipartFile file) {
        return CommonResult.data(iotDriverLogService.importData(file));
    }

    /**
     * 导出运行日志
     *
     * @author jetox
     * @date  2025/12/13 09:46
     */
    @Operation(summary = "导出运行日志")
    @SaCheckPermission("/iot/driverlog/exportData")
    @PostMapping(value = "/iot/driverlog/exportData", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void exportData(@RequestBody List<IotDriverLogIdParam> iotDriverLogIdParamList, HttpServletResponse response) throws IOException {
        iotDriverLogService.exportData(iotDriverLogIdParamList, response);
    }
}
