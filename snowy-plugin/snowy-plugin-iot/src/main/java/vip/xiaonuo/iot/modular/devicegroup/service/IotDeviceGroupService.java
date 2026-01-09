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
package vip.xiaonuo.iot.modular.devicegroup.service;

import cn.hutool.core.lang.tree.Tree;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import vip.xiaonuo.iot.modular.device.entity.IotDevice;
import vip.xiaonuo.iot.modular.devicegroup.entity.IotDeviceGroup;
import vip.xiaonuo.iot.modular.devicegroup.param.IotDeviceGroupAddParam;
import vip.xiaonuo.iot.modular.devicegroup.param.IotDeviceGroupEditParam;
import vip.xiaonuo.iot.modular.devicegroup.param.IotDeviceGroupIdParam;
import vip.xiaonuo.iot.modular.devicegroup.param.IotDeviceGroupPageParam;
import java.io.IOException;
import java.util.List;

/**
 * 设备分组表Service接口
 *
 * @author jetox
 * @date  2025/12/13 18:30
 **/
public interface IotDeviceGroupService extends IService<IotDeviceGroup> {

    /**
     * 获取设备分组表分页
     *
     * @author jetox
     * @date  2025/12/13 18:30
     */
    Page<IotDeviceGroup> page(IotDeviceGroupPageParam iotDeviceGroupPageParam);

    /**
     * 获取设备分组树
     *
     * @author jetox
     * @date  2025/12/13 18:30
     */
    List<Tree<String>> tree();

    /**
     * 添加设备分组表
     *
     * @author jetox
     * @date  2025/12/13 18:30
     */
    void add(IotDeviceGroupAddParam iotDeviceGroupAddParam);

    /**
     * 编辑设备分组表
     *
     * @author jetox
     * @date  2025/12/13 18:30
     */
    void edit(IotDeviceGroupEditParam iotDeviceGroupEditParam);

    /**
     * 删除设备分组表
     *
     * @author jetox
     * @date  2025/12/13 18:30
     */
    void delete(List<IotDeviceGroupIdParam> iotDeviceGroupIdParamList);

    /**
     * 获取设备分组表详情
     *
     * @author jetox
     * @date  2025/12/13 18:30
     */
    IotDeviceGroup detail(IotDeviceGroupIdParam iotDeviceGroupIdParam);

    /**
     * 获取设备分组表详情
     *
     * @author jetox
     * @date  2025/12/13 18:30
     **/
    IotDeviceGroup queryEntity(String id);

    /**
     * 下载设备分组表导入模板
     *
     * @author jetox
     * @date  2025/12/13 18:30
     */
    void downloadImportTemplate(HttpServletResponse response) throws IOException;

    /**
     * 导入设备分组表
     *
     * @author jetox
     * @date  2025/12/13 18:30
     **/
    JSONObject importData(MultipartFile file);

    /**
     * 导出设备分组表
     *
     * @author jetox
     * @date  2025/12/13 18:30
     */
    void exportData(List<IotDeviceGroupIdParam> iotDeviceGroupIdParamList, HttpServletResponse response) throws IOException;

    /**
     * 批量关联设备到分组
     *
     * @author jetox
     * @date 2025/12/13
     */
    void batchRelateDevices(String groupId, List<String> deviceIds);

    /**
     * 批量移除分组下的设备
     *
     * @author jetox
     * @date 2025/12/13
     */
    void batchRemoveDevices(String groupId, List<String> deviceIds);

    /**
     * 获取分组下的设备ID列表
     *
     * @author jetox
     * @date 2025/12/13
     */
    List<String> getDeviceIdsByGroupId(String groupId);

    /**
     * 获取设备关联的所有分组ID
     *
     * @author jetox
     * @date 2025/12/13
     */
    List<String> getGroupIdsByDeviceId(String deviceId);

    /**
     * 同步设备分组（全量替换）
     *
     * @author jetox
     * @date 2025/12/13
     */
    void syncDeviceGroups(String deviceId, List<String> groupIds);

    /**
     * 获取分组及其所有子分组的ID列表（包含自身）
     *
     * @param groupId 分组ID
     * @return 分组ID列表（包含自身及所有子孙分组）
     * @author jetox
     * @date 2025/12/13
     */
    List<String> getGroupAndChildIds(String groupId);

    /**
     * 获取指定分组的已关联设备列表（分页）
     *
     * @param groupId 分组ID
     * @param searchKey 搜索关键字（设备名称或设备Key）
     * @param pageSize 每页数量
     * @return 设备列表
     * @author jetox
     * @date 2025/12/13
     */
    Page<IotDevice> getRelatedDevices(String groupId, String searchKey, Integer pageSize);

    /**
     * 获取未被任何分组关联的设备列表（分页）
     *
     * @param searchKey 搜索关键字（设备名称或设备Key）
     * @param pageSize 每页数量
     * @return 设备列表
     * @author jetox
     * @date 2025/12/13
     */
    Page<IotDevice> getUnrelatedDevices(String searchKey, Integer pageSize);
}