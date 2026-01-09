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
package vip.xiaonuo.iot.modular.devicegroup.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.lang.tree.Tree;
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
import vip.xiaonuo.iot.modular.devicegroup.entity.IotDeviceGroup;
import vip.xiaonuo.iot.modular.devicegroup.param.IotDeviceGroupAddParam;
import vip.xiaonuo.iot.modular.devicegroup.param.IotDeviceGroupEditParam;
import vip.xiaonuo.iot.modular.devicegroup.param.IotDeviceGroupIdParam;
import vip.xiaonuo.iot.modular.devicegroup.param.IotDeviceGroupPageParam;
import vip.xiaonuo.iot.modular.devicegroup.service.IotDeviceGroupService;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.io.IOException;
import java.util.List;

/**
 * 设备分组表控制器
 *
 * @author jetox
 * @date  2025/12/13 18:30
 */
@Tag(name = "设备分组表控制器")
@RestController
@Validated
public class IotDeviceGroupController {

    @Resource
    private IotDeviceGroupService iotDeviceGroupService;

    /**
     * 获取设备分组表分页
     *
     * @author jetox
     * @date  2025/12/13 18:30
     */
    @Operation(summary = "获取设备分组表分页")
    @SaCheckPermission("/iot/devicegroup/page")
    @GetMapping("/iot/devicegroup/page")
    public CommonResult<Page<IotDeviceGroup>> page(IotDeviceGroupPageParam iotDeviceGroupPageParam) {
        return CommonResult.data(iotDeviceGroupService.page(iotDeviceGroupPageParam));
    }

    /**
     * 获取设备分组树
     *
     * @author jetox
     * @date  2025/12/13 18:30
     */
    @Operation(summary = "获取设备分组树")
    @SaCheckPermission("/iot/devicegroup/tree")
    @GetMapping("/iot/devicegroup/tree")
    public CommonResult<List<Tree<String>>> tree() {
        return CommonResult.data(iotDeviceGroupService.tree());
    }

    /**
     * 添加设备分组表
     *
     * @author jetox
     * @date  2025/12/13 18:30
     */
    @Operation(summary = "添加设备分组表")
    @CommonLog("添加设备分组表")
    @SaCheckPermission("/iot/devicegroup/add")
    @PostMapping("/iot/devicegroup/add")
    public CommonResult<String> add(@RequestBody @Valid IotDeviceGroupAddParam iotDeviceGroupAddParam) {
        iotDeviceGroupService.add(iotDeviceGroupAddParam);
        return CommonResult.ok();
    }

    /**
     * 编辑设备分组表
     *
     * @author jetox
     * @date  2025/12/13 18:30
     */
    @Operation(summary = "编辑设备分组表")
    @CommonLog("编辑设备分组表")
    @SaCheckPermission("/iot/devicegroup/edit")
    @PostMapping("/iot/devicegroup/edit")
    public CommonResult<String> edit(@RequestBody @Valid IotDeviceGroupEditParam iotDeviceGroupEditParam) {
        iotDeviceGroupService.edit(iotDeviceGroupEditParam);
        return CommonResult.ok();
    }

    /**
     * 删除设备分组表
     *
     * @author jetox
     * @date  2025/12/13 18:30
     */
    @Operation(summary = "删除设备分组表")
    @CommonLog("删除设备分组表")
    @SaCheckPermission("/iot/devicegroup/delete")
    @PostMapping("/iot/devicegroup/delete")
    public CommonResult<String> delete(@RequestBody @Valid @NotEmpty(message = "集合不能为空")
                                                   List<IotDeviceGroupIdParam> iotDeviceGroupIdParamList) {
        iotDeviceGroupService.delete(iotDeviceGroupIdParamList);
        return CommonResult.ok();
    }

    /**
     * 获取设备分组表详情
     *
     * @author jetox
     * @date  2025/12/13 18:30
     */
    @Operation(summary = "获取设备分组表详情")
    @SaCheckPermission("/iot/devicegroup/detail")
    @GetMapping("/iot/devicegroup/detail")
    public CommonResult<IotDeviceGroup> detail(@Valid IotDeviceGroupIdParam iotDeviceGroupIdParam) {
        return CommonResult.data(iotDeviceGroupService.detail(iotDeviceGroupIdParam));
    }

    /**
     * 下载设备分组表导入模板
     *
     * @author jetox
     * @date  2025/12/13 18:30
     */
    @Operation(summary = "下载设备分组表导入模板")
    @GetMapping(value = "/iot/devicegroup/downloadImportTemplate", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
        iotDeviceGroupService.downloadImportTemplate(response);
    }

    /**
     * 导入设备分组表
     *
     * @author jetox
     * @date  2025/12/13 18:30
     */
    @Operation(summary = "导入设备分组表")
    @CommonLog("导入设备分组表")
    @SaCheckPermission("/iot/devicegroup/importData")
    @PostMapping("/iot/devicegroup/importData")
    public CommonResult<JSONObject> importData(@RequestPart("file") MultipartFile file) {
        return CommonResult.data(iotDeviceGroupService.importData(file));
    }

    /**
     * 导出设备分组表
     *
     * @author jetox
     * @date  2025/12/13 18:30
     */
    @Operation(summary = "导出设备分组表")
    @SaCheckPermission("/iot/devicegroup/exportData")
    @PostMapping(value = "/iot/devicegroup/exportData", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void exportData(@RequestBody List<IotDeviceGroupIdParam> iotDeviceGroupIdParamList, HttpServletResponse response) throws IOException {
        iotDeviceGroupService.exportData(iotDeviceGroupIdParamList, response);
    }

    /**
     * 批量关联设备到分组
     *
     * @author jetox
     * @date 2025/12/13
     */
    @Operation(summary = "批量关联设备到分组")
    @CommonLog("批量关联设备到分组")
    @SaCheckPermission("/iot/devicegroup/batchRelateDevices")
    @PostMapping("/iot/devicegroup/batchRelateDevices")
    public CommonResult<String> batchRelateDevices(@RequestBody JSONObject param) {
        String groupId = param.getStr("groupId");
        List<String> deviceIds = param.getBeanList("deviceIds", String.class);
        iotDeviceGroupService.batchRelateDevices(groupId, deviceIds);
        return CommonResult.ok();
    }

    /**
     * 批量移除分组下的设备
     *
     * @author jetox
     * @date 2025/12/13
     */
    @Operation(summary = "批量移除分组下的设备")
    @CommonLog("批量移除分组下的设备")
    @SaCheckPermission("/iot/devicegroup/batchRemoveDevices")
    @PostMapping("/iot/devicegroup/batchRemoveDevices")
    public CommonResult<String> batchRemoveDevices(@RequestBody JSONObject param) {
        String groupId = param.getStr("groupId");
        List<String> deviceIds = param.getBeanList("deviceIds", String.class);
        iotDeviceGroupService.batchRemoveDevices(groupId, deviceIds);
        return CommonResult.ok();
    }

    /**
     * 获取分组下的设备ID列表
     *
     * @author jetox
     * @date 2025/12/13
     */
    @Operation(summary = "获取分组下的设备ID列表")
    @SaCheckPermission("/iot/devicegroup/getDeviceIds")
    @GetMapping("/iot/devicegroup/getDeviceIds")
    public CommonResult<List<String>> getDeviceIds(@RequestParam String groupId) {
        return CommonResult.data(iotDeviceGroupService.getDeviceIdsByGroupId(groupId));
    }

    /**
     * 获取设备关联的所有分组ID
     *
     * @author jetox
     * @date 2025/12/13
     */
    @Operation(summary = "获取设备关联的所有分组ID")
    @SaCheckPermission("/iot/devicegroup/getGroupIdsByDeviceId")
    @GetMapping("/iot/devicegroup/getGroupIdsByDeviceId")
    public CommonResult<List<String>> getGroupIdsByDeviceId(@RequestParam String deviceId) {
        return CommonResult.data(iotDeviceGroupService.getGroupIdsByDeviceId(deviceId));
    }

    /**
     * 同步设备分组（全量替换）
     *
     * @author jetox
     * @date 2025/12/13
     */
    @Operation(summary = "同步设备分组")
    @CommonLog("同步设备分组")
    @SaCheckPermission("/iot/devicegroup/syncDeviceGroups")
    @PostMapping("/iot/devicegroup/syncDeviceGroups")
    public CommonResult<String> syncDeviceGroups(@RequestBody JSONObject param) {
        String deviceId = param.getStr("deviceId");
        List<String> groupIds = param.getBeanList("groupIds", String.class);
        iotDeviceGroupService.syncDeviceGroups(deviceId, groupIds);
        return CommonResult.ok();
    }

    /**
     * 获取分组的已关联设备列表
     *
     * @author jetox
     * @date 2025/12/13
     */
    @Operation(summary = "获取分组的已关联设备列表")
    @SaCheckPermission("/iot/devicegroup/getRelatedDevices")
    @GetMapping("/iot/devicegroup/getRelatedDevices")
    public CommonResult<Page<IotDevice>> getRelatedDevices(
            @RequestParam String groupId,
            @RequestParam(required = false) String searchKey,
            @RequestParam(required = false, defaultValue = "1000") Integer pageSize) {
        return CommonResult.data(iotDeviceGroupService.getRelatedDevices(groupId, searchKey, pageSize));
    }

    /**
     * 获取未被任何分组关联的设备列表
     *
     * @author jetox
     * @date 2025/12/13
     */
    @Operation(summary = "获取未关联设备列表")
    @SaCheckPermission("/iot/devicegroup/getUnrelatedDevices")
    @GetMapping("/iot/devicegroup/getUnrelatedDevices")
    public CommonResult<Page<IotDevice>> getUnrelatedDevices(
            @RequestParam(required = false) String searchKey,
            @RequestParam(required = false, defaultValue = "1000") Integer pageSize) {
        return CommonResult.data(iotDeviceGroupService.getUnrelatedDevices(searchKey, pageSize));
    }
}
