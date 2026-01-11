/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 */
package vip.xiaonuo.iot.modular.product.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.xiaonuo.iot.modular.device.entity.IotDevicePropertyMapping;
import vip.xiaonuo.iot.modular.device.entity.IotDeviceAddressConfig;
import vip.xiaonuo.iot.modular.device.mapper.IotDevicePropertyMappingMapper;
import vip.xiaonuo.iot.modular.device.mapper.IotDeviceAddressConfigMapper;
import vip.xiaonuo.iot.modular.product.entity.IotProductPropertyMapping;
import vip.xiaonuo.iot.modular.product.entity.IotProductAddressConfig;
import vip.xiaonuo.iot.modular.product.mapper.IotProductPropertyMappingMapper;
import vip.xiaonuo.iot.modular.product.mapper.IotProductAddressConfigMapper;
import vip.xiaonuo.iot.modular.product.service.IotProductPropertyMappingService;

import java.util.Date;
import java.util.List;

/**
 * 产品属性映射Service实现类
 *
 * @author gtc
 * @date  2026/01/11
 **/
@Slf4j
@Service
public class IotProductPropertyMappingServiceImpl extends ServiceImpl<IotProductPropertyMappingMapper, IotProductPropertyMapping> 
        implements IotProductPropertyMappingService {

    @Resource
    private IotDevicePropertyMappingMapper devicePropertyMappingMapper;
    
    @Resource
    private IotDeviceAddressConfigMapper deviceAddressConfigMapper;
    
    @Resource
    private IotProductAddressConfigMapper productAddressConfigMapper;

    @Override
    public List<IotProductPropertyMapping> listByProductId(String productId) {
        return this.list(new LambdaQueryWrapper<IotProductPropertyMapping>()
                .eq(IotProductPropertyMapping::getProductId, productId)
                .orderByAsc(IotProductPropertyMapping::getSortCode));
    }

    @Override
    public IotProductPropertyMapping getByProductIdAndIdentifier(String productId, String identifier) {
        return this.getOne(new LambdaQueryWrapper<IotProductPropertyMapping>()
                .eq(IotProductPropertyMapping::getProductId, productId)
                .eq(IotProductPropertyMapping::getIdentifier, identifier));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBatch(String productId, List<IotProductPropertyMapping> mappings) {
        // 删除旧的映射
        this.remove(new LambdaQueryWrapper<IotProductPropertyMapping>()
                .eq(IotProductPropertyMapping::getProductId, productId));
        
        // 保存新的映射
        if (mappings != null && !mappings.isEmpty()) {
            Date now = new Date();
            mappings.forEach(mapping -> {
                mapping.setId(IdUtil.getSnowflakeNextIdStr());
                mapping.setProductId(productId);
                mapping.setCreateTime(now);
                mapping.setUpdateTime(now);
                if (mapping.getEnabled() == null) {
                    mapping.setEnabled(true);
                }
                if (mapping.getSortCode() == null) {
                    mapping.setSortCode(100);
                }
            });
            this.saveBatch(mappings);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int copyToDevice(String productId, String deviceId) {
        // 查询产品的所有属性映射
        List<IotProductPropertyMapping> productMappings = listByProductId(productId);
        
        if (productMappings.isEmpty()) {
            log.info("产品{}没有属性映射，无需复制", productId);
            return 0;
        }
        
        // 删除设备原有的映射和地址配置
        devicePropertyMappingMapper.delete(new LambdaQueryWrapper<IotDevicePropertyMapping>()
                .eq(IotDevicePropertyMapping::getDeviceId, deviceId));
        
        // 复制产品映射到设备，并同时复制地址配置
        Date now = new Date();
        int totalAddressConfigs = 0;
        
        for (IotProductPropertyMapping productMapping : productMappings) {
            // 1. 创建设备属性映射
            IotDevicePropertyMapping deviceMapping = new IotDevicePropertyMapping();
            String deviceMappingId = IdUtil.getSnowflakeNextIdStr();
            deviceMapping.setId(deviceMappingId);
            deviceMapping.setDeviceId(deviceId);
            deviceMapping.setThingModelId(productMapping.getThingModelId());
            deviceMapping.setIdentifier(productMapping.getIdentifier());
            deviceMapping.setEnabled(productMapping.getEnabled());
            deviceMapping.setSortCode(productMapping.getSortCode());
            deviceMapping.setCreateTime(now);
            deviceMapping.setUpdateTime(now);
            devicePropertyMappingMapper.insert(deviceMapping);
            
            // 2. 复制该映射下的所有地址配置
            List<IotProductAddressConfig> productAddressConfigs = productAddressConfigMapper.selectList(
                new LambdaQueryWrapper<IotProductAddressConfig>()
                    .eq(IotProductAddressConfig::getMappingId, productMapping.getId())
            );
            
            for (IotProductAddressConfig productConfig : productAddressConfigs) {
                IotDeviceAddressConfig deviceConfig = new IotDeviceAddressConfig();
                deviceConfig.setId(IdUtil.getSnowflakeNextIdStr());
                deviceConfig.setMappingId(deviceMappingId);  // 关联到设备映射ID
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
                deviceConfig.setRemark(productConfig.getRemark());
                deviceConfig.setCreateTime(now);
                deviceConfig.setUpdateTime(now);
                
                deviceAddressConfigMapper.insert(deviceConfig);
                totalAddressConfigs++;
            }
        }
        
        log.info("成功将产品{}的{}个属性映射和{}个地址配置复制到设备{}", 
                productId, productMappings.size(), totalAddressConfigs, deviceId);
        return productMappings.size();
    }
}
