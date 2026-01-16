package vip.xiaonuo.iot.modular.drivermarket.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vip.xiaonuo.common.pojo.CommonResult;
import vip.xiaonuo.iot.modular.drivermarket.entity.IotDriverMarket;
import vip.xiaonuo.iot.modular.drivermarket.param.IotDriverMarketIdParam;
import vip.xiaonuo.iot.modular.drivermarket.param.IotDriverMarketOperateParam;
import vip.xiaonuo.iot.modular.drivermarket.param.IotDriverMarketPageParam;
import vip.xiaonuo.iot.modular.drivermarket.service.IotDriverMarketService;

import java.util.List;

/**
 * 驱动市场控制器
 *
 * @author wqs
 * @date 2026/01/15
 */
@Tag(name = "驱动市场")
@RestController
@Validated
public class IotDriverMarketController {

    @Resource
    private IotDriverMarketService iotDriverMarketService;

    /**
     * 分页查询驱动市场
     */
    @Operation(summary = "分页查询驱动市场")
    @GetMapping("/iot/drivermarket/page")
    public CommonResult<Page<IotDriverMarket>> page(IotDriverMarketPageParam param) {
        return CommonResult.data(iotDriverMarketService.page(param));
    }

    /**
     * 获取驱动详情
     */
    @Operation(summary = "获取驱动详情")
    @GetMapping("/iot/drivermarket/detail")
    public CommonResult<IotDriverMarket> detail(@Valid IotDriverMarketIdParam param) {
        return CommonResult.data(iotDriverMarketService.detail(param));
    }

    /**
     * 安装驱动
     */
    @Operation(summary = "安装驱动")
    @SaCheckPermission("/iot/drivermarket/install")
    @PostMapping("/iot/drivermarket/install")
    public CommonResult<String> install(@RequestBody @Valid IotDriverMarketOperateParam param) {
        iotDriverMarketService.installDriver(param.getDriverType());
        return CommonResult.ok();
    }

    /**
     * 卸载驱动
     */
    @Operation(summary = "卸载驱动")
    @SaCheckPermission("/iot/drivermarket/uninstall")
    @PostMapping("/iot/drivermarket/uninstall")
    public CommonResult<String> uninstall(@RequestBody @Valid IotDriverMarketOperateParam param) {
        iotDriverMarketService.uninstallDriver(param.getDriverType());
        return CommonResult.ok();
    }

    /**
     * 启用驱动
     */
    @Operation(summary = "启用驱动")
    @SaCheckPermission("/iot/drivermarket/enable")
    @PostMapping("/iot/drivermarket/enable")
    public CommonResult<String> enable(@RequestBody @Valid IotDriverMarketOperateParam param) {
        iotDriverMarketService.enableDriver(param.getDriverType());
        return CommonResult.ok();
    }

    /**
     * 禁用驱动
     */
    @Operation(summary = "禁用驱动")
    @SaCheckPermission("/iot/drivermarket/disable")
    @PostMapping("/iot/drivermarket/disable")
    public CommonResult<String> disable(@RequestBody @Valid IotDriverMarketOperateParam param) {
        iotDriverMarketService.disableDriver(param.getDriverType());
        return CommonResult.ok();
    }

    /**
     * 上传驱动JAR
     */
    @Operation(summary = "上传驱动JAR")
    @SaCheckPermission("/iot/drivermarket/upload")
    @PostMapping("/iot/drivermarket/upload")
    public CommonResult<String> upload(@RequestPart("file") MultipartFile file) {
        iotDriverMarketService.uploadDriver(file);
        return CommonResult.ok();
    }

    /**
     * 同步内置驱动列表
     */
    @Operation(summary = "同步内置驱动列表")
    @SaCheckPermission("/iot/drivermarket/sync")
    @PostMapping("/iot/drivermarket/sync")
    public CommonResult<String> sync() {
        iotDriverMarketService.syncBuiltinDrivers();
        return CommonResult.ok();
    }

    /**
     * 获取已启用驱动列表
     */
    @Operation(summary = "获取已启用驱动列表")
    @GetMapping("/iot/drivermarket/enabled")
    public CommonResult<List<IotDriverMarket>> getEnabledDrivers() {
        return CommonResult.data(iotDriverMarketService.getEnabledDrivers());
    }
}
