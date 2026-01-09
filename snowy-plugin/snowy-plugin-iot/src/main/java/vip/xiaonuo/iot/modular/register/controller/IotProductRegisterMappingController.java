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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vip.xiaonuo.common.pojo.CommonResult;
import vip.xiaonuo.iot.modular.register.entity.IotProductRegisterMapping;
import vip.xiaonuo.iot.modular.register.service.IotProductRegisterMappingService;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;

/**
 * 产品寄存器映射Controller
 *
 * @author jetox
 * @date  2025/12/13 07:45
 **/
@Tag(name = "产品寄存器映射")
@RestController
@Validated
@RequestMapping("/iot/product/register")
public class IotProductRegisterMappingController {

    @Resource
    private IotProductRegisterMappingService iotProductRegisterMappingService;

    /**
     * 获取产品的寄存器映射列表
     */
    @Operation(summary = "获取产品寄存器映射列表")
    @GetMapping("/list/{productId}")
    public CommonResult<List<IotProductRegisterMapping>> list(@PathVariable String productId) {
        return CommonResult.data(iotProductRegisterMappingService.getProductRegisterMappings(productId));
    }

    /**
     * 批量保存产品的寄存器映射
     */
    @Operation(summary = "批量保存产品寄存器映射")
    @PostMapping("/batchSave/{productId}")
    public CommonResult<String> batchSave(@PathVariable String productId, 
                                           @RequestBody @Valid List<IotProductRegisterMapping> mappings) {
        iotProductRegisterMappingService.batchSaveProductRegisterMappings(productId, mappings);
        return CommonResult.ok();
    }

    /**
     * 删除产品的所有寄存器映射
     */
    @Operation(summary = "删除产品所有寄存器映射")
    @DeleteMapping("/delete/{productId}")
    public CommonResult<String> delete(@PathVariable String productId) {
        iotProductRegisterMappingService.deleteByProductId(productId);
        return CommonResult.ok();
    }
}
