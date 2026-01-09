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
package vip.xiaonuo.iot.modular.product.service;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import vip.xiaonuo.iot.modular.product.entity.IotProduct;
import vip.xiaonuo.iot.modular.product.param.IotProductAddParam;
import vip.xiaonuo.iot.modular.product.param.IotProductEditParam;
import vip.xiaonuo.iot.modular.product.param.IotProductIdParam;
import vip.xiaonuo.iot.modular.product.param.IotProductPageParam;
import java.io.IOException;
import java.util.List;

/**
 * 产品Service接口
 *
 * @author jetox
 * @date  2025/12/11 06:57
 **/
public interface IotProductService extends IService<IotProduct> {

    /**
     * 获取产品分页
     *
     * @author jetox
     * @date  2025/12/11 06:57
     */
    Page<IotProduct> page(IotProductPageParam iotProductPageParam);

    /**
     * 添加产品
     *
     * @author jetox
     * @date  2025/12/11 06:57
     */
    void add(IotProductAddParam iotProductAddParam);

    /**
     * 编辑产品
     *
     * @author jetox
     * @date  2025/12/11 06:57
     */
    void edit(IotProductEditParam iotProductEditParam);

    /**
     * 删除产品
     *
     * @author jetox
     * @date  2025/12/11 06:57
     */
    void delete(List<IotProductIdParam> iotProductIdParamList);

    /**
     * 获取产品详情
     *
     * @author jetox
     * @date  2025/12/11 06:57
     */
    IotProduct detail(IotProductIdParam iotProductIdParam);

    /**
     * 获取产品详情
     *
     * @author jetox
     * @date  2025/12/11 06:57
     **/
    IotProduct queryEntity(String id);

    /**
     * 下载产品导入模板
     *
     * @author jetox
     * @date  2025/12/11 06:57
     */
    void downloadImportTemplate(HttpServletResponse response) throws IOException;

    /**
     * 导入产品
     *
     * @author jetox
     * @date  2025/12/11 06:57
     **/
    JSONObject importData(MultipartFile file);

    /**
     * 导出产品
     *
     * @author jetox
     * @date  2025/12/11 06:57
     */
    void exportData(List<IotProductIdParam> iotProductIdParamList, HttpServletResponse response) throws IOException;
}
