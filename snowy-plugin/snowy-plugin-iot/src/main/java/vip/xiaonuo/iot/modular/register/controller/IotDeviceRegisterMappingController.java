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
package vip.xiaonuo.iot.modular.register.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import vip.xiaonuo.common.pojo.CommonResult;
import vip.xiaonuo.iot.modular.register.entity.IotDeviceRegisterMapping;
import vip.xiaonuo.iot.modular.register.service.IotDeviceRegisterMappingService;

import java.util.List;

/**
 * 设备寄存器映射Controller
 *
 * @author jetox
 * @date  2025/12/12 12:33
 **/
@Tag(name = "设备寄存器映射")
@RestController
@RequestMapping("/iot/device/register")
public class IotDeviceRegisterMappingController {

    @Resource
    private IotDeviceRegisterMappingService iotDeviceRegisterMappingService;

    /**
     * 获取设备的寄存器映射列表
     */
    @Operation(summary = "获取设备寄存器映射列表")
    @GetMapping("/list/{deviceId}")
    public CommonResult<List<IotDeviceRegisterMapping>> list(@PathVariable String deviceId) {
        return CommonResult.data(iotDeviceRegisterMappingService.getDeviceRegisterMappings(deviceId));
    }

    /**
     * 批量保存设备的寄存器映射
     */
    @Operation(summary = "批量保存设备寄存器映射")
    @PostMapping("/batchSave/{deviceId}")
    public CommonResult<String> batchSave(@PathVariable String deviceId, 
                                           @RequestBody @Valid List<IotDeviceRegisterMapping> mappings) {
        iotDeviceRegisterMappingService.batchSaveDeviceRegisterMappings(deviceId, mappings);
        return CommonResult.ok();
    }

    /**
     * 删除设备的所有寄存器映射
     */
    @Operation(summary = "删除设备所有寄存器映射")
    @DeleteMapping("/delete/{deviceId}")
    public CommonResult<String> delete(@PathVariable String deviceId) {
        iotDeviceRegisterMappingService.deleteByDeviceId(deviceId);
        return CommonResult.ok();
    }
}
