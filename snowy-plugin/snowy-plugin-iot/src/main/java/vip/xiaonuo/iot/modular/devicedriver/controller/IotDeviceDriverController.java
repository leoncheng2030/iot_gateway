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
package vip.xiaonuo.iot.modular.devicedriver.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vip.xiaonuo.common.annotation.CommonLog;
import vip.xiaonuo.common.pojo.CommonResult;
import vip.xiaonuo.iot.core.config.DriverConfigFactory;
import vip.xiaonuo.iot.core.config.DriverConfigField;
import vip.xiaonuo.iot.core.config.DriverConfigTemplate;
import vip.xiaonuo.iot.modular.devicedriver.entity.IotDeviceDriver;
import vip.xiaonuo.iot.modular.devicedriver.param.IotDeviceDriverAddParam;
import vip.xiaonuo.iot.modular.devicedriver.param.IotDeviceDriverEditParam;
import vip.xiaonuo.iot.modular.devicedriver.param.IotDeviceDriverIdParam;
import vip.xiaonuo.iot.modular.devicedriver.param.IotDeviceDriverPageParam;
import vip.xiaonuo.iot.modular.devicedriver.service.IotDeviceDriverService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 设备驱动配置表控制器
 *
 * @author jetox
 * @date 2025/12/13 09:45
 */
@Slf4j
@Tag(name = "设备驱动配置表控制器")
@RestController
@Validated
public class IotDeviceDriverController {

    @Resource
    private IotDeviceDriverService iotDeviceDriverService;

/**
     * 获取设备驱动配置表分页
     *
     * @author jetox
     * @date 2025/12/13 09:45
     */
    @Operation(summary = "获取设备驱动配置表分页")
    @SaCheckPermission("/iot/devicedriver/page")
    @GetMapping("/iot/devicedriver/page")
    public CommonResult<Page<IotDeviceDriver>> page(IotDeviceDriverPageParam iotDeviceDriverPageParam) {
        return CommonResult.data(iotDeviceDriverService.page(iotDeviceDriverPageParam));
    }

    /**
     * 添加设备驱动配置表
     *
     * @author jetox
     * @date 2025/12/13 09:45
     */
    @Operation(summary = "添加设备驱动配置表")
    @CommonLog("添加设备驱动配置表")
    @SaCheckPermission("/iot/devicedriver/add")
    @PostMapping("/iot/devicedriver/add")
    public CommonResult<String> add(@RequestBody @Valid IotDeviceDriverAddParam iotDeviceDriverAddParam) {
        iotDeviceDriverService.add(iotDeviceDriverAddParam);
        return CommonResult.ok();
    }

    /**
     * 编辑设备驱动配置表
     *
     * @author jetox
     * @date 2025/12/13 09:45
     */
    @Operation(summary = "编辑设备驱动配置表")
    @CommonLog("编辑设备驱动配置表")
    @SaCheckPermission("/iot/devicedriver/edit")
    @PostMapping("/iot/devicedriver/edit")
    public CommonResult<String> edit(@RequestBody @Valid IotDeviceDriverEditParam iotDeviceDriverEditParam) {
        iotDeviceDriverService.edit(iotDeviceDriverEditParam);
        return CommonResult.ok();
    }

    /**
     * 删除设备驱动配置表
     *
     * @author jetox
     * @date 2025/12/13 09:45
     */
    @Operation(summary = "删除设备驱动配置表")
    @CommonLog("删除设备驱动配置表")
    @SaCheckPermission("/iot/devicedriver/delete")
    @PostMapping("/iot/devicedriver/delete")
    public CommonResult<String> delete(@RequestBody @Valid @NotEmpty(message = "集合不能为空")
                                                   List<IotDeviceDriverIdParam> iotDeviceDriverIdParamList) {
        iotDeviceDriverService.delete(iotDeviceDriverIdParamList);
        return CommonResult.ok();
    }

    /**
     * 获取设备驱动配置表详情
     *
     * @author jetox
     * @date 2025/12/13 09:45
     */
    @Operation(summary = "获取设备驱动配置表详情")
    @SaCheckPermission("/iot/devicedriver/detail")
    @GetMapping("/iot/devicedriver/detail")
    public CommonResult<IotDeviceDriver> detail(@Valid IotDeviceDriverIdParam iotDeviceDriverIdParam) {
        return CommonResult.data(iotDeviceDriverService.detail(iotDeviceDriverIdParam));
    }

    /**
     * 下载设备驱动配置表导入模板
     *
     * @author jetox
     * @date 2025/12/13 09:45
     */
    @Operation(summary = "下载设备驱动配置表导入模板")
    @GetMapping(value = "/iot/devicedriver/downloadImportTemplate", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
        iotDeviceDriverService.downloadImportTemplate(response);
    }

    /**
     * 导入设备驱动配置表
     *
     * @author jetox
     * @date 2025/12/13 09:45
     */
    @Operation(summary = "导入设备驱动配置表")
    @CommonLog("导入设备驱动配置表")
    @SaCheckPermission("/iot/devicedriver/importData")
    @PostMapping("/iot/devicedriver/importData")
    public CommonResult<JSONObject> importData(@RequestPart("file") MultipartFile file) {
        return CommonResult.data(iotDeviceDriverService.importData(file));
    }

    /**
     * 导出设备驱动配置表
     *
     * @author jetox
     * @date 2025/12/13 09:45
     */
    @Operation(summary = "导出设备驱动配置表")
    @SaCheckPermission("/iot/devicedriver/exportData")
    @PostMapping(value = "/iot/devicedriver/exportData", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void exportData(@RequestBody List<IotDeviceDriverIdParam> iotDeviceDriverIdParamList, HttpServletResponse response) throws IOException {
        iotDeviceDriverService.exportData(iotDeviceDriverIdParamList, response);
    }

    /**
     * 启动驱动服务
     *
     * @author jetox
     * @date 2025/12/13
     */
    @Operation(summary = "启动驱动服务")
    @CommonLog("启动驱动服务")
    @SaCheckPermission("/iot/devicedriver/start")
    @PostMapping("/iot/devicedriver/start")
    public CommonResult<String> startDriver(@RequestBody @Valid IotDeviceDriverIdParam iotDeviceDriverIdParam) {
        iotDeviceDriverService.startDriver(iotDeviceDriverIdParam.getId());
        return CommonResult.ok();
    }

    /**
     * 停止驱动服务
     *
     * @author jetox
     * @date 2025/12/13
     */
    @Operation(summary = "停止驱动服务")
    @CommonLog("停止驱动服务")
    @SaCheckPermission("/iot/devicedriver/stop")
    @PostMapping("/iot/devicedriver/stop")
    public CommonResult<String> stopDriver(@RequestBody @Valid IotDeviceDriverIdParam iotDeviceDriverIdParam) {
        iotDeviceDriverService.stopDriver(iotDeviceDriverIdParam.getId());
        return CommonResult.ok();
    }

    /**
     * 重启驱动服务
     *
     * @author jetox
     * @date 2025/12/13
     */
    @Operation(summary = "重启驱动服务")
    @CommonLog("重启驱动服务")
    @SaCheckPermission("/iot/devicedriver/restart")
    @PostMapping("/iot/devicedriver/restart")
    public CommonResult<String> restartDriver(@RequestBody @Valid IotDeviceDriverIdParam iotDeviceDriverIdParam) {
        iotDeviceDriverService.restartDriver(iotDeviceDriverIdParam.getId());
        return CommonResult.ok();
    }

    /**
     * 获取驱动运行状态
     *
     * @author jetox
     * @date 2025/12/13
     */
    @Operation(summary = "获取驱动运行状态")
    @SaCheckPermission("/iot/devicedriver/status")
    @GetMapping("/iot/devicedriver/status")
    public CommonResult<JSONObject> getDriverStatus(@RequestParam String id) {
        return CommonResult.data(iotDeviceDriverService.getDriverStatus(id));
    }

    /**
     * 获取驱动配置模板（区分驱动级和设备级）
     *
     * @author jetox
     * @date 2026/01/08
     */
    @Operation(summary = "获取驱动配置模板")
    @GetMapping("/iot/devicedriver/configTemplate")
    public CommonResult<DriverConfigTemplate> getConfigTemplate(@RequestParam String driverType) {
        try {
            log.info("获取驱动配置模板 - driverType: {}", driverType);
            // 通过工厂获取配置字段
            List<DriverConfigField> allFields = DriverConfigFactory.getConfigFields(driverType);
            log.info("返回配置字段数量: {}", allFields.size());
            
            // 区分驱动级和设备级字段
            List<DriverConfigField> driverFields = new ArrayList<>();
            List<DriverConfigField> deviceFields = new ArrayList<>();
            
            for (DriverConfigField field : allFields) {
                if ("driver".equals(field.getLevel())) {
                    driverFields.add(field);
                } else if ("device".equals(field.getLevel())) {
                    deviceFields.add(field);
                } else {
                    // 如果没有设置level，默认为设备级（向后兼容）
                    log.warn("字段 {} 没有设置level，默认为设备级", field.getKey());
                    deviceFields.add(field);
                }
            }
            
            log.info("驱动级字段数量: {}, 设备级字段数量: {}", driverFields.size(), deviceFields.size());
            
            DriverConfigTemplate template = new DriverConfigTemplate(driverFields, deviceFields);
            return CommonResult.data(template);
        } catch (Exception e) {
            log.error("获取驱动配置模板失败 - driverType: {}", driverType, e);
            return CommonResult.data(new DriverConfigTemplate(new ArrayList<>(), new ArrayList<>()));
        }
    }
}