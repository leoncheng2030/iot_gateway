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
package vip.xiaonuo.iot.modular.protocol.service;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import vip.xiaonuo.iot.modular.protocol.entity.IotProtocol;
import vip.xiaonuo.iot.modular.protocol.param.IotProtocolAddParam;
import vip.xiaonuo.iot.modular.protocol.param.IotProtocolEditParam;
import vip.xiaonuo.iot.modular.protocol.param.IotProtocolIdParam;
import vip.xiaonuo.iot.modular.protocol.param.IotProtocolPageParam;
import java.io.IOException;
import java.util.List;

/**
 * 协议配置Service接口
 *
 * @author jetox
 * @date  2025/12/11 07:09
 **/
public interface IotProtocolService extends IService<IotProtocol> {

    /**
     * 获取协议配置分页
     *
     * @author jetox
     * @date  2025/12/11 07:09
     */
    Page<IotProtocol> page(IotProtocolPageParam iotProtocolPageParam);

    /**
     * 添加协议配置
     *
     * @author jetox
     * @date  2025/12/11 07:09
     */
    void add(IotProtocolAddParam iotProtocolAddParam);

    /**
     * 编辑协议配置
     *
     * @author jetox
     * @date  2025/12/11 07:09
     */
    void edit(IotProtocolEditParam iotProtocolEditParam);

    /**
     * 删除协议配置
     *
     * @author jetox
     * @date  2025/12/11 07:09
     */
    void delete(List<IotProtocolIdParam> iotProtocolIdParamList);

    /**
     * 获取协议配置详情
     *
     * @author jetox
     * @date  2025/12/11 07:09
     */
    IotProtocol detail(IotProtocolIdParam iotProtocolIdParam);

    /**
     * 获取协议配置详情
     *
     * @author jetox
     * @date  2025/12/11 07:09
     **/
    IotProtocol queryEntity(String id);

    /**
     * 下载协议配置导入模板
     *
     * @author jetox
     * @date  2025/12/11 07:09
     */
    void downloadImportTemplate(HttpServletResponse response) throws IOException;

    /**
     * 导入协议配置
     *
     * @author jetox
     * @date  2025/12/11 07:09
     **/
    JSONObject importData(MultipartFile file);

    /**
     * 导出协议配置
     *
     * @author jetox
     * @date  2025/12/11 07:09
     */
    void exportData(List<IotProtocolIdParam> iotProtocolIdParamList, HttpServletResponse response) throws IOException;
}
