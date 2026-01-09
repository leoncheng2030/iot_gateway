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
package vip.xiaonuo.iot.modular.northbounddevicerel.service;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import vip.xiaonuo.iot.modular.northbounddevicerel.entity.IotNorthboundDeviceRel;
import vip.xiaonuo.iot.modular.northbounddevicerel.param.IotNorthboundDeviceRelAddParam;
import vip.xiaonuo.iot.modular.northbounddevicerel.param.IotNorthboundDeviceRelEditParam;
import vip.xiaonuo.iot.modular.northbounddevicerel.param.IotNorthboundDeviceRelIdParam;
import vip.xiaonuo.iot.modular.northbounddevicerel.param.IotNorthboundDeviceRelPageParam;
import java.io.IOException;
import java.util.List;

/**
 * 北向推送设备关联表Service接口
 *
 * @author jetox
 * @date  2026/01/08 10:25
 **/
public interface IotNorthboundDeviceRelService extends IService<IotNorthboundDeviceRel> {

    /**
     * 获取北向推送设备关联表分页
     *
     * @author jetox
     * @date  2026/01/08 10:25
     */
    Page<IotNorthboundDeviceRel> page(IotNorthboundDeviceRelPageParam iotNorthboundDeviceRelPageParam);

    /**
     * 添加北向推送设备关联表
     *
     * @author jetox
     * @date  2026/01/08 10:25
     */
    void add(IotNorthboundDeviceRelAddParam iotNorthboundDeviceRelAddParam);

    /**
     * 编辑北向推送设备关联表
     *
     * @author jetox
     * @date  2026/01/08 10:25
     */
    void edit(IotNorthboundDeviceRelEditParam iotNorthboundDeviceRelEditParam);

    /**
     * 删除北向推送设备关联表
     *
     * @author jetox
     * @date  2026/01/08 10:25
     */
    void delete(List<IotNorthboundDeviceRelIdParam> iotNorthboundDeviceRelIdParamList);

    /**
     * 获取北向推送设备关联表详情
     *
     * @author jetox
     * @date  2026/01/08 10:25
     */
    IotNorthboundDeviceRel detail(IotNorthboundDeviceRelIdParam iotNorthboundDeviceRelIdParam);

    /**
     * 获取北向推送设备关联表详情
     *
     * @author jetox
     * @date  2026/01/08 10:25
     **/
    IotNorthboundDeviceRel queryEntity(String id);

    /**
     * 下载北向推送设备关联表导入模板
     *
     * @author jetox
     * @date  2026/01/08 10:25
     */
    void downloadImportTemplate(HttpServletResponse response) throws IOException;

    /**
     * 导入北向推送设备关联表
     *
     * @author jetox
     * @date  2026/01/08 10:25
     **/
    JSONObject importData(MultipartFile file);

    /**
     * 导出北向推送设备关联表
     *
     * @author jetox
     * @date  2026/01/08 10:25
     */
    void exportData(List<IotNorthboundDeviceRelIdParam> iotNorthboundDeviceRelIdParamList, HttpServletResponse response) throws IOException;

    /**
     * 设备绑定推送配置
     *
     * @author jetox
     * @date  2026/01/08
     */
    void bind(JSONObject params);
}
