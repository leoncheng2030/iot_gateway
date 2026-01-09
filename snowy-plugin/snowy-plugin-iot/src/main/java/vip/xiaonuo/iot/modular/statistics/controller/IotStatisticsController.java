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
package vip.xiaonuo.iot.modular.statistics.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vip.xiaonuo.common.pojo.CommonResult;
import vip.xiaonuo.iot.modular.statistics.service.IotStatisticsService;

import java.util.Map;

/**
 * IoT统计数据控制器
 *
 * @author jetox
 * @date 2026/01/09 17:00
 */
@Tag(name = "IoT统计数据控制器")
@RestController
@RequestMapping("/iot/statistics")
public class IotStatisticsController {

    @Resource
    private IotStatisticsService iotStatisticsService;

    /**
     * 获取设备统计数据
     *
     * @author jetox
     * @date 2026/01/09 17:00
     */
    @Operation(summary = "获取设备统计数据")
    @GetMapping("/device")
    public CommonResult<Map<String, Object>> getDeviceStatistics() {
        return CommonResult.data(iotStatisticsService.getDeviceStatistics());
    }

    /**
     * 获取驱动统计数据
     *
     * @author jetox
     * @date 2026/01/09 17:00
     */
    @Operation(summary = "获取驱动统计数据")
    @GetMapping("/driver")
    public CommonResult<Map<String, Object>> getDriverStatistics() {
        return CommonResult.data(iotStatisticsService.getDriverStatistics());
    }

    /**
     * 获取告警统计数据
     *
     * @author jetox
     * @date 2026/01/09 17:00
     */
    @Operation(summary = "获取告警统计数据")
    @GetMapping("/alarm")
    public CommonResult<Map<String, Object>> getAlarmStatistics() {
        return CommonResult.data(iotStatisticsService.getAlarmStatistics());
    }

    /**
     * 获取数据采集趋势
     *
     * @param timeRange 时间范围：1h/6h/24h
     * @author jetox
     * @date 2026/01/09 17:00
     */
    @Operation(summary = "获取数据采集趋势")
    @GetMapping("/dataTrend")
    public CommonResult<Map<String, Object>> getDataTrend(@RequestParam(required = false, defaultValue = "1h") String timeRange) {
        return CommonResult.data(iotStatisticsService.getDataTrend(timeRange));
    }

    /**
     * 获取系统资源统计
     *
     * @author jetox
     * @date 2026/01/09 17:00
     */
    @Operation(summary = "获取系统资源统计")
    @GetMapping("/systemResource")
    public CommonResult<Map<String, Object>> getSystemResource() {
        return CommonResult.data(iotStatisticsService.getSystemResource());
    }
}
