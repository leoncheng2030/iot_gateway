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
package vip.xiaonuo.iot.modular.devicedriverrel.controller;

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
import vip.xiaonuo.iot.modular.devicedriverrel.entity.IotDeviceDriverRel;
import vip.xiaonuo.iot.modular.devicedriverrel.param.IotDeviceDriverRelAddParam;
import vip.xiaonuo.iot.modular.devicedriverrel.param.IotDeviceDriverRelBindParam;
import vip.xiaonuo.iot.modular.devicedriverrel.param.IotDeviceDriverRelEditParam;
import vip.xiaonuo.iot.modular.devicedriverrel.param.IotDeviceDriverRelIdParam;
import vip.xiaonuo.iot.modular.devicedriverrel.param.IotDeviceDriverRelPageParam;
import vip.xiaonuo.iot.modular.devicedriverrel.param.IotDeviceDriverRelUnbindParam;
import vip.xiaonuo.iot.modular.devicedriverrel.service.IotDeviceDriverRelService;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.io.IOException;
import java.util.List;

/**
 * 设备驱动控制器
 *
 * @author jetox
 * @date  2025/12/13 09:46
 */
@Tag(name = "设备驱动控制器")
@RestController
@Validated
public class IotDeviceDriverRelController {

    @Resource
    private IotDeviceDriverRelService iotDeviceDriverRelService;

    /**
     * 获取设备驱动分页
     *
     * @author jetox
     * @date  2025/12/13 09:46
     */
    @Operation(summary = "获取设备驱动分页")
    @SaCheckPermission("/iot/devicedriverrel/page")
    @GetMapping("/iot/devicedriverrel/page")
    public CommonResult<Page<IotDeviceDriverRel>> page(IotDeviceDriverRelPageParam iotDeviceDriverRelPageParam) {
        return CommonResult.data(iotDeviceDriverRelService.page(iotDeviceDriverRelPageParam));
    }

    /**
     * 添加设备驱动
     *
     * @author jetox
     * @date  2025/12/13 09:46
     */
    @Operation(summary = "添加设备驱动")
    @CommonLog("添加设备驱动")
    @SaCheckPermission("/iot/devicedriverrel/add")
    @PostMapping("/iot/devicedriverrel/add")
    public CommonResult<String> add(@RequestBody @Valid IotDeviceDriverRelAddParam iotDeviceDriverRelAddParam) {
        iotDeviceDriverRelService.add(iotDeviceDriverRelAddParam);
        return CommonResult.ok();
    }

    /**
     * 编辑设备驱动
     *
     * @author jetox
     * @date  2025/12/13 09:46
     */
    @Operation(summary = "编辑设备驱动")
    @CommonLog("编辑设备驱动")
    @SaCheckPermission("/iot/devicedriverrel/edit")
    @PostMapping("/iot/devicedriverrel/edit")
    public CommonResult<String> edit(@RequestBody @Valid IotDeviceDriverRelEditParam iotDeviceDriverRelEditParam) {
        iotDeviceDriverRelService.edit(iotDeviceDriverRelEditParam);
        return CommonResult.ok();
    }

    /**
     * 删除设备驱动
     *
     * @author jetox
     * @date  2025/12/13 09:46
     */
    @Operation(summary = "删除设备驱动")
    @CommonLog("删除设备驱动")
    @SaCheckPermission("/iot/devicedriverrel/delete")
    @PostMapping("/iot/devicedriverrel/delete")
    public CommonResult<String> delete(@RequestBody @Valid @NotEmpty(message = "集合不能为空")
                                                   List<IotDeviceDriverRelIdParam> iotDeviceDriverRelIdParamList) {
        iotDeviceDriverRelService.delete(iotDeviceDriverRelIdParamList);
        return CommonResult.ok();
    }

    /**
     * 获取设备驱动详情
     *
     * @author jetox
     * @date  2025/12/13 09:46
     */
    @Operation(summary = "获取设备驱动详情")
    @SaCheckPermission("/iot/devicedriverrel/detail")
    @GetMapping("/iot/devicedriverrel/detail")
    public CommonResult<IotDeviceDriverRel> detail(@Valid IotDeviceDriverRelIdParam iotDeviceDriverRelIdParam) {
        return CommonResult.data(iotDeviceDriverRelService.detail(iotDeviceDriverRelIdParam));
    }

    /**
     * 下载设备驱动导入模板
     *
     * @author jetox
     * @date  2025/12/13 09:46
     */
    @Operation(summary = "下载设备驱动导入模板")
    @GetMapping(value = "/iot/devicedriverrel/downloadImportTemplate", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
        iotDeviceDriverRelService.downloadImportTemplate(response);
    }

    /**
     * 导入设备驱动
     *
     * @author jetox
     * @date  2025/12/13 09:46
     */
    @Operation(summary = "导入设备驱动")
    @CommonLog("导入设备驱动")
    @SaCheckPermission("/iot/devicedriverrel/importData")
    @PostMapping("/iot/devicedriverrel/importData")
    public CommonResult<JSONObject> importData(@RequestPart("file") MultipartFile file) {
        return CommonResult.data(iotDeviceDriverRelService.importData(file));
    }

    /**
     * 导出设备驱动
     *
     * @author jetox
     * @date  2025/12/13 09:46
     */
    @Operation(summary = "导出设备驱动")
    @SaCheckPermission("/iot/devicedriverrel/exportData")
    @PostMapping(value = "/iot/devicedriverrel/exportData", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void exportData(@RequestBody List<IotDeviceDriverRelIdParam> iotDeviceDriverRelIdParamList, HttpServletResponse response) throws IOException {
        iotDeviceDriverRelService.exportData(iotDeviceDriverRelIdParamList, response);
    }

    /**
     * 获取设备关联的驱动列表
     *
     * @author jetox
     * @date  2025/12/13
     */
    @Operation(summary = "获取设备关联的驱动列表")
    @SaCheckPermission("/iot/devicedriverrel/listByDeviceId")
    @GetMapping("/iot/devicedriverrel/listByDeviceId")
    public CommonResult<List<IotDeviceDriverRel>> listByDeviceId(@RequestParam String deviceId) {
        return CommonResult.data(iotDeviceDriverRelService.listByDeviceId(deviceId));
    }

    /**
     * 绑定设备驱动
     *
     * @author jetox
     * @date  2025/12/13
     */
    @Operation(summary = "绑定设备驱动")
    @CommonLog("绑定设备驱动")
    @SaCheckPermission("/iot/devicedriverrel/bindDriver")
    @PostMapping("/iot/devicedriverrel/bindDriver")
    public CommonResult<String> bindDriver(@RequestBody @Valid IotDeviceDriverRelBindParam param) {
        iotDeviceDriverRelService.bindDriver(param.getDeviceId(), param.getDriverId(), param.getDeviceConfig());
        return CommonResult.ok();
    }

    /**
     * 解绑设备驱动
     *
     * @author jetox
     * @date  2025/12/13
     */
    @Operation(summary = "解绑设备驱动")
    @CommonLog("解绑设备驱动")
    @SaCheckPermission("/iot/devicedriverrel/unbindDriver")
    @PostMapping("/iot/devicedriverrel/unbindDriver")
    public CommonResult<String> unbindDriver(@RequestBody @Valid IotDeviceDriverRelUnbindParam param) {
        iotDeviceDriverRelService.unbindDriver(param.getDeviceId(), param.getDriverId());
        return CommonResult.ok();
    }
}
