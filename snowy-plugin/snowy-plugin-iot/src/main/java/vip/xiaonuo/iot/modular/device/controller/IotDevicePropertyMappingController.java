/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 */
package vip.xiaonuo.iot.modular.device.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import vip.xiaonuo.common.pojo.CommonResult;
import vip.xiaonuo.iot.modular.device.entity.IotDevice;
import vip.xiaonuo.iot.modular.device.entity.IotDeviceAddressConfig;
import vip.xiaonuo.iot.modular.device.entity.IotDevicePropertyMapping;
import vip.xiaonuo.iot.modular.device.mapper.IotDeviceAddressConfigMapper;
import vip.xiaonuo.iot.modular.device.mapper.IotDeviceMapper;
import vip.xiaonuo.iot.modular.device.mapper.IotDevicePropertyMappingMapper;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 设备属性映射控制器
 *
 * @author gtc
 * @date  2026/01/11
 **/
@Slf4j
@Tag(name = "设备属性映射")
@RestController
@RequestMapping("/iot/devicePropertyMapping")
public class IotDevicePropertyMappingController {

    @Resource
    private IotDevicePropertyMappingMapper devicePropertyMappingMapper;

    @Resource
    private IotDeviceAddressConfigMapper deviceAddressConfigMapper;
    
    @Resource
    private IotDeviceMapper deviceMapper;

    @Operation(summary = "获取设备属性映射列表")
    @SaCheckPermission("/iot/device/detail")
    @GetMapping("/list/{deviceId}")
    public CommonResult<List<Map<String, Object>>> list(@PathVariable String deviceId) {
        // 查询属性映射
        List<IotDevicePropertyMapping> mappings = devicePropertyMappingMapper.selectList(
            new LambdaQueryWrapper<IotDevicePropertyMapping>()
                .eq(IotDevicePropertyMapping::getDeviceId, deviceId)
                .orderByAsc(IotDevicePropertyMapping::getSortCode)
        );
        
        // 转换为扁平化数据结构（将双表数据合并为单层Map，便于前端展示）
        List<Map<String, Object>> result = mappings.stream().map(mapping -> {
            Map<String, Object> item = new HashMap<>();
            
            // 属性映射字段
            item.put("id", mapping.getId());
            item.put("thingModelId", mapping.getThingModelId());
            item.put("identifier", mapping.getIdentifier());
            item.put("enabled", mapping.getEnabled());
            item.put("sortCode", mapping.getSortCode());
            
            // 查询地址配置
            List<IotDeviceAddressConfig> addressConfigs = deviceAddressConfigMapper.selectList(
                new LambdaQueryWrapper<IotDeviceAddressConfig>()
                    .eq(IotDeviceAddressConfig::getMappingId, mapping.getId())
            );
            if (!addressConfigs.isEmpty()) {
                IotDeviceAddressConfig config = addressConfigs.get(0);
                
                // 地址配置字段（扁平化到顶层）
                item.put("registerAddress", config.getDeviceAddress());
                item.put("dataType", config.getDataType());
                item.put("scaleFactor", config.getValueMultiplier());
                item.put("offset", config.getValueOffset());
                item.put("byteOrder", config.getByteOrder());
                
                // 通用化处理：自动提取 extConfig 中的所有字段到顶层
                // 这样新增协议时，前端不需要修改代码
                if (config.getExtConfig() != null && !config.getExtConfig().isEmpty()) {
                    try {
                        cn.hutool.json.JSONObject extConfig = cn.hutool.json.JSONUtil.parseObj(config.getExtConfig());
                        // 将 extConfig 中的所有字段提取到顶层
                        extConfig.forEach((key, value) -> {
                            // 避免覆盖已有的顶层字段
                            if (!item.containsKey(key)) {
                                item.put(key, value);
                            }
                        });
                    } catch (Exception e) {
                        log.warn("解析 extConfig 失败: {}", config.getExtConfig());
                    }
                }
            }
            
            return item;
        }).collect(Collectors.toList());
        
        return CommonResult.data(result);
    }

    @Operation(summary = "批量保存设备属性映射")
    @SaCheckPermission("/iot/device/edit")
    @PostMapping("/batchSave/{deviceId}")
    @Transactional(rollbackFor = Exception.class)
    public CommonResult<String> batchSave(@PathVariable String deviceId, @RequestBody List<Map<String, Object>> mappings) {
        log.info("批量保存设备{}的属性映射，数量：{}", deviceId, mappings.size());
        
        // 1. 查询设备信息获取协议类型
        IotDevice device = deviceMapper.selectById(deviceId);
        String protocolType = device != null && device.getProtocolType() != null 
            ? device.getProtocolType() 
            : "MODBUS_TCP";
        
        // 2. 删除设备原有的所有属性映射和地址配置
        List<IotDevicePropertyMapping> existMappings = devicePropertyMappingMapper.selectList(
            new LambdaQueryWrapper<IotDevicePropertyMapping>()
                .eq(IotDevicePropertyMapping::getDeviceId, deviceId)
        );
        
        if (!existMappings.isEmpty()) {
            List<String> mappingIds = existMappings.stream().map(IotDevicePropertyMapping::getId).collect(Collectors.toList());
            
            // 批量删除地址配置
            deviceAddressConfigMapper.delete(
                new LambdaQueryWrapper<IotDeviceAddressConfig>()
                    .in(IotDeviceAddressConfig::getMappingId, mappingIds)
            );
            
            // 删除属性映射
            devicePropertyMappingMapper.deleteBatchIds(mappingIds);
        }
        
        // 3. 保存新的映射配置
        Date now = new Date();
        for (Map<String, Object> item : mappings) {
            try {
                // 创建属性映射
                IotDevicePropertyMapping mapping = new IotDevicePropertyMapping();
                String mappingId = cn.hutool.core.util.IdUtil.getSnowflakeNextIdStr();
                mapping.setId(mappingId);
                mapping.setDeviceId(deviceId);
                mapping.setThingModelId((String) item.get("thingModelId"));
                mapping.setIdentifier((String) item.get("identifier"));
                mapping.setEnabled(item.get("enabled") != null ? (Boolean) item.get("enabled") : true);
                mapping.setSortCode(item.get("sortCode") != null ? (Integer) item.get("sortCode") : 0);
                mapping.setCreateTime(now);
                mapping.setUpdateTime(now);
                devicePropertyMappingMapper.insert(mapping);
                
                // 创建地址配置
                IotDeviceAddressConfig addressConfig = new IotDeviceAddressConfig();
                addressConfig.setId(cn.hutool.core.util.IdUtil.getSnowflakeNextIdStr());
                addressConfig.setMappingId(mappingId);
                addressConfig.setProtocolType(protocolType);
                
                // 设置deviceAddress（必填字段）
                String registerAddress = (String) item.get("registerAddress");
                addressConfig.setDeviceAddress(registerAddress != null && !registerAddress.isEmpty() ? registerAddress : "0");
                
                addressConfig.setDataType((String) item.get("dataType"));
                
                // 处理数值类型转换
                Object scaleFactor = item.get("scaleFactor");
                addressConfig.setValueMultiplier(scaleFactor != null ? new java.math.BigDecimal(scaleFactor.toString()) : java.math.BigDecimal.ONE);
                
                // valueOffset 默认为 0
                Object valueOffset = item.get("valueOffset");
                addressConfig.setValueOffset(valueOffset != null ? new java.math.BigDecimal(valueOffset.toString()) : java.math.BigDecimal.ZERO);
                
                addressConfig.setByteOrder((String) item.get("byteOrder"));
                
                // 构建 extConfig JSON（存储所有协议特定字段）
                cn.hutool.json.JSONObject extConfig = cn.hutool.json.JSONUtil.createObj();
                
                // ⚠️ 重要：registerAddress 必须存入 extConfig，设备控制时需要从这里读取
                if (registerAddress != null && !registerAddress.isEmpty()) {
                    try {
                        // 尝试转换为整数（Modbus寄存器地址）
                        extConfig.set("registerAddress", Integer.parseInt(registerAddress));
                    } catch (NumberFormatException e) {
                        // 如果不是纯数字，存储为字符串（其他协议可能需要）
                        extConfig.set("registerAddress", registerAddress);
                    }
                }
                
                // 通用化：自动将所有非标准字段存入 extConfig
                // 标准字段列表（已经单独处理的字段）
                Set<String> standardFields = new HashSet<>(Arrays.asList(
                    "thingModelId", "identifier", "registerAddress", "dataType", 
                    "scaleFactor", "valueOffset", "byteOrder", "enabled", 
                    "sortCode", "name", "description", "displayAddress",
                    "id", "deviceId", "createTime", "updateTime"
                ));
                
                // 自动存储所有非标准字段到 extConfig
                item.forEach((key, value) -> {
                    if (!standardFields.contains(key) && value != null) {
                        // registerAddress 已经单独处理过了，不要重复添加
                        if (!key.equals("registerAddress")) {
                            extConfig.set(key, value);
                        }
                    }
                });
                
                addressConfig.setExtConfig(extConfig.toString());
                addressConfig.setPollingInterval(0);
                addressConfig.setTimeout(3000);
                addressConfig.setRetryCount(3);
                addressConfig.setEnabled(item.get("enabled") != null ? (Boolean) item.get("enabled") : true);
                addressConfig.setCreateTime(now);
                addressConfig.setUpdateTime(now);
                
                deviceAddressConfigMapper.insert(addressConfig);
                
            } catch (Exception e) {
                log.error("保存设备映射配置失败: {}", item.get("identifier"), e);
                throw new RuntimeException("保存设备映射配置失败: " + e.getMessage());
            }
        }
        
        log.info("成功保存{}条设备属性映射", mappings.size());
        return CommonResult.ok("保存成功");
    }

    @Operation(summary = "删除设备属性映射")
    @SaCheckPermission("/iot/device/edit")
    @DeleteMapping("/delete/{deviceId}")
    @Transactional(rollbackFor = Exception.class)
    public CommonResult<String> delete(@PathVariable String deviceId, @RequestBody List<String> ids) {
        log.info("删除设备{}的属性映射，ID列表：{}", deviceId, ids);
        
        // 1. 批量删除关联的地址配置（级联删除）
        deviceAddressConfigMapper.delete(
            new LambdaQueryWrapper<IotDeviceAddressConfig>()
                .in(IotDeviceAddressConfig::getMappingId, ids)
        );
        
        // 2. 删除属性映射
        devicePropertyMappingMapper.deleteBatchIds(ids);
        
        log.info("成功删除{}条设备属性映射", ids.size());
        return CommonResult.ok("删除成功");
    }

    @Operation(summary = "清除设备级映射")
    @SaCheckPermission("/iot/device/edit")
    @DeleteMapping("/clear/{deviceId}")
    @Transactional(rollbackFor = Exception.class)
    public CommonResult<String> clear(@PathVariable String deviceId) {
        log.info("清除设备{}的所有属性映射", deviceId);
        
        // 1. 查询所有设备级映射
        List<IotDevicePropertyMapping> mappings = devicePropertyMappingMapper.selectList(
            new LambdaQueryWrapper<IotDevicePropertyMapping>()
                .eq(IotDevicePropertyMapping::getDeviceId, deviceId)
        );
        
        // 2. 批量删除关联的地址配置
        if (!mappings.isEmpty()) {
            List<String> mappingIds = mappings.stream()
                .map(IotDevicePropertyMapping::getId)
                .collect(Collectors.toList());
            deviceAddressConfigMapper.delete(
                new LambdaQueryWrapper<IotDeviceAddressConfig>()
                    .in(IotDeviceAddressConfig::getMappingId, mappingIds)
            );
        }
        
        // 3. 删除设备的所有属性映射
        devicePropertyMappingMapper.delete(
            new LambdaQueryWrapper<IotDevicePropertyMapping>()
                .eq(IotDevicePropertyMapping::getDeviceId, deviceId)
        );
        
        log.info("成功清除设备{}的{}条属性映射", deviceId, mappings.size());
        return CommonResult.ok("清除成功");
    }
}
