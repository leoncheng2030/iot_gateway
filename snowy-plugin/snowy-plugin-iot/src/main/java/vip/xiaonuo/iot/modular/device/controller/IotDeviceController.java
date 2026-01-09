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
package vip.xiaonuo.iot.modular.device.controller;

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
import vip.xiaonuo.iot.modular.device.entity.IotDevice;
import vip.xiaonuo.iot.modular.device.param.IotDeviceAddParam;
import vip.xiaonuo.iot.modular.device.param.IotDeviceEditParam;
import vip.xiaonuo.iot.modular.device.param.IotDeviceIdParam;
import vip.xiaonuo.iot.modular.device.param.IotDevicePageParam;
import vip.xiaonuo.iot.modular.device.param.IotDeviceCommandParam;
import vip.xiaonuo.iot.modular.device.param.IotDevicePropertySetParam;
import vip.xiaonuo.iot.modular.device.param.IotDeviceServiceParam;
import vip.xiaonuo.iot.modular.device.service.IotDeviceService;
import vip.xiaonuo.iot.core.message.DeviceMessageService;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.io.IOException;
import java.util.List;

/**
 * 设备控制器
 *
 * @author jetox
 * @date  2025/12/11 07:24
 */
@Tag(name = "设备控制器")
@RestController
@Validated
public class IotDeviceController {

    @Resource
    private IotDeviceService iotDeviceService;
    
    @Resource
    private DeviceMessageService deviceMessageService;

    /**
     * 获取设备分页
     *
     * @author jetox
     * @date  2025/12/11 07:24
     */
    @Operation(summary = "获取设备分页")
    @SaCheckPermission("/iot/device/page")
    @GetMapping("/iot/device/page")
    public CommonResult<Page<IotDevice>> page(IotDevicePageParam iotDevicePageParam) {
        return CommonResult.data(iotDeviceService.page(iotDevicePageParam));
    }

    /**
     * 添加设备
     *
     * @author jetox
     * @date  2025/12/11 07:24
     */
    @Operation(summary = "添加设备")
    @CommonLog("添加设备")
    @SaCheckPermission("/iot/device/add")
    @PostMapping("/iot/device/add")
    public CommonResult<String> add(@RequestBody @Valid IotDeviceAddParam iotDeviceAddParam) {
        String deviceId = iotDeviceService.add(iotDeviceAddParam);
        return CommonResult.data(deviceId);
    }

    /**
     * 编辑设备
     *
     * @author jetox
     * @date  2025/12/11 07:24
     */
    @Operation(summary = "编辑设备")
    @CommonLog("编辑设备")
    @SaCheckPermission("/iot/device/edit")
    @PostMapping("/iot/device/edit")
    public CommonResult<String> edit(@RequestBody @Valid IotDeviceEditParam iotDeviceEditParam) {
        iotDeviceService.edit(iotDeviceEditParam);
        return CommonResult.ok();
    }

    /**
     * 删除设备
     *
     * @author jetox
     * @date  2025/12/11 07:24
     */
    @Operation(summary = "删除设备")
    @CommonLog("删除设备")
    @SaCheckPermission("/iot/device/delete")
    @PostMapping("/iot/device/delete")
    public CommonResult<String> delete(@RequestBody @Valid @NotEmpty(message = "集合不能为空")
                                                   List<IotDeviceIdParam> iotDeviceIdParamList) {
        iotDeviceService.delete(iotDeviceIdParamList);
        return CommonResult.ok();
    }

    /**
     * 获取设备详情
     *
     * @author jetox
     * @date  2025/12/11 07:24
     */
    @Operation(summary = "获取设备详情")
    @SaCheckPermission("/iot/device/detail")
    @GetMapping("/iot/device/detail")
    public CommonResult<IotDevice> detail(@Valid IotDeviceIdParam iotDeviceIdParam) {
        return CommonResult.data(iotDeviceService.detail(iotDeviceIdParam));
    }

    /**
     * 下载设备导入模板
     *
     * @author jetox
     * @date  2025/12/11 07:24
     */
    @Operation(summary = "下载设备导入模板")
    @GetMapping(value = "/iot/device/downloadImportTemplate", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
        iotDeviceService.downloadImportTemplate(response);
    }

    /**
     * 导入设备
     *
     * @author jetox
     * @date  2025/12/11 07:24
     */
    @Operation(summary = "导入设备")
    @CommonLog("导入设备")
    @SaCheckPermission("/iot/device/importData")
    @PostMapping("/iot/device/importData")
    public CommonResult<JSONObject> importData(@RequestPart("file") MultipartFile file) {
        return CommonResult.data(iotDeviceService.importData(file));
    }

    /**
     * 导出设备
     *
     * @author jetox
     * @date  2025/12/11 07:24
     */
    @Operation(summary = "导出设备")
    @SaCheckPermission("/iot/device/exportData")
    @PostMapping(value = "/iot/device/exportData", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void exportData(@RequestBody List<IotDeviceIdParam> iotDeviceIdParamList, HttpServletResponse response) throws IOException {
        iotDeviceService.exportData(iotDeviceIdParamList, response);
    }

    /**
     * 设置设备属性
     *
     * @author yubaoshan
     * @date  2025/12/11 16:35
     */
    @Operation(summary = "设置设备属性")
    @CommonLog("设置设备属性")
    @SaCheckPermission("/iot/device/setProperty")
    @PostMapping("/iot/device/setProperty")
    public CommonResult<String> setProperty(@RequestBody @Valid IotDevicePropertySetParam param) {
        iotDeviceService.setProperty(param);
        return CommonResult.ok();
    }

    /**
     * 下发设备指令
     *
     * @author yubaoshan
     * @date  2025/12/11 16:35
     */
    @Operation(summary = "下发设备指令")
    @CommonLog("下发设备指令")
    @SaCheckPermission("/iot/device/sendCommand")
    @PostMapping("/iot/device/sendCommand")
    public CommonResult<String> sendCommand(@RequestBody @Valid IotDeviceCommandParam param) {
        iotDeviceService.sendCommand(param);
        return CommonResult.ok();
    }

    /**
     * 调用设备服务
     *
     * @author jetox
     * @date  2025/12/12 15:24
     */
    @Operation(summary = "调用设备服务")
    @CommonLog("调用设备服务")
    @SaCheckPermission("/iot/device/invokeService")
    @PostMapping("/iot/device/invokeService")
    public CommonResult<String> invokeService(@RequestBody @Valid IotDeviceServiceParam param) {
        iotDeviceService.invokeService(param);
        return CommonResult.ok();
    }
}
