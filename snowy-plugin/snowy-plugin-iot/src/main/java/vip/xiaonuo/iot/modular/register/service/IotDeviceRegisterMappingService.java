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
package vip.xiaonuo.iot.modular.register.service;

import com.baomidou.mybatisplus.extension.service.IService;
import vip.xiaonuo.iot.modular.register.entity.IotDeviceRegisterMapping;
import java.util.List;
import java.util.Map;

/**
 * 设备寄存器映射Service接口
 *
 * @author jetox
 * @date  2025/12/12 12:33
 **/
public interface IotDeviceRegisterMappingService extends IService<IotDeviceRegisterMapping> {

    /**
     * 获取设备的寄存器映射列表
     * 优先返回设备级配置，如果设备级不存在则返回产品级（物模型）配置
     *
     * @param deviceId 设备ID
     * @return 寄存器映射列表
     */
    List<IotDeviceRegisterMapping> getDeviceRegisterMappings(String deviceId);

    /**
     * 获取设备的寄存器映射Map（按标识符索引）
     * 优先返回设备级配置，如果设备级不存在则返回产品级（物模型）配置
     *
     * @param deviceId 设备ID
     * @return Map<标识符, 寄存器映射>
     */
    Map<String, IotDeviceRegisterMapping> getDeviceRegisterMappingMap(String deviceId);

    /**
     * 根据功能码获取设备的寄存器映射
     *
     * @param deviceId 设备ID
     * @param functionCode 功能码
     * @return Map<寄存器地址, 寄存器映射>
     */
    Map<Integer, IotDeviceRegisterMapping> getRegisterMappingByFunctionCode(String deviceId, String functionCode);

    /**
     * 批量保存设备的寄存器映射
     *
     * @param deviceId 设备ID
     * @param mappings 寄存器映射列表
     */
    void batchSaveDeviceRegisterMappings(String deviceId, List<IotDeviceRegisterMapping> mappings);

    /**
     * 删除设备的所有寄存器映射
     *
     * @param deviceId 设备ID
     */
    void deleteByDeviceId(String deviceId);
}
