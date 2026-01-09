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
package vip.xiaonuo.iot.modular.rule.controller;

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
import vip.xiaonuo.iot.modular.rule.entity.IotRule;
import vip.xiaonuo.iot.modular.rule.param.IotRuleAddParam;
import vip.xiaonuo.iot.modular.rule.param.IotRuleEditParam;
import vip.xiaonuo.iot.modular.rule.param.IotRuleIdParam;
import vip.xiaonuo.iot.modular.rule.param.IotRulePageParam;
import vip.xiaonuo.iot.modular.rule.service.IotRuleService;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.io.IOException;
import java.util.List;

/**
 * 规则引擎控制器
 *
 * @author jetox
 * @date  2025/12/11 07:32
 */
@Tag(name = "规则引擎控制器")
@RestController
@Validated
public class IotRuleController {

    @Resource
    private IotRuleService iotRuleService;

    /**
     * 获取规则引擎分页
     *
     * @author jetox
     * @date  2025/12/11 07:32
     */
    @Operation(summary = "获取规则引擎分页")
    @SaCheckPermission("/iot/rule/page")
    @GetMapping("/iot/rule/page")
    public CommonResult<Page<IotRule>> page(IotRulePageParam iotRulePageParam) {
        return CommonResult.data(iotRuleService.page(iotRulePageParam));
    }

    /**
     * 添加规则引擎
     *
     * @author jetox
     * @date  2025/12/11 07:32
     */
    @Operation(summary = "添加规则引擎")
    @CommonLog("添加规则引擎")
    @SaCheckPermission("/iot/rule/add")
    @PostMapping("/iot/rule/add")
    public CommonResult<String> add(@RequestBody @Valid IotRuleAddParam iotRuleAddParam) {
        iotRuleService.add(iotRuleAddParam);
        return CommonResult.ok();
    }

    /**
     * 编辑规则引擎
     *
     * @author jetox
     * @date  2025/12/11 07:32
     */
    @Operation(summary = "编辑规则引擎")
    @CommonLog("编辑规则引擎")
    @SaCheckPermission("/iot/rule/edit")
    @PostMapping("/iot/rule/edit")
    public CommonResult<String> edit(@RequestBody @Valid IotRuleEditParam iotRuleEditParam) {
        iotRuleService.edit(iotRuleEditParam);
        return CommonResult.ok();
    }

    /**
     * 删除规则引擎
     *
     * @author jetox
     * @date  2025/12/11 07:32
     */
    @Operation(summary = "删除规则引擎")
    @CommonLog("删除规则引擎")
    @SaCheckPermission("/iot/rule/delete")
    @PostMapping("/iot/rule/delete")
    public CommonResult<String> delete(@RequestBody @Valid @NotEmpty(message = "集合不能为空")
                                                   List<IotRuleIdParam> iotRuleIdParamList) {
        iotRuleService.delete(iotRuleIdParamList);
        return CommonResult.ok();
    }

    /**
     * 获取规则引擎详情
     *
     * @author jetox
     * @date  2025/12/11 07:32
     */
    @Operation(summary = "获取规则引擎详情")
    @SaCheckPermission("/iot/rule/detail")
    @GetMapping("/iot/rule/detail")
    public CommonResult<IotRule> detail(@Valid IotRuleIdParam iotRuleIdParam) {
        return CommonResult.data(iotRuleService.detail(iotRuleIdParam));
    }

    /**
     * 下载规则引擎导入模板
     *
     * @author jetox
     * @date  2025/12/11 07:32
     */
    @Operation(summary = "下载规则引擎导入模板")
    @GetMapping(value = "/iot/rule/downloadImportTemplate", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
        iotRuleService.downloadImportTemplate(response);
    }

    /**
     * 导入规则引擎
     *
     * @author jetox
     * @date  2025/12/11 07:32
     */
    @Operation(summary = "导入规则引擎")
    @CommonLog("导入规则引擎")
    @SaCheckPermission("/iot/rule/importData")
    @PostMapping("/iot/rule/importData")
    public CommonResult<JSONObject> importData(@RequestPart("file") MultipartFile file) {
        return CommonResult.data(iotRuleService.importData(file));
    }

    /**
     * 导出规则引擎
     *
     * @author jetox
     * @date  2025/12/11 07:32
     */
    @Operation(summary = "导出规则引擎")
    @SaCheckPermission("/iot/rule/exportData")
    @PostMapping(value = "/iot/rule/exportData", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void exportData(@RequestBody List<IotRuleIdParam> iotRuleIdParamList, HttpServletResponse response) throws IOException {
        iotRuleService.exportData(iotRuleIdParamList, response);
    }
}
