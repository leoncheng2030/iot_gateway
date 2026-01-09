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
package vip.xiaonuo.iot.modular.northboundstatistics.service;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import vip.xiaonuo.iot.modular.northboundstatistics.entity.IotNorthboundStatistics;
import vip.xiaonuo.iot.modular.northboundstatistics.param.IotNorthboundStatisticsAddParam;
import vip.xiaonuo.iot.modular.northboundstatistics.param.IotNorthboundStatisticsEditParam;
import vip.xiaonuo.iot.modular.northboundstatistics.param.IotNorthboundStatisticsIdParam;
import vip.xiaonuo.iot.modular.northboundstatistics.param.IotNorthboundStatisticsPageParam;
import java.io.IOException;
import java.util.List;

/**
 * 北向推送统计表Service接口
 *
 * @author jetox
 * @date  2026/01/08 10:26
 **/
public interface IotNorthboundStatisticsService extends IService<IotNorthboundStatistics> {

    /**
     * 获取北向推送统计表分页
     *
     * @author jetox
     * @date  2026/01/08 10:26
     */
    Page<IotNorthboundStatistics> page(IotNorthboundStatisticsPageParam iotNorthboundStatisticsPageParam);

    /**
     * 添加北向推送统计表
     *
     * @author jetox
     * @date  2026/01/08 10:26
     */
    void add(IotNorthboundStatisticsAddParam iotNorthboundStatisticsAddParam);

    /**
     * 编辑北向推送统计表
     *
     * @author jetox
     * @date  2026/01/08 10:26
     */
    void edit(IotNorthboundStatisticsEditParam iotNorthboundStatisticsEditParam);

    /**
     * 删除北向推送统计表
     *
     * @author jetox
     * @date  2026/01/08 10:26
     */
    void delete(List<IotNorthboundStatisticsIdParam> iotNorthboundStatisticsIdParamList);

    /**
     * 获取北向推送统计表详情
     *
     * @author jetox
     * @date  2026/01/08 10:26
     */
    IotNorthboundStatistics detail(IotNorthboundStatisticsIdParam iotNorthboundStatisticsIdParam);

    /**
     * 获取北向推送统计表详情
     *
     * @author jetox
     * @date  2026/01/08 10:26
     **/
    IotNorthboundStatistics queryEntity(String id);

    /**
     * 下载北向推送统计表导入模板
     *
     * @author jetox
     * @date  2026/01/08 10:26
     */
    void downloadImportTemplate(HttpServletResponse response) throws IOException;

    /**
     * 导入北向推送统计表
     *
     * @author jetox
     * @date  2026/01/08 10:26
     **/
    JSONObject importData(MultipartFile file);

    /**
     * 导出北向推送统计表
     *
     * @author jetox
     * @date  2026/01/08 10:26
     */
    void exportData(List<IotNorthboundStatisticsIdParam> iotNorthboundStatisticsIdParamList, HttpServletResponse response) throws IOException;

    /**
     * 更新推送统计数据
     *
     * @param configId 配置ID
     * @param success 是否成功
     * @param costTime 耗时（毫秒）
     * @author jetox
     * @date  2026/01/08 10:40
     */
    void updateStatistics(String configId, boolean success, int costTime);
}
