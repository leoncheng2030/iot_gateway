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
package vip.xiaonuo.iot.modular.thingmodel.service;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import vip.xiaonuo.iot.modular.thingmodel.entity.IotThingModel;
import vip.xiaonuo.iot.modular.thingmodel.param.*;

import java.io.IOException;
import java.util.List;

/**
 * 物模型Service接口
 *
 * @author jetox
 * @date  2025/12/11 09:08
 **/
public interface IotThingModelService extends IService<IotThingModel> {

    /**
     * 获取物模型分页
     *
     * @author jetox
     * @date  2025/12/11 09:08
     */
    Page<IotThingModel> page(IotThingModelPageParam iotThingModelPageParam);

    /**
     * 添加物模型
     *
     * @author jetox
     * @date  2025/12/11 09:08
     */
    void add(IotThingModelAddParam iotThingModelAddParam);

    /**
     * 编辑物模型
     *
     * @author jetox
     * @date  2025/12/11 09:08
     */
    void edit(IotThingModelEditParam iotThingModelEditParam);

    /**
     * 删除物模型
     *
     * @author jetox
     * @date  2025/12/11 09:08
     */
    void delete(List<IotThingModelIdParam> iotThingModelIdParamList);

    /**
     * 获取物模型详情
     *
     * @author jetox
     * @date  2025/12/11 09:08
     */
    IotThingModel detail(IotThingModelIdParam iotThingModelIdParam);

    /**
     * 获取物模型详情
     *
     * @author jetox
     * @date  2025/12/11 09:08
     **/
    IotThingModel queryEntity(String id);

    /**
     * 根据产品ID获取物模型列表
     *
     * @author jetox
     * @date  2025/12/11 09:26
     */
    List<IotThingModel> listByProduct(IotThingModelListParam iotThingModelListParam);

    /**
     * 下载物模型导入模板
     *
     * @author jetox
     * @date  2025/12/11 09:08
     */
    void downloadImportTemplate(HttpServletResponse response) throws IOException;

    /**
     * 导入物模型
     *
     * @author jetox
     * @date  2025/12/11 09:08
     **/
    JSONObject importData(MultipartFile file);

    /**
     * 导出物模型
     *
     * @author jetox
     * @date  2025/12/11 09:08
     */
    void exportData(List<IotThingModelIdParam> iotThingModelIdParamList, HttpServletResponse response) throws IOException;
}
