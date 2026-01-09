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
package vip.xiaonuo.iot.modular.devicedata.controller;

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
import vip.xiaonuo.iot.modular.devicedata.entity.IotDeviceData;
import vip.xiaonuo.iot.modular.devicedata.param.IotDeviceDataAddParam;
import vip.xiaonuo.iot.modular.devicedata.param.IotDeviceDataEditParam;
import vip.xiaonuo.iot.modular.devicedata.param.IotDeviceDataIdParam;
import vip.xiaonuo.iot.modular.devicedata.param.IotDeviceDataPageParam;
import vip.xiaonuo.iot.modular.devicedata.service.IotDeviceDataService;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.io.IOException;
import java.util.List;

/**
 * 设备数据控制器
 *
 * @author jetox
 * @date  2025/12/11 07:27
 */
@Tag(name = "设备数据控制器")
@RestController
@Validated
public class IotDeviceDataController {

    @Resource
    private IotDeviceDataService iotDeviceDataService;

    /**
     * 获取设备数据分页
     *
     * @author jetox
     * @date  2025/12/11 07:27
     */
    @Operation(summary = "获取设备数据分页")
    @SaCheckPermission("/iot/devicedata/page")
    @GetMapping("/iot/devicedata/page")
    public CommonResult<Page<IotDeviceData>> page(IotDeviceDataPageParam iotDeviceDataPageParam) {
        return CommonResult.data(iotDeviceDataService.page(iotDeviceDataPageParam));
    }

    /**
     * 获取设备图表数据(按时间分组)
     *
     * @author jetox
     * @date  2025/12/11 10:20
     */
    @Operation(summary = "获取设备图表数据")
    @SaCheckPermission("/iot/devicedata/page")
    @GetMapping("/iot/devicedata/chartData")
    public CommonResult<List<JSONObject>> chartData(@RequestParam String deviceId) {
        return CommonResult.data(iotDeviceDataService.getChartData(deviceId));
    }

    /**
     * 添加设备数据
     *
     * @author jetox
     * @date  2025/12/11 07:27
     */
    @Operation(summary = "添加设备数据")
    @CommonLog("添加设备数据")
    @SaCheckPermission("/iot/devicedata/add")
    @PostMapping("/iot/devicedata/add")
    public CommonResult<String> add(@RequestBody @Valid IotDeviceDataAddParam iotDeviceDataAddParam) {
        iotDeviceDataService.add(iotDeviceDataAddParam);
        return CommonResult.ok();
    }

    /**
     * 编辑设备数据
     *
     * @author jetox
     * @date  2025/12/11 07:27
     */
    @Operation(summary = "编辑设备数据")
    @CommonLog("编辑设备数据")
    @SaCheckPermission("/iot/devicedata/edit")
    @PostMapping("/iot/devicedata/edit")
    public CommonResult<String> edit(@RequestBody @Valid IotDeviceDataEditParam iotDeviceDataEditParam) {
        iotDeviceDataService.edit(iotDeviceDataEditParam);
        return CommonResult.ok();
    }

    /**
     * 删除设备数据
     *
     * @author jetox
     * @date  2025/12/11 07:27
     */
    @Operation(summary = "删除设备数据")
    @CommonLog("删除设备数据")
    @SaCheckPermission("/iot/devicedata/delete")
    @PostMapping("/iot/devicedata/delete")
    public CommonResult<String> delete(@RequestBody @Valid @NotEmpty(message = "集合不能为空")
                                                   List<IotDeviceDataIdParam> iotDeviceDataIdParamList) {
        iotDeviceDataService.delete(iotDeviceDataIdParamList);
        return CommonResult.ok();
    }

    /**
     * 获取设备数据详情
     *
     * @author jetox
     * @date  2025/12/11 07:27
     */
    @Operation(summary = "获取设备数据详情")
    @SaCheckPermission("/iot/devicedata/detail")
    @GetMapping("/iot/devicedata/detail")
    public CommonResult<IotDeviceData> detail(@Valid IotDeviceDataIdParam iotDeviceDataIdParam) {
        return CommonResult.data(iotDeviceDataService.detail(iotDeviceDataIdParam));
    }

    /**
     * 下载设备数据导入模板
     *
     * @author jetox
     * @date  2025/12/11 07:27
     */
    @Operation(summary = "下载设备数据导入模板")
    @GetMapping(value = "/iot/devicedata/downloadImportTemplate", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
        iotDeviceDataService.downloadImportTemplate(response);
    }

    /**
     * 导入设备数据
     *
     * @author jetox
     * @date  2025/12/11 07:27
     */
    @Operation(summary = "导入设备数据")
    @CommonLog("导入设备数据")
    @SaCheckPermission("/iot/devicedata/importData")
    @PostMapping("/iot/devicedata/importData")
    public CommonResult<JSONObject> importData(@RequestPart("file") MultipartFile file) {
        return CommonResult.data(iotDeviceDataService.importData(file));
    }

    /**
     * 导出设备数据
     *
     * @author jetox
     * @date  2025/12/11 07:27
     */
    @Operation(summary = "导出设备数据")
    @SaCheckPermission("/iot/devicedata/exportData")
    @PostMapping(value = "/iot/devicedata/exportData", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void exportData(@RequestBody List<IotDeviceDataIdParam> iotDeviceDataIdParamList, HttpServletResponse response) throws IOException {
        iotDeviceDataService.exportData(iotDeviceDataIdParamList, response);
    }
}
