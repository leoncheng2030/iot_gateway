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
package vip.xiaonuo.iot.modular.thingmodel.controller;

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
import vip.xiaonuo.iot.modular.thingmodel.entity.IotThingModel;
import vip.xiaonuo.iot.modular.thingmodel.param.*;
import vip.xiaonuo.iot.modular.thingmodel.service.IotThingModelService;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.io.IOException;
import java.util.List;

/**
 * 物模型控制器
 *
 * @author jetox
 * @date  2025/12/11 09:08
 */
@Tag(name = "物模型控制器")
@RestController
@Validated
public class IotThingModelController {

    @Resource
    private IotThingModelService iotThingModelService;

    /**
     * 获取物模型分页
     *
     * @author jetox
     * @date  2025/12/11 09:08
     */
    @Operation(summary = "获取物模型分页")
    @SaCheckPermission("/iot/thingmodel/page")
    @GetMapping("/iot/thingmodel/page")
    public CommonResult<Page<IotThingModel>> page(IotThingModelPageParam iotThingModelPageParam) {
        return CommonResult.data(iotThingModelService.page(iotThingModelPageParam));
    }

    /**
     * 根据产品ID获取物模型列表
     *
     * @author jetox
     * @date  2025/12/11 09:26
     */
    @Operation(summary = "根据产品ID获取物模型列表")
    @GetMapping("/iot/thingmodel/listByProduct")
    public CommonResult<List<IotThingModel>> listByProduct(IotThingModelListParam iotThingModelListParam) {
        return CommonResult.data(iotThingModelService.listByProduct(iotThingModelListParam));
    }

    /**
     * 添加物模型
     *
     * @author jetox
     * @date  2025/12/11 09:08
     */
    @Operation(summary = "添加物模型")
    @CommonLog("添加物模型")
    @SaCheckPermission("/iot/thingmodel/add")
    @PostMapping("/iot/thingmodel/add")
    public CommonResult<String> add(@RequestBody @Valid IotThingModelAddParam iotThingModelAddParam) {
        iotThingModelService.add(iotThingModelAddParam);
        return CommonResult.ok();
    }

    /**
     * 编辑物模型
     *
     * @author jetox
     * @date  2025/12/11 09:08
     */
    @Operation(summary = "编辑物模型")
    @CommonLog("编辑物模型")
    @SaCheckPermission("/iot/thingmodel/edit")
    @PostMapping("/iot/thingmodel/edit")
    public CommonResult<String> edit(@RequestBody @Valid IotThingModelEditParam iotThingModelEditParam) {
        iotThingModelService.edit(iotThingModelEditParam);
        return CommonResult.ok();
    }

    /**
     * 删除物模型
     *
     * @author jetox
     * @date  2025/12/11 09:08
     */
    @Operation(summary = "删除物模型")
    @CommonLog("删除物模型")
    @SaCheckPermission("/iot/thingmodel/delete")
    @PostMapping("/iot/thingmodel/delete")
    public CommonResult<String> delete(@RequestBody @Valid @NotEmpty(message = "集合不能为空")
                                                   List<IotThingModelIdParam> iotThingModelIdParamList) {
        iotThingModelService.delete(iotThingModelIdParamList);
        return CommonResult.ok();
    }

    /**
     * 获取物模型详情
     *
     * @author jetox
     * @date  2025/12/11 09:08
     */
    @Operation(summary = "获取物模型详情")
    @SaCheckPermission("/iot/thingmodel/detail")
    @GetMapping("/iot/thingmodel/detail")
    public CommonResult<IotThingModel> detail(@Valid IotThingModelIdParam iotThingModelIdParam) {
        return CommonResult.data(iotThingModelService.detail(iotThingModelIdParam));
    }

    /**
     * 下载物模型导入模板
     *
     * @author jetox
     * @date  2025/12/11 09:08
     */
    @Operation(summary = "下载物模型导入模板")
    @GetMapping(value = "/iot/thingmodel/downloadImportTemplate", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
        iotThingModelService.downloadImportTemplate(response);
    }

    /**
     * 导入物模型
     *
     * @author jetox
     * @date  2025/12/11 09:08
     */
    @Operation(summary = "导入物模型")
    @CommonLog("导入物模型")
    @SaCheckPermission("/iot/thingmodel/importData")
    @PostMapping("/iot/thingmodel/importData")
    public CommonResult<JSONObject> importData(@RequestPart("file") MultipartFile file) {
        return CommonResult.data(iotThingModelService.importData(file));
    }

    /**
     * 导出物模型
     *
     * @author jetox
     * @date  2025/12/11 09:08
     */
    @Operation(summary = "导出物模型")
    @SaCheckPermission("/iot/thingmodel/exportData")
    @PostMapping(value = "/iot/thingmodel/exportData", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void exportData(@RequestBody List<IotThingModelIdParam> iotThingModelIdParamList, HttpServletResponse response) throws IOException {
        iotThingModelService.exportData(iotThingModelIdParamList, response);
    }
}
