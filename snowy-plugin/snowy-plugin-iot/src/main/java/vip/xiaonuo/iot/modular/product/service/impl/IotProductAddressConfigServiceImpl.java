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
import vip.xiaonuo.iot.modular.device.entity.IotDeviceAddressConfig;
import vip.xiaonuo.iot.modular.device.mapper.IotDeviceAddressConfigMapper;
import vip.xiaonuo.iot.modular.product.entity.IotProductAddressConfig;
import vip.xiaonuo.iot.modular.product.mapper.IotProductAddressConfigMapper;
import vip.xiaonuo.iot.modular.product.service.IotProductAddressConfigService;

import java.util.Date;
import java.util.List;

/**
 * 产品级地址配置Service实现类
 *
 * @author gtc
 * @date  2026/01/11
 **/
@Slf4j
@Service
public class IotProductAddressConfigServiceImpl extends ServiceImpl<IotProductAddressConfigMapper, IotProductAddressConfig> 
        implements IotProductAddressConfigService {

    @Resource
    private IotDeviceAddressConfigMapper deviceAddressConfigMapper;

    @Override
    public List<IotProductAddressConfig> listByMappingId(String mappingId) {
        return this.list(new LambdaQueryWrapper<IotProductAddressConfig>()
                .eq(IotProductAddressConfig::getMappingId, mappingId));
    }

    @Override
    public IotProductAddressConfig getByMappingIdAndProtocol(String mappingId, String protocolType) {
        return this.getOne(new LambdaQueryWrapper<IotProductAddressConfig>()
                .eq(IotProductAddressConfig::getMappingId, mappingId)
                .eq(IotProductAddressConfig::getProtocolType, protocolType));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBatch(String mappingId, List<IotProductAddressConfig> configs) {
        // 删除旧的配置
        this.remove(new LambdaQueryWrapper<IotProductAddressConfig>()
                .eq(IotProductAddressConfig::getMappingId, mappingId));
        
        // 保存新的配置
        if (configs != null && !configs.isEmpty()) {
            Date now = new Date();
            configs.forEach(config -> {
                config.setId(IdUtil.getSnowflakeNextIdStr());
                config.setMappingId(mappingId);
                config.setCreateTime(now);
                config.setUpdateTime(now);
                if (config.getEnabled() == null) {
                    config.setEnabled(true);
                }
                if (config.getPollingInterval() == null) {
                    config.setPollingInterval(0);
                }
                if (config.getTimeout() == null) {
                    config.setTimeout(3000);
                }
                if (config.getRetryCount() == null) {
                    config.setRetryCount(3);
                }
            });
            this.saveBatch(configs);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int copyToDevice(String productMappingId, String deviceMappingId) {
        // 查询产品的所有地址配置
        List<IotProductAddressConfig> productConfigs = listByMappingId(productMappingId);
        
        if (productConfigs.isEmpty()) {
            log.debug("产品属性映射{}没有地址配置，无需复制", productMappingId);
            return 0;
        }
        
        // 删除设备该映射下的原有配置
        deviceAddressConfigMapper.delete(new LambdaQueryWrapper<IotDeviceAddressConfig>()
                .eq(IotDeviceAddressConfig::getMappingId, deviceMappingId));
        
        // 复制产品配置到设备
        Date now = new Date();
        for (IotProductAddressConfig productConfig : productConfigs) {
            IotDeviceAddressConfig deviceConfig = new IotDeviceAddressConfig();
            deviceConfig.setId(IdUtil.getSnowflakeNextIdStr());
            deviceConfig.setMappingId(deviceMappingId);
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
        }
        
        log.info("成功将产品属性映射{}的{}个地址配置复制到设备属性映射{}", 
                productMappingId, productConfigs.size(), deviceMappingId);
        return productConfigs.size();
    }
}
