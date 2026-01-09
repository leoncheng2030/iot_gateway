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
package vip.xiaonuo.iot.modular.devicedriverrel.service;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import vip.xiaonuo.iot.modular.devicedriverrel.entity.IotDeviceDriverRel;
import vip.xiaonuo.iot.modular.devicedriverrel.param.IotDeviceDriverRelAddParam;
import vip.xiaonuo.iot.modular.devicedriverrel.param.IotDeviceDriverRelEditParam;
import vip.xiaonuo.iot.modular.devicedriverrel.param.IotDeviceDriverRelIdParam;
import vip.xiaonuo.iot.modular.devicedriverrel.param.IotDeviceDriverRelPageParam;
import java.io.IOException;
import java.util.List;

/**
 * 设备驱动Service接口
 *
 * @author jetox
 * @date  2025/12/13 09:46
 **/
public interface IotDeviceDriverRelService extends IService<IotDeviceDriverRel> {

    /**
     * 获取设备驱动分页
     *
     * @author jetox
     * @date  2025/12/13 09:46
     */
    Page<IotDeviceDriverRel> page(IotDeviceDriverRelPageParam iotDeviceDriverRelPageParam);

    /**
     * 添加设备驱动
     *
     * @author jetox
     * @date  2025/12/13 09:46
     */
    void add(IotDeviceDriverRelAddParam iotDeviceDriverRelAddParam);

    /**
     * 编辑设备驱动
     *
     * @author jetox
     * @date  2025/12/13 09:46
     */
    void edit(IotDeviceDriverRelEditParam iotDeviceDriverRelEditParam);

    /**
     * 删除设备驱动
     *
     * @author jetox
     * @date  2025/12/13 09:46
     */
    void delete(List<IotDeviceDriverRelIdParam> iotDeviceDriverRelIdParamList);

    /**
     * 获取设备驱动详情
     *
     * @author jetox
     * @date  2025/12/13 09:46
     */
    IotDeviceDriverRel detail(IotDeviceDriverRelIdParam iotDeviceDriverRelIdParam);

    /**
     * 获取设备驱动详情
     *
     * @author jetox
     * @date  2025/12/13 09:46
     **/
    IotDeviceDriverRel queryEntity(String id);

    /**
     * 下载设备驱动导入模板
     *
     * @author jetox
     * @date  2025/12/13 09:46
     */
    void downloadImportTemplate(HttpServletResponse response) throws IOException;

    /**
     * 导入设备驱动
     *
     * @author jetox
     * @date  2025/12/13 09:46
     **/
    JSONObject importData(MultipartFile file);

    /**
     * 导出设备驱动
     *
     * @author jetox
     * @date  2025/12/13 09:46
     */
    void exportData(List<IotDeviceDriverRelIdParam> iotDeviceDriverRelIdParamList, HttpServletResponse response) throws IOException;

    /**
     * 获取设备关联的驱动列表
     *
     * @param deviceId 设备ID
     * @return 驱动列表
     * @author jetox
     * @date 2025/12/13
     */
    List<IotDeviceDriverRel> listByDeviceId(String deviceId);

    /**
     * 绑定设备驱动
     *
     * @param deviceId 设备ID
     * @param driverId 驱动ID
     * @param deviceConfig 设备级配置JSON
     * @author jetox
     * @date 2025/12/13
     */
    void bindDriver(String deviceId, String driverId, String deviceConfig);

    /**
     * 解绑设备驱动
     *
     * @param deviceId 设备ID
     * @param driverId 驱动ID
     * @author jetox
     * @date 2025/12/13
     */
    void unbindDriver(String deviceId, String driverId);
}
