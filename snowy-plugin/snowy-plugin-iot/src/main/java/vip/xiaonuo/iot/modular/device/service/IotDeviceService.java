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
package vip.xiaonuo.iot.modular.device.service;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import vip.xiaonuo.iot.modular.device.entity.IotDevice;
import vip.xiaonuo.iot.modular.device.param.IotDeviceAddParam;
import vip.xiaonuo.iot.modular.device.param.IotDeviceEditParam;
import vip.xiaonuo.iot.modular.device.param.IotDeviceIdParam;
import vip.xiaonuo.iot.modular.device.param.IotDevicePageParam;
import vip.xiaonuo.iot.modular.device.param.IotDeviceCommandParam;
import vip.xiaonuo.iot.modular.device.param.IotDevicePropertySetParam;
import vip.xiaonuo.iot.modular.device.param.IotDeviceServiceParam;
import java.io.IOException;
import java.util.List;

/**
 * 设备Service接口
 *
 * @author jetox
 * @date  2025/12/11 07:24
 **/
public interface IotDeviceService extends IService<IotDevice> {

    /**
     * 获取设备分页
     *
     * @author jetox
     * @date  2025/12/11 07:24
     */
    Page<IotDevice> page(IotDevicePageParam iotDevicePageParam);

    /**
     * 添加设备
     *
     * @author jetox
     * @date  2025/12/11 07:24
     * @return 设备ID
     */
    String add(IotDeviceAddParam iotDeviceAddParam);

    /**
     * 编辑设备
     *
     * @author jetox
     * @date  2025/12/11 07:24
     */
    void edit(IotDeviceEditParam iotDeviceEditParam);

    /**
     * 删除设备
     *
     * @author jetox
     * @date  2025/12/11 07:24
     */
    void delete(List<IotDeviceIdParam> iotDeviceIdParamList);

    /**
     * 获取设备详情
     *
     * @author jetox
     * @date  2025/12/11 07:24
     */
    IotDevice detail(IotDeviceIdParam iotDeviceIdParam);

    /**
     * 获取设备详情
     *
     * @author jetox
     * @date  2025/12/11 07:24
     **/
    IotDevice queryEntity(String id);

    /**
     * 下载设备导入模板
     *
     * @author jetox
     * @date  2025/12/11 07:24
     */
    void downloadImportTemplate(HttpServletResponse response) throws IOException;

    /**
     * 导入设备
     *
     * @author jetox
     * @date  2025/12/11 07:24
     **/
    JSONObject importData(MultipartFile file);

    /**
     * 导出设备
     *
     * @author jetox
     * @date  2025/12/11 07:24
     */
    void exportData(List<IotDeviceIdParam> iotDeviceIdParamList, HttpServletResponse response) throws IOException;

    /**
     * 设置设备属性
     *
     * @author yubaoshan
     * @date  2025/12/11 16:35
     */
    void setProperty(IotDevicePropertySetParam param);

    /**
     * 下发设备指令
     *
     * @author yubaoshan
     * @date  2025/12/11 16:35
     */
    void sendCommand(IotDeviceCommandParam param);

    /**
     * 调用设备服务
     *
     * @author jetox
     * @date  2025/12/12 15:24
     */
    void invokeService(IotDeviceServiceParam param);
}
