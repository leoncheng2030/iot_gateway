/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 */
package vip.xiaonuo.iot.modular.device.service;

import com.baomidou.mybatisplus.extension.service.IService;
import vip.xiaonuo.iot.modular.device.entity.IotDeviceAddressConfig;
import vip.xiaonuo.iot.modular.device.entity.IotDevicePropertyMapping;

import java.util.List;
import java.util.Map;

/**
 * 设备属性映射Service接口（新架构）
 *
 * @author gtc
 * @date  2026/01/11
 **/
public interface IotDevicePropertyMappingService extends IService<IotDevicePropertyMapping> {

    /**
     * 获取设备的属性映射列表（带地址配置）
     * 优先返回设备级配置，如果设备级不存在则返回产品级（物模型）配置
     *
     * @param deviceId 设备ID
     * @return 属性映射列表（Map包含propertyMapping和addressConfig）
     */
    List<Map<String, Object>> getDevicePropertyMappingsWithAddress(String deviceId);

    /**
     * 获取设备的属性映射Map（按标识符索引，包含地址配置）
     * 优先返回设备级配置，如果设备级不存在则返回产品级（物模型）配置
     *
     * @param deviceId 设备ID
     * @return Map<标识符, 完整映射信息>
     */
    Map<String, Map<String, Object>> getDevicePropertyMappingMap(String deviceId);

    /**
     * 根据功能码获取设备的地址配置映射
     *
     * @param deviceId 设备ID
     * @param functionCode 功能码
     * @return Map<寄存器地址(Integer), 地址配置>
     */
    Map<Integer, IotDeviceAddressConfig> getAddressConfigByFunctionCode(String deviceId, String functionCode);

    /**
     * 按产品ID查询产品级属性映射
     *
     * @param productId 产品ID
     * @return 属性映射列表
     */
    List<IotDevicePropertyMapping> listByProductId(String productId);

    /**
     * 按设备ID查询设备级属性映射
     *
     * @param deviceId 设备ID
     * @return 属性映射列表
     */
    List<IotDevicePropertyMapping> listByDeviceId(String deviceId);
}
