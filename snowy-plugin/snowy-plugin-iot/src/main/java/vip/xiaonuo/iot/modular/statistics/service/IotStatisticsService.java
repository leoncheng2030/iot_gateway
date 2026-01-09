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
package vip.xiaonuo.iot.modular.statistics.service;

import java.util.Map;

/**
 * IoT统计数据Service接口
 *
 * @author jetox
 * @date 2026/01/09 17:00
 */
public interface IotStatisticsService {

    /**
     * 获取设备统计数据
     *
     * @return 设备统计数据
     * @author jetox
     * @date 2026/01/09 17:00
     */
    Map<String, Object> getDeviceStatistics();

    /**
     * 获取驱动统计数据
     *
     * @return 驱动统计数据
     * @author jetox
     * @date 2026/01/09 17:00
     */
    Map<String, Object> getDriverStatistics();

    /**
     * 获取告警统计数据
     *
     * @return 告警统计数据
     * @author jetox
     * @date 2026/01/09 17:00
     */
    Map<String, Object> getAlarmStatistics();

    /**
     * 获取数据采集趋势
     *
     * @param timeRange 时间范围：1h/6h/24h
     * @return 数据采集趋势
     * @author jetox
     * @date 2026/01/09 17:00
     */
    Map<String, Object> getDataTrend(String timeRange);

    /**
     * 获取系统资源统计
     *
     * @return 系统资源统计
     * @author jetox
     * @date 2026/01/09 17:00
     */
    Map<String, Object> getSystemResource();
}
