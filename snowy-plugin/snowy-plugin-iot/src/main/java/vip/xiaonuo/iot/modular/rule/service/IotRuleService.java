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
package vip.xiaonuo.iot.modular.rule.service;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import vip.xiaonuo.iot.modular.rule.entity.IotRule;
import vip.xiaonuo.iot.modular.rule.param.IotRuleAddParam;
import vip.xiaonuo.iot.modular.rule.param.IotRuleEditParam;
import vip.xiaonuo.iot.modular.rule.param.IotRuleIdParam;
import vip.xiaonuo.iot.modular.rule.param.IotRulePageParam;
import java.io.IOException;
import java.util.List;

/**
 * 规则引擎Service接口
 *
 * @author jetox
 * @date  2025/12/11 07:32
 **/
public interface IotRuleService extends IService<IotRule> {

    /**
     * 获取规则引擎分页
     *
     * @author jetox
     * @date  2025/12/11 07:32
     */
    Page<IotRule> page(IotRulePageParam iotRulePageParam);

    /**
     * 添加规则引擎
     *
     * @author jetox
     * @date  2025/12/11 07:32
     */
    void add(IotRuleAddParam iotRuleAddParam);

    /**
     * 编辑规则引擎
     *
     * @author jetox
     * @date  2025/12/11 07:32
     */
    void edit(IotRuleEditParam iotRuleEditParam);

    /**
     * 删除规则引擎
     *
     * @author jetox
     * @date  2025/12/11 07:32
     */
    void delete(List<IotRuleIdParam> iotRuleIdParamList);

    /**
     * 获取规则引擎详情
     *
     * @author jetox
     * @date  2025/12/11 07:32
     */
    IotRule detail(IotRuleIdParam iotRuleIdParam);

    /**
     * 获取规则引擎详情
     *
     * @author jetox
     * @date  2025/12/11 07:32
     **/
    IotRule queryEntity(String id);

    /**
     * 下载规则引擎导入模板
     *
     * @author jetox
     * @date  2025/12/11 07:32
     */
    void downloadImportTemplate(HttpServletResponse response) throws IOException;

    /**
     * 导入规则引擎
     *
     * @author jetox
     * @date  2025/12/11 07:32
     **/
    JSONObject importData(MultipartFile file);

    /**
     * 导出规则引擎
     *
     * @author jetox
     * @date  2025/12/11 07:32
     */
    void exportData(List<IotRuleIdParam> iotRuleIdParamList, HttpServletResponse response) throws IOException;
}
