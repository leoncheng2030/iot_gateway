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
package vip.xiaonuo.iot.modular.driverlog.service;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import vip.xiaonuo.iot.modular.driverlog.entity.IotDriverLog;
import vip.xiaonuo.iot.modular.driverlog.param.IotDriverLogAddParam;
import vip.xiaonuo.iot.modular.driverlog.param.IotDriverLogEditParam;
import vip.xiaonuo.iot.modular.driverlog.param.IotDriverLogIdParam;
import vip.xiaonuo.iot.modular.driverlog.param.IotDriverLogPageParam;
import java.io.IOException;
import java.util.List;

/**
 * 运行日志Service接口
 *
 * @author jetox
 * @date  2025/12/13 09:46
 **/
public interface IotDriverLogService extends IService<IotDriverLog> {

    /**
     * 获取运行日志分页
     *
     * @author jetox
     * @date  2025/12/13 09:46
     */
    Page<IotDriverLog> page(IotDriverLogPageParam iotDriverLogPageParam);

    /**
     * 添加运行日志
     *
     * @author jetox
     * @date  2025/12/13 09:46
     */
    void add(IotDriverLogAddParam iotDriverLogAddParam);

    /**
     * 编辑运行日志
     *
     * @author jetox
     * @date  2025/12/13 09:46
     */
    void edit(IotDriverLogEditParam iotDriverLogEditParam);

    /**
     * 删除运行日志
     *
     * @author jetox
     * @date  2025/12/13 09:46
     */
    void delete(List<IotDriverLogIdParam> iotDriverLogIdParamList);

    /**
     * 获取运行日志详情
     *
     * @author jetox
     * @date  2025/12/13 09:46
     */
    IotDriverLog detail(IotDriverLogIdParam iotDriverLogIdParam);

    /**
     * 获取运行日志详情
     *
     * @author jetox
     * @date  2025/12/13 09:46
     **/
    IotDriverLog queryEntity(String id);

    /**
     * 下载运行日志导入模板
     *
     * @author jetox
     * @date  2025/12/13 09:46
     */
    void downloadImportTemplate(HttpServletResponse response) throws IOException;

    /**
     * 导入运行日志
     *
     * @author jetox
     * @date  2025/12/13 09:46
     **/
    JSONObject importData(MultipartFile file);

    /**
     * 导出运行日志
     *
     * @author jetox
     * @date  2025/12/13 09:46
     */
    void exportData(List<IotDriverLogIdParam> iotDriverLogIdParamList, HttpServletResponse response) throws IOException;
}
