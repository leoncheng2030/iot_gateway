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
package vip.xiaonuo.iot.modular.devicedata.service;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import vip.xiaonuo.iot.modular.devicedata.entity.IotDeviceData;
import vip.xiaonuo.iot.modular.devicedata.param.IotDeviceDataAddParam;
import vip.xiaonuo.iot.modular.devicedata.param.IotDeviceDataEditParam;
import vip.xiaonuo.iot.modular.devicedata.param.IotDeviceDataIdParam;
import vip.xiaonuo.iot.modular.devicedata.param.IotDeviceDataPageParam;
import java.io.IOException;
import java.util.List;

/**
 * 设备数据Service接口
 *
 * @author jetox
 * @date  2025/12/11 07:27
 **/
public interface IotDeviceDataService extends IService<IotDeviceData> {

    /**
     * 获取设备数据分页
     *
     * @author jetox
     * @date  2025/12/11 07:27
     */
    Page<IotDeviceData> page(IotDeviceDataPageParam iotDeviceDataPageParam);

    /**
     * 添加设备数据
     *
     * @author jetox
     * @date  2025/12/11 07:27
     */
    void add(IotDeviceDataAddParam iotDeviceDataAddParam);

    /**
     * 编辑设备数据
     *
     * @author jetox
     * @date  2025/12/11 07:27
     */
    void edit(IotDeviceDataEditParam iotDeviceDataEditParam);

    /**
     * 删除设备数据
     *
     * @author jetox
     * @date  2025/12/11 07:27
     */
    void delete(List<IotDeviceDataIdParam> iotDeviceDataIdParamList);

    /**
     * 获取设备数据详情
     *
     * @author jetox
     * @date  2025/12/11 07:27
     */
    IotDeviceData detail(IotDeviceDataIdParam iotDeviceDataIdParam);

    /**
     * 获取设备数据详情
     *
     * @author jetox
     * @date  2025/12/11 07:27
     **/
    IotDeviceData queryEntity(String id);

    /**
     * 下载设备数据导入模板
     *
     * @author jetox
     * @date  2025/12/11 07:27
     */
    void downloadImportTemplate(HttpServletResponse response) throws IOException;

    /**
     * 导入设备数据
     *
     * @author jetox
     * @date  2025/12/11 07:27
     **/
    JSONObject importData(MultipartFile file);

    /**
     * 导出设备数据
     *
     * @author jetox
     * @date  2025/12/11 07:27
     */
    void exportData(List<IotDeviceDataIdParam> iotDeviceDataIdParamList, HttpServletResponse response) throws IOException;

    /**
     * 获取设备图表数据(按时间分组)
     *
     * @author jetox
     * @date  2025/12/11 10:20
     */
    List<JSONObject> getChartData(String deviceId);
}
