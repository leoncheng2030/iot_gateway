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
package vip.xiaonuo.iot.modular.deviceshadow.service;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import vip.xiaonuo.iot.modular.deviceshadow.entity.IotDeviceShadow;
import vip.xiaonuo.iot.modular.deviceshadow.param.IotDeviceShadowAddParam;
import vip.xiaonuo.iot.modular.deviceshadow.param.IotDeviceShadowEditParam;
import vip.xiaonuo.iot.modular.deviceshadow.param.IotDeviceShadowIdParam;
import vip.xiaonuo.iot.modular.deviceshadow.param.IotDeviceShadowPageParam;
import java.io.IOException;
import java.util.List;

/**
 * 设备影子表Service接口
 *
 * @author jetox
 * @date  2025/12/11 07:28
 **/
public interface IotDeviceShadowService extends IService<IotDeviceShadow> {

    /**
     * 获取设备影子表分页
     *
     * @author jetox
     * @date  2025/12/11 07:28
     */
    Page<IotDeviceShadow> page(IotDeviceShadowPageParam iotDeviceShadowPageParam);

    /**
     * 添加设备影子表
     *
     * @author jetox
     * @date  2025/12/11 07:28
     */
    void add(IotDeviceShadowAddParam iotDeviceShadowAddParam);

    /**
     * 编辑设备影子表
     *
     * @author jetox
     * @date  2025/12/11 07:28
     */
    void edit(IotDeviceShadowEditParam iotDeviceShadowEditParam);

    /**
     * 删除设备影子表
     *
     * @author jetox
     * @date  2025/12/11 07:28
     */
    void delete(List<IotDeviceShadowIdParam> iotDeviceShadowIdParamList);

    /**
     * 获取设备影子表详情
     *
     * @author jetox
     * @date  2025/12/11 07:28
     */
    IotDeviceShadow detail(IotDeviceShadowIdParam iotDeviceShadowIdParam);

    /**
     * 获取设备影子表详情
     *
     * @author jetox
     * @date  2025/12/11 07:28
     **/
    IotDeviceShadow queryEntity(String id);

    /**
     * 下载设备影子表导入模板
     *
     * @author jetox
     * @date  2025/12/11 07:28
     */
    void downloadImportTemplate(HttpServletResponse response) throws IOException;

    /**
     * 导入设备影子表
     *
     * @author jetox
     * @date  2025/12/11 07:28
     **/
    JSONObject importData(MultipartFile file);

    /**
     * 导出设备影子表
     *
     * @author jetox
     * @date  2025/12/11 07:28
     */
    void exportData(List<IotDeviceShadowIdParam> iotDeviceShadowIdParamList, HttpServletResponse response) throws IOException;

    IotDeviceShadow getByDeviceId(String id);
}
