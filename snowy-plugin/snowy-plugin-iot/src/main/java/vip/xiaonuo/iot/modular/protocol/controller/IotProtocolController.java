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
package vip.xiaonuo.iot.modular.protocol.controller;

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
import vip.xiaonuo.iot.modular.protocol.entity.IotProtocol;
import vip.xiaonuo.iot.modular.protocol.param.IotProtocolAddParam;
import vip.xiaonuo.iot.modular.protocol.param.IotProtocolEditParam;
import vip.xiaonuo.iot.modular.protocol.param.IotProtocolIdParam;
import vip.xiaonuo.iot.modular.protocol.param.IotProtocolPageParam;
import vip.xiaonuo.iot.modular.protocol.service.IotProtocolService;
import vip.xiaonuo.iot.core.protocol.ProtocolManager;
import vip.xiaonuo.iot.core.protocol.ProtocolServerFactory;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.io.IOException;
import java.util.List;

/**
 * 协议配置控制器
 *
 * @author jetox
 * @date  2025/12/11 07:09
 */
@Tag(name = "协议配置控制器")
@RestController
@Validated
public class IotProtocolController {

    @Resource
    private IotProtocolService iotProtocolService;

    @Resource
    private ProtocolManager protocolManager;
    
    @Resource
    private ProtocolServerFactory protocolServerFactory;

    /**
     * 获取协议配置分页
     *
     * @author jetox
     * @date  2025/12/11 07:09
     */
    @Operation(summary = "获取协议配置分页")
    @SaCheckPermission("/iot/protocol/page")
    @GetMapping("/iot/protocol/page")
    public CommonResult<Page<IotProtocol>> page(IotProtocolPageParam iotProtocolPageParam) {
        return CommonResult.data(iotProtocolService.page(iotProtocolPageParam));
    }

    /**
     * 添加协议配置
     *
     * @author jetox
     * @date  2025/12/11 07:09
     */
    @Operation(summary = "添加协议配置")
    @CommonLog("添加协议配置")
    @SaCheckPermission("/iot/protocol/add")
    @PostMapping("/iot/protocol/add")
    public CommonResult<String> add(@RequestBody @Valid IotProtocolAddParam iotProtocolAddParam) {
        iotProtocolService.add(iotProtocolAddParam);
        return CommonResult.ok();
    }

    /**
     * 编辑协议配置
     *
     * @author jetox
     * @date  2025/12/11 07:09
     */
    @Operation(summary = "编辑协议配置")
    @CommonLog("编辑协议配置")
    @SaCheckPermission("/iot/protocol/edit")
    @PostMapping("/iot/protocol/edit")
    public CommonResult<String> edit(@RequestBody @Valid IotProtocolEditParam iotProtocolEditParam) {
        iotProtocolService.edit(iotProtocolEditParam);
        return CommonResult.ok();
    }

    /**
     * 删除协议配置
     *
     * @author jetox
     * @date  2025/12/11 07:09
     */
    @Operation(summary = "删除协议配置")
    @CommonLog("删除协议配置")
    @SaCheckPermission("/iot/protocol/delete")
    @PostMapping("/iot/protocol/delete")
    public CommonResult<String> delete(@RequestBody @Valid @NotEmpty(message = "集合不能为空")
                                                   List<IotProtocolIdParam> iotProtocolIdParamList) {
        iotProtocolService.delete(iotProtocolIdParamList);
        return CommonResult.ok();
    }

    /**
     * 获取协议配置详情
     *
     * @author jetox
     * @date  2025/12/11 07:09
     */
    @Operation(summary = "获取协议配置详情")
    @SaCheckPermission("/iot/protocol/detail")
    @GetMapping("/iot/protocol/detail")
    public CommonResult<IotProtocol> detail(@Valid IotProtocolIdParam iotProtocolIdParam) {
        return CommonResult.data(iotProtocolService.detail(iotProtocolIdParam));
    }

    /**
     * 下载协议配置导入模板
     *
     * @author jetox
     * @date  2025/12/11 07:09
     */
    @Operation(summary = "下载协议配置导入模板")
    @GetMapping(value = "/iot/protocol/downloadImportTemplate", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
        iotProtocolService.downloadImportTemplate(response);
    }

    /**
     * 导入协议配置
     *
     * @author jetox
     * @date  2025/12/11 07:09
     */
    @Operation(summary = "导入协议配置")
    @CommonLog("导入协议配置")
    @SaCheckPermission("/iot/protocol/importData")
    @PostMapping("/iot/protocol/importData")
    public CommonResult<JSONObject> importData(@RequestPart("file") MultipartFile file) {
        return CommonResult.data(iotProtocolService.importData(file));
    }

    /**
     * 导出协议配置
     *
     * @author jetox
     * @date  2025/12/11 07:09
     */
    @Operation(summary = "导出协议配置")
    @SaCheckPermission("/iot/protocol/exportData")
    @PostMapping(value = "/iot/protocol/exportData", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void exportData(@RequestBody List<IotProtocolIdParam> iotProtocolIdParamList, HttpServletResponse response) throws IOException {
        iotProtocolService.exportData(iotProtocolIdParamList, response);
    }

    /**
     * 启动协议服务
     *
     * @author jetox
     * @date 2025/12/11 10:40
     */
    @Operation(summary = "启动协议服务")
    @CommonLog("启动协议服务")
    @SaCheckPermission("/iot/protocol/start")
    @PostMapping("/iot/protocol/start")
    public CommonResult<String> start(@RequestBody @Valid IotProtocolIdParam iotProtocolIdParam) {
        protocolManager.startProtocol(iotProtocolIdParam.getId());
        return CommonResult.ok();
    }

    /**
     * 停止协议服务
     *
     * @author jetox
     * @date 2025/12/11 10:40
     */
    @Operation(summary = "停止协议服务")
    @CommonLog("停止协议服务")
    @SaCheckPermission("/iot/protocol/stop")
    @PostMapping("/iot/protocol/stop")
    public CommonResult<String> stop(@RequestBody @Valid IotProtocolIdParam iotProtocolIdParam) {
        protocolManager.stopProtocol(iotProtocolIdParam.getId());
        return CommonResult.ok();
    }

    /**
     * 重启协议服务
     *
     * @author jetox
     * @date 2025/12/11 10:40
     */
    @Operation(summary = "重启协议服务")
    @CommonLog("重启协议服务")
    @SaCheckPermission("/iot/protocol/restart")
    @PostMapping("/iot/protocol/restart")
    public CommonResult<String> restart(@RequestBody @Valid IotProtocolIdParam iotProtocolIdParam) {
        protocolManager.restartProtocol(iotProtocolIdParam.getId());
        return CommonResult.ok();
    }

    /**
     * 获取协议运行状态
     *
     * @author jetox
     * @date 2025/12/11 10:40
     */
    @Operation(summary = "获取协议运行状态")
    @SaCheckPermission("/iot/protocol/page")
    @GetMapping("/iot/protocol/status")
    public CommonResult<Boolean> status(@Valid IotProtocolIdParam iotProtocolIdParam) {
        return CommonResult.data(protocolManager.isRunning(iotProtocolIdParam.getId()));
    }
    
    /**
     * 获取所有已注册的协议类型列表
     * 用于前端下拉选择
     *
     * @author jetox
     * @date 2026/01/10
     */
    @Operation(summary = "获取协议类型列表")
    @GetMapping("/iot/protocol/types")
    public CommonResult<List<ProtocolServerFactory.ProtocolTypeDTO>> getProtocolTypes() {
        return CommonResult.data(protocolServerFactory.getAllProtocolTypes());
    }
}
