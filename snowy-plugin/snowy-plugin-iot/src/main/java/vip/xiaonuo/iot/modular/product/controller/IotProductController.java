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
package vip.xiaonuo.iot.modular.product.controller;

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
import vip.xiaonuo.iot.modular.product.entity.IotProduct;
import vip.xiaonuo.iot.modular.product.param.IotProductAddParam;
import vip.xiaonuo.iot.modular.product.param.IotProductEditParam;
import vip.xiaonuo.iot.modular.product.param.IotProductIdParam;
import vip.xiaonuo.iot.modular.product.param.IotProductPageParam;
import vip.xiaonuo.iot.modular.product.service.IotProductService;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.io.IOException;
import java.util.List;

/**
 * 产品控制器
 *
 * @author jetox
 * @date  2025/12/11 06:57
 */
@Tag(name = "产品控制器")
@RestController
@Validated
public class IotProductController {

    @Resource
    private IotProductService iotProductService;

    /**
     * 获取产品分页
     *
     * @author jetox
     * @date  2025/12/11 06:57
     */
    @Operation(summary = "获取产品分页")
    @SaCheckPermission("/iot/product/page")
    @GetMapping("/iot/product/page")
    public CommonResult<Page<IotProduct>> page(IotProductPageParam iotProductPageParam) {
        return CommonResult.data(iotProductService.page(iotProductPageParam));
    }

    /**
     * 添加产品
     *
     * @author jetox
     * @date  2025/12/11 06:57
     */
    @Operation(summary = "添加产品")
    @CommonLog("添加产品")
    @SaCheckPermission("/iot/product/add")
    @PostMapping("/iot/product/add")
    public CommonResult<String> add(@RequestBody @Valid IotProductAddParam iotProductAddParam) {
        iotProductService.add(iotProductAddParam);
        return CommonResult.ok();
    }

    /**
     * 编辑产品
     *
     * @author jetox
     * @date  2025/12/11 06:57
     */
    @Operation(summary = "编辑产品")
    @CommonLog("编辑产品")
    @SaCheckPermission("/iot/product/edit")
    @PostMapping("/iot/product/edit")
    public CommonResult<String> edit(@RequestBody @Valid IotProductEditParam iotProductEditParam) {
        iotProductService.edit(iotProductEditParam);
        return CommonResult.ok();
    }

    /**
     * 删除产品
     *
     * @author jetox
     * @date  2025/12/11 06:57
     */
    @Operation(summary = "删除产品")
    @CommonLog("删除产品")
    @SaCheckPermission("/iot/product/delete")
    @PostMapping("/iot/product/delete")
    public CommonResult<String> delete(@RequestBody @Valid @NotEmpty(message = "集合不能为空")
                                                   List<IotProductIdParam> iotProductIdParamList) {
        iotProductService.delete(iotProductIdParamList);
        return CommonResult.ok();
    }

    /**
     * 获取产品详情
     *
     * @author jetox
     * @date  2025/12/11 06:57
     */
    @Operation(summary = "获取产品详情")
    @SaCheckPermission("/iot/product/detail")
    @GetMapping("/iot/product/detail")
    public CommonResult<IotProduct> detail(@Valid IotProductIdParam iotProductIdParam) {
        return CommonResult.data(iotProductService.detail(iotProductIdParam));
    }

    /**
     * 下载产品导入模板
     *
     * @author jetox
     * @date  2025/12/11 06:57
     */
    @Operation(summary = "下载产品导入模板")
    @GetMapping(value = "/iot/product/downloadImportTemplate", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
        iotProductService.downloadImportTemplate(response);
    }

    /**
     * 导入产品
     *
     * @author jetox
     * @date  2025/12/11 06:57
     */
    @Operation(summary = "导入产品")
    @CommonLog("导入产品")
    @SaCheckPermission("/iot/product/importData")
    @PostMapping("/iot/product/importData")
    public CommonResult<JSONObject> importData(@RequestPart("file") MultipartFile file) {
        return CommonResult.data(iotProductService.importData(file));
    }

    /**
     * 导出产品
     *
     * @author jetox
     * @date  2025/12/11 06:57
     */
    @Operation(summary = "导出产品")
    @SaCheckPermission("/iot/product/exportData")
    @PostMapping(value = "/iot/product/exportData", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void exportData(@RequestBody List<IotProductIdParam> iotProductIdParamList, HttpServletResponse response) throws IOException {
        iotProductService.exportData(iotProductIdParamList, response);
    }
}
