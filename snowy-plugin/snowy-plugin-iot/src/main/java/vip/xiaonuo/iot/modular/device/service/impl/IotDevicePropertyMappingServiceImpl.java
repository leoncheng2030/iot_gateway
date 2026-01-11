/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 */
package vip.xiaonuo.iot.modular.device.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vip.xiaonuo.iot.modular.device.entity.IotDevice;
import vip.xiaonuo.iot.modular.device.entity.IotDeviceAddressConfig;
import vip.xiaonuo.iot.modular.device.entity.IotDevicePropertyMapping;
import vip.xiaonuo.iot.modular.device.mapper.IotDeviceAddressConfigMapper;
import vip.xiaonuo.iot.modular.device.mapper.IotDeviceMapper;
import vip.xiaonuo.iot.modular.device.mapper.IotDevicePropertyMappingMapper;
import vip.xiaonuo.iot.modular.device.service.IotDevicePropertyMappingService;
import vip.xiaonuo.iot.modular.product.entity.IotProductAddressConfig;
import vip.xiaonuo.iot.modular.product.entity.IotProductPropertyMapping;
import vip.xiaonuo.iot.modular.product.mapper.IotProductAddressConfigMapper;
import vip.xiaonuo.iot.modular.product.mapper.IotProductPropertyMappingMapper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 设备属性映射Service实现（新架构）
 *
 * @author gtc
 * @date  2026/01/11
 **/
@Slf4j
@Service
public class IotDevicePropertyMappingServiceImpl extends ServiceImpl<IotDevicePropertyMappingMapper, IotDevicePropertyMapping> 
        implements IotDevicePropertyMappingService {

    @Resource
    private IotDevicePropertyMappingMapper devicePropertyMappingMapper;

    @Resource
    private IotDeviceAddressConfigMapper deviceAddressConfigMapper;

    @Resource
    private IotProductPropertyMappingMapper productPropertyMappingMapper;

    @Resource
    private IotProductAddressConfigMapper productAddressConfigMapper;

    @Resource
    private IotDeviceMapper deviceMapper;

    @Override
    public List<Map<String, Object>> getDevicePropertyMappingsWithAddress(String deviceId) {
        // 1. 先检查设备级配置
        List<IotDevicePropertyMapping> deviceMappings = devicePropertyMappingMapper.selectList(
            new LambdaQueryWrapper<IotDevicePropertyMapping>()
                .eq(IotDevicePropertyMapping::getDeviceId, deviceId)
                .eq(IotDevicePropertyMapping::getEnabled, true)
                .orderByAsc(IotDevicePropertyMapping::getSortCode)
        );

        if (!deviceMappings.isEmpty()) {
            log.debug("使用设备级配置，设备ID: {}", deviceId);
            return convertToMappingList(deviceMappings, deviceAddressConfigMapper);
        }

        // 2. 如果没有设备级配置，使用产品级配置
        IotDevice device = deviceMapper.selectById(deviceId);
        if (device == null || device.getProductId() == null) {
            log.warn("设备不存在或未关联产品，设备ID: {}", deviceId);
            return Collections.emptyList();
        }

        List<IotProductPropertyMapping> productMappings = productPropertyMappingMapper.selectList(
            new LambdaQueryWrapper<IotProductPropertyMapping>()
                .eq(IotProductPropertyMapping::getProductId, device.getProductId())
                .eq(IotProductPropertyMapping::getEnabled, true)
                .orderByAsc(IotProductPropertyMapping::getSortCode)
        );

        log.debug("使用产品级配置，产品ID: {}", device.getProductId());
        return convertProductToDeviceMappingList(productMappings, productAddressConfigMapper);
    }

    @Override
    public Map<String, Map<String, Object>> getDevicePropertyMappingMap(String deviceId) {
        List<Map<String, Object>> list = getDevicePropertyMappingsWithAddress(deviceId);
        return list.stream().collect(Collectors.toMap(
            item -> (String) item.get("identifier"),
            item -> item,
            (old, newVal) -> newVal
        ));
    }

    @Override
    public Map<Integer, IotDeviceAddressConfig> getAddressConfigByFunctionCode(String deviceId, String functionCode) {
        // 1. 先获取设备的所有映射（设备级或产品级）
        List<Map<String, Object>> mappings = getDevicePropertyMappingsWithAddress(deviceId);
        
        // 2. 过滤出指定功能码的地址配置
        Map<Integer, IotDeviceAddressConfig> result = new HashMap<>();
        for (Map<String, Object> item : mappings) {
            IotDeviceAddressConfig config = (IotDeviceAddressConfig) item.get("addressConfig");
            if (config != null && config.getExtConfig() != null) {
                try {
                    cn.hutool.json.JSONObject extConfig = JSONUtil.parseObj(config.getExtConfig());
                    String fc = extConfig.getStr("functionCode");
                    if (functionCode.equals(fc)) {
                        Integer registerAddress = extConfig.getInt("registerAddress");
                        if (registerAddress != null) {
                            // 将identifier存入extConfig，便于后续使用
                            String identifier = (String) item.get("identifier");
                            extConfig.set("identifier", identifier);
                            config.setExtConfig(extConfig.toString());
                            result.put(registerAddress, config);
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析extConfig失败: {}", config.getExtConfig());
                }
            }
        }
        
        return result;
    }

    @Override
    public List<IotDevicePropertyMapping> listByProductId(String productId) {
        return devicePropertyMappingMapper.selectList(
            new LambdaQueryWrapper<IotDevicePropertyMapping>()
                .eq(IotDevicePropertyMapping::getDeviceId, productId)
        );
    }

    @Override
    public List<IotDevicePropertyMapping> listByDeviceId(String deviceId) {
        return devicePropertyMappingMapper.selectList(
            new LambdaQueryWrapper<IotDevicePropertyMapping>()
                .eq(IotDevicePropertyMapping::getDeviceId, deviceId)
        );
    }

    /**
     * 转换设备级映射为Map列表
     */
    private List<Map<String, Object>> convertToMappingList(
            List<IotDevicePropertyMapping> mappings, 
            IotDeviceAddressConfigMapper addressConfigMapper) {
        
        return mappings.stream().map(mapping -> {
            Map<String, Object> item = new HashMap<>();
            item.put("propertyMapping", mapping);
            item.put("identifier", mapping.getIdentifier());
            
            // 查询地址配置
            List<IotDeviceAddressConfig> configs = addressConfigMapper.selectList(
                new LambdaQueryWrapper<IotDeviceAddressConfig>()
                    .eq(IotDeviceAddressConfig::getMappingId, mapping.getId())
            );
            item.put("addressConfig", configs.isEmpty() ? null : configs.get(0));
            
            return item;
        }).collect(Collectors.toList());
    }

    /**
     * 转换产品级映射为设备级Map列表（用于继承）
     */
    private List<Map<String, Object>> convertProductToDeviceMappingList(
            List<IotProductPropertyMapping> productMappings,
            IotProductAddressConfigMapper addressConfigMapper) {
        
        return productMappings.stream().map(mapping -> {
            Map<String, Object> item = new HashMap<>();
            
            // 将产品级映射转换为设备级结构
            IotDevicePropertyMapping deviceMapping = new IotDevicePropertyMapping();
            deviceMapping.setIdentifier(mapping.getIdentifier());
            deviceMapping.setThingModelId(mapping.getThingModelId());
            deviceMapping.setEnabled(mapping.getEnabled());
            deviceMapping.setSortCode(mapping.getSortCode());
            
            item.put("propertyMapping", deviceMapping);
            item.put("identifier", mapping.getIdentifier());
            
            // 查询产品级地址配置，转换为设备级
            List<IotProductAddressConfig> productConfigs = addressConfigMapper.selectList(
                new LambdaQueryWrapper<IotProductAddressConfig>()
                    .eq(IotProductAddressConfig::getMappingId, mapping.getId())
            );
            
            if (!productConfigs.isEmpty()) {
                IotProductAddressConfig productConfig = productConfigs.get(0);
                
                // 转换为设备级配置结构
                IotDeviceAddressConfig deviceConfig = new IotDeviceAddressConfig();
                deviceConfig.setProtocolType(productConfig.getProtocolType());
                deviceConfig.setDeviceAddress(productConfig.getDeviceAddress());
                deviceConfig.setDataType(productConfig.getDataType());
                deviceConfig.setValueMultiplier(productConfig.getValueMultiplier());
                deviceConfig.setValueOffset(productConfig.getValueOffset());
                deviceConfig.setByteOrder(productConfig.getByteOrder());
                deviceConfig.setExtConfig(productConfig.getExtConfig());
                deviceConfig.setPollingInterval(productConfig.getPollingInterval());
                deviceConfig.setTimeout(productConfig.getTimeout());
                deviceConfig.setRetryCount(productConfig.getRetryCount());
                deviceConfig.setEnabled(productConfig.getEnabled());
                
                item.put("addressConfig", deviceConfig);
            } else {
                item.put("addressConfig", null);
            }
            
            return item;
        }).collect(Collectors.toList());
    }
}
