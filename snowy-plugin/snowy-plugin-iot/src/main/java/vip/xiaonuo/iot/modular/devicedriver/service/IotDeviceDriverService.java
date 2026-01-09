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
package vip.xiaonuo.iot.modular.devicedriver.service;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import vip.xiaonuo.iot.modular.devicedriver.entity.IotDeviceDriver;
import vip.xiaonuo.iot.modular.devicedriver.param.IotDeviceDriverAddParam;
import vip.xiaonuo.iot.modular.devicedriver.param.IotDeviceDriverEditParam;
import vip.xiaonuo.iot.modular.devicedriver.param.IotDeviceDriverIdParam;
import vip.xiaonuo.iot.modular.devicedriver.param.IotDeviceDriverPageParam;
import java.io.IOException;
import java.util.List;

/**
 * 设备驱动配置表Service接口
 *
 * @author jetox
 * @date  2025/12/13 09:45
 **/
public interface IotDeviceDriverService extends IService<IotDeviceDriver> {

    /**
     * 获取设备驱动配置表分页
     *
     * @author jetox
     * @date  2025/12/13 09:45
     */
    Page<IotDeviceDriver> page(IotDeviceDriverPageParam iotDeviceDriverPageParam);

    /**
     * 添加设备驱动配置表
     *
     * @author jetox
     * @date  2025/12/13 09:45
     */
    void add(IotDeviceDriverAddParam iotDeviceDriverAddParam);

    /**
     * 编辑设备驱动配置表
     *
     * @author jetox
     * @date  2025/12/13 09:45
     */
    void edit(IotDeviceDriverEditParam iotDeviceDriverEditParam);

    /**
     * 删除设备驱动配置表
     *
     * @author jetox
     * @date  2025/12/13 09:45
     */
    void delete(List<IotDeviceDriverIdParam> iotDeviceDriverIdParamList);

    /**
     * 获取设备驱动配置表详情
     *
     * @author jetox
     * @date  2025/12/13 09:45
     */
    IotDeviceDriver detail(IotDeviceDriverIdParam iotDeviceDriverIdParam);

    /**
     * 获取设备驱动配置表详情
     *
     * @author jetox
     * @date  2025/12/13 09:45
     **/
    IotDeviceDriver queryEntity(String id);

    /**
     * 下载设备驱动配置表导入模板
     *
     * @author jetox
     * @date  2025/12/13 09:45
     */
    void downloadImportTemplate(HttpServletResponse response) throws IOException;

    /**
     * 导入设备驱动配置表
     *
     * @author jetox
     * @date  2025/12/13 09:45
     **/
    JSONObject importData(MultipartFile file);

    /**
     * 导出设备驱动配置表
     *
     * @author jetox
     * @date  2025/12/13 09:45
     */
    void exportData(List<IotDeviceDriverIdParam> iotDeviceDriverIdParamList, HttpServletResponse response) throws IOException;

    /**
     * 启动驱动服务
     *
     * @author jetox
     * @date 2025/12/13
     */
    void startDriver(String driverId);

    /**
     * 停止驱动服务
     *
     * @author jetox
     * @date 2025/12/13
     */
    void stopDriver(String driverId);

    /**
     * 重启驱动服务
     *
     * @author jetox
     * @date 2025/12/13
     */
    void restartDriver(String driverId);

    /**
     * 获取驱动运行状态
     *
     * @author jetox
     * @date 2025/12/13
     * @return true-运行中 false-已停止
     */
    boolean isDriverRunning(String driverId);
    
    /**
     * 获取驱动详细状态(包含运行时长)
     *
     * @author jetox
     * @date 2025/12/13
     * @return 状态信息
     */
    JSONObject getDriverStatus(String driverId);
}
