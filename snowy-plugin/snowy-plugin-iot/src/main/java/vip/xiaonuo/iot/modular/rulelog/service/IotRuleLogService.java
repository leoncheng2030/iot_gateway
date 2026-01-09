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
package vip.xiaonuo.iot.modular.rulelog.service;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import vip.xiaonuo.iot.modular.rulelog.entity.IotRuleLog;
import vip.xiaonuo.iot.modular.rulelog.param.IotRuleLogAddParam;
import vip.xiaonuo.iot.modular.rulelog.param.IotRuleLogEditParam;
import vip.xiaonuo.iot.modular.rulelog.param.IotRuleLogIdParam;
import vip.xiaonuo.iot.modular.rulelog.param.IotRuleLogPageParam;
import java.io.IOException;
import java.util.List;

/**
 * 规则执行日志表Service接口
 *
 * @author jetox
 * @date  2025/12/11 07:39
 **/
public interface IotRuleLogService extends IService<IotRuleLog> {

    /**
     * 获取规则执行日志表分页
     *
     * @author jetox
     * @date  2025/12/11 07:39
     */
    Page<IotRuleLog> page(IotRuleLogPageParam iotRuleLogPageParam);

    /**
     * 添加规则执行日志表
     *
     * @author jetox
     * @date  2025/12/11 07:39
     */
    void add(IotRuleLogAddParam iotRuleLogAddParam);

    /**
     * 编辑规则执行日志表
     *
     * @author jetox
     * @date  2025/12/11 07:39
     */
    void edit(IotRuleLogEditParam iotRuleLogEditParam);

    /**
     * 删除规则执行日志表
     *
     * @author jetox
     * @date  2025/12/11 07:39
     */
    void delete(List<IotRuleLogIdParam> iotRuleLogIdParamList);

    /**
     * 获取规则执行日志表详情
     *
     * @author jetox
     * @date  2025/12/11 07:39
     */
    IotRuleLog detail(IotRuleLogIdParam iotRuleLogIdParam);

    /**
     * 获取规则执行日志表详情
     *
     * @author jetox
     * @date  2025/12/11 07:39
     **/
    IotRuleLog queryEntity(String id);

    /**
     * 下载规则执行日志表导入模板
     *
     * @author jetox
     * @date  2025/12/11 07:39
     */
    void downloadImportTemplate(HttpServletResponse response) throws IOException;

    /**
     * 导入规则执行日志表
     *
     * @author jetox
     * @date  2025/12/11 07:39
     **/
    JSONObject importData(MultipartFile file);

    /**
     * 导出规则执行日志表
     *
     * @author jetox
     * @date  2025/12/11 07:39
     */
    void exportData(List<IotRuleLogIdParam> iotRuleLogIdParamList, HttpServletResponse response) throws IOException;
}
