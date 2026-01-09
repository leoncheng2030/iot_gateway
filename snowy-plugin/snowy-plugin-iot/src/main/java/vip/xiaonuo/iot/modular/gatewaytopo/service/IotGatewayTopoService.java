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
package vip.xiaonuo.iot.modular.gatewaytopo.service;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import vip.xiaonuo.iot.modular.gatewaytopo.entity.IotGatewayTopo;
import vip.xiaonuo.iot.modular.gatewaytopo.param.IotGatewayTopoAddParam;
import vip.xiaonuo.iot.modular.gatewaytopo.param.IotGatewayTopoEditParam;
import vip.xiaonuo.iot.modular.gatewaytopo.param.IotGatewayTopoIdParam;
import vip.xiaonuo.iot.modular.gatewaytopo.param.IotGatewayTopoPageParam;
import java.io.IOException;
import java.util.List;

/**
 * 网关拓扑Service接口
 *
 * @author jetox
 * @date  2025/12/11 07:29
 **/
public interface IotGatewayTopoService extends IService<IotGatewayTopo> {

    /**
     * 获取网关拓扑分页
     *
     * @author jetox
     * @date  2025/12/11 07:29
     */
    Page<IotGatewayTopo> page(IotGatewayTopoPageParam iotGatewayTopoPageParam);

    /**
     * 添加网关拓扑
     *
     * @author jetox
     * @date  2025/12/11 07:29
     */
    void add(IotGatewayTopoAddParam iotGatewayTopoAddParam);

    /**
     * 编辑网关拓扑
     *
     * @author jetox
     * @date  2025/12/11 07:29
     */
    void edit(IotGatewayTopoEditParam iotGatewayTopoEditParam);

    /**
     * 删除网关拓扑
     *
     * @author jetox
     * @date  2025/12/11 07:29
     */
    void delete(List<IotGatewayTopoIdParam> iotGatewayTopoIdParamList);

    /**
     * 获取网关拓扑详情
     *
     * @author jetox
     * @date  2025/12/11 07:29
     */
    IotGatewayTopo detail(IotGatewayTopoIdParam iotGatewayTopoIdParam);

    /**
     * 获取网关拓扑详情
     *
     * @author jetox
     * @date  2025/12/11 07:29
     **/
    IotGatewayTopo queryEntity(String id);

    /**
     * 下载网关拓扑导入模板
     *
     * @author jetox
     * @date  2025/12/11 07:29
     */
    void downloadImportTemplate(HttpServletResponse response) throws IOException;

    /**
     * 导入网关拓扑
     *
     * @author jetox
     * @date  2025/12/11 07:29
     **/
    JSONObject importData(MultipartFile file);

    /**
     * 导出网关拓扑
     *
     * @author jetox
     * @date  2025/12/11 07:29
     */
    void exportData(List<IotGatewayTopoIdParam> iotGatewayTopoIdParamList, HttpServletResponse response) throws IOException;

    /**
     * 绑定子设备
     *
     * @author yubaoshan
     * @date 2024/12/11 08:00
     */
    void bindSubDevice(String gatewayId, String subDeviceId);

    /**
     * 解绑子设备
     *
     * @author yubaoshan
     * @date 2024/12/11 08:00
     */
    void unbindSubDevice(String gatewayId, String subDeviceId);

    /**
     * 检查拓扑关系
     *
     * @author yubaoshan
     * @date 2024/12/11 08:00
     */
    boolean checkTopoRelation(String gatewayId, String subDeviceId);

    /**
     * 获取网关的所有子设备
     *
     * @author yubaoshan
     * @date 2024/12/11 08:00
     */
    List<IotGatewayTopo> getSubDevicesByGatewayId(String gatewayId);
}
