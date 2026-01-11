/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 */
package vip.xiaonuo.iot.modular.product.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import vip.xiaonuo.common.pojo.CommonResult;
import vip.xiaonuo.iot.modular.product.entity.IotProductAddressConfig;
import vip.xiaonuo.iot.modular.product.entity.IotProductPropertyMapping;
import vip.xiaonuo.iot.modular.product.entity.IotProduct;
import vip.xiaonuo.iot.modular.product.service.IotProductAddressConfigService;
import vip.xiaonuo.iot.modular.product.service.IotProductPropertyMappingService;
import vip.xiaonuo.iot.modular.product.service.IotProductService;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 产品属性映射控制器
 *
 * @author gtc
 * @date  2026/01/11
 **/
@Slf4j
@Tag(name = "产品属性映射")
@RestController
@RequestMapping("/iot/productPropertyMapping")
public class IotProductPropertyMappingController {

    @Resource
    private IotProductPropertyMappingService productPropertyMappingService;

    @Resource
    private IotProductAddressConfigService productAddressConfigService;
    
    @Resource
    private IotProductService productService;

    @Operation(summary = "获取产品属性映射列表")
    @SaCheckPermission("/iot/product/detail")
    @GetMapping("/list/{productId}")
    public CommonResult<List<Map<String, Object>>> list(@PathVariable String productId) {
        // 查询属性映射
        List<IotProductPropertyMapping> mappings = productPropertyMappingService.listByProductId(productId);
        
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
            List<IotProductAddressConfig> addressConfigs = productAddressConfigService.listByMappingId(mapping.getId());
            if (!addressConfigs.isEmpty()) {
                IotProductAddressConfig config = addressConfigs.get(0);
                
                // 地址配置字段（扁平化到顶层）
                item.put("registerAddress", config.getDeviceAddress());
                item.put("dataType", config.getDataType());
                item.put("scaleFactor", config.getValueMultiplier());
                item.put("offset", config.getValueOffset());
                item.put("byteOrder", config.getByteOrder());
                
                // 从extConfig中提取协议特定字段
                if (config.getExtConfig() != null && !config.getExtConfig().isEmpty()) {
                    try {
                        cn.hutool.json.JSONObject extConfig = cn.hutool.json.JSONUtil.parseObj(config.getExtConfig());
                                        
                        // Modbus字段
                        item.put("functionCode", extConfig.getStr("functionCode"));
                        item.put("bitIndex", extConfig.getInt("bitIndex"));
                                        
                        // S7字段（同时返回area和storageArea，兼容前后端）
                        String storageArea = extConfig.getStr("storageArea");
                        item.put("storageArea", storageArea);  // 后端保存时使用
                        item.put("area", storageArea);         // 前端显示时使用
                        item.put("dbNumber", extConfig.getInt("dbNumber"));
                        item.put("offset", extConfig.getInt("offset"));
                        item.put("dataTypePrefix", extConfig.getStr("dataTypePrefix"));
                                        
                    } catch (Exception e) {
                        log.warn("解析extConfig失败: {}", config.getExtConfig());
                    }
                }
            }
            
            return item;
        }).collect(Collectors.toList());
        
        return CommonResult.data(result);
    }

    @Operation(summary = "批量保存产品属性映射")
    @SaCheckPermission("/iot/product/edit")
    @PostMapping("/batchSave/{productId}")
    @Transactional(rollbackFor = Exception.class)
    public CommonResult<String> batchSave(@PathVariable String productId, @RequestBody List<Map<String, Object>> mappings) {
        log.info("批量保存产品{}的属性映射，数量：{}", productId, mappings.size());
        
        // 1. 查询产品信息获取协议类型
        IotProduct product = productService.getById(productId);
        String protocolType = product != null ? product.getProtocolType() : "MODBUS_TCP";
        
        // 2. 删除产品原有的所有属性映射和地址配置
        List<IotProductPropertyMapping> existMappings = productPropertyMappingService.listByProductId(productId);
        if (!existMappings.isEmpty()) {
            List<String> mappingIds = existMappings.stream().map(IotProductPropertyMapping::getId).collect(Collectors.toList());
            
            // 删除地址配置
            for (String mappingId : mappingIds) {
                productAddressConfigService.remove(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<IotProductAddressConfig>()
                        .eq(IotProductAddressConfig::getMappingId, mappingId)
                );
            }
            
            // 删除属性映射
            productPropertyMappingService.removeByIds(mappingIds);
        }
        
        // 3. 保存新的映射配置
        Date now = new Date();
        for (Map<String, Object> item : mappings) {
            try {
                // 创建属性映射
                IotProductPropertyMapping mapping = new IotProductPropertyMapping();
                String mappingId = cn.hutool.core.util.IdUtil.getSnowflakeNextIdStr();
                mapping.setId(mappingId);
                mapping.setProductId(productId);
                mapping.setThingModelId((String) item.get("thingModelId"));
                mapping.setIdentifier((String) item.get("identifier"));
                mapping.setEnabled(item.get("enabled") != null ? (Boolean) item.get("enabled") : true);
                mapping.setSortCode(item.get("sortCode") != null ? (Integer) item.get("sortCode") : 0);
                mapping.setCreateTime(now);
                mapping.setUpdateTime(now);
                productPropertyMappingService.save(mapping);
                
                // 创建地址配置
                IotProductAddressConfig addressConfig = new IotProductAddressConfig();
                addressConfig.setId(cn.hutool.core.util.IdUtil.getSnowflakeNextIdStr());
                addressConfig.setMappingId(mappingId);
                addressConfig.setProtocolType(protocolType); // 从产品获取协议类型
                
                // 设置deviceAddress（必填字段，如果null则使用默认值"0"）
                String registerAddress = (String) item.get("registerAddress");
                addressConfig.setDeviceAddress(registerAddress != null && !registerAddress.isEmpty() ? registerAddress : "0");
                
                addressConfig.setDataType((String) item.get("dataType"));
                
                // 处理数值类型转换
                Object scaleFactor = item.get("scaleFactor");
                addressConfig.setValueMultiplier(scaleFactor != null ? new java.math.BigDecimal(scaleFactor.toString()) : java.math.BigDecimal.ONE);
                
                Object offset = item.get("offset");
                addressConfig.setValueOffset(offset != null ? new java.math.BigDecimal(offset.toString()) : java.math.BigDecimal.ZERO);
                
                addressConfig.setByteOrder((String) item.get("byteOrder"));
                
                // 构建extConfig JSON（存储所有协议特定字段）
                cn.hutool.json.JSONObject extConfig = cn.hutool.json.JSONUtil.createObj();
                
                // 1. 先尝试从xtJson中提取字段（前端可能把S7字段放在extJson中）
                String extJsonStr = (String) item.get("extJson");
                if (extJsonStr != null && !extJsonStr.isEmpty()) {
                    try {
                        cn.hutool.json.JSONObject frontendExtJson = cn.hutool.json.JSONUtil.parseObj(extJsonStr);
                        // 将extJson中的所有字段提取到item顶层（便于后续处理）
                        frontendExtJson.forEach((key, value) -> {
                            if (!item.containsKey(key)) {
                                item.put(key, value);
                            }
                        });
                    } catch (Exception e) {
                        log.warn("解析extJson失败: {}", extJsonStr);
                    }
                }
                
                // 2. registerAddress作为整数存入extConfig
                if (registerAddress != null && !registerAddress.isEmpty()) {
                    try {
                        extConfig.set("registerAddress", Integer.parseInt(registerAddress));
                    } catch (NumberFormatException e) {
                        extConfig.set("registerAddress", 0);
                    }
                }
                
                // 3. 存储所有可能的协议特定字段
                // Modbus字段
                if (item.get("functionCode") != null) extConfig.set("functionCode", item.get("functionCode"));
                if (item.get("slaveAddress") != null) extConfig.set("slaveAddress", item.get("slaveAddress"));
                else if ("MODBUS_TCP".equals(protocolType)) extConfig.set("slaveAddress", 1);
                if (item.get("bitIndex") != null) extConfig.set("bitIndex", item.get("bitIndex"));
                
                // S7字段（兼容area和storageArea两种字段名）
                Object area = item.get("area") != null ? item.get("area") : item.get("storageArea");
                if (area != null) extConfig.set("storageArea", area);
                if (item.get("dbNumber") != null) extConfig.set("dbNumber", item.get("dbNumber"));
                if (item.get("offset") != null) extConfig.set("offset", item.get("offset"));
                if (item.get("dataTypePrefix") != null) extConfig.set("dataTypePrefix", item.get("dataTypePrefix"));
                
                addressConfig.setExtConfig(extConfig.toString());
                addressConfig.setPollingInterval(0);
                addressConfig.setTimeout(3000);
                addressConfig.setRetryCount(3);
                addressConfig.setEnabled(item.get("enabled") != null ? (Boolean) item.get("enabled") : true);
                addressConfig.setCreateTime(now);
                addressConfig.setUpdateTime(now);
                
                productAddressConfigService.save(addressConfig);
                
            } catch (Exception e) {
                log.error("保存映射配置失败: {}", item.get("identifier"), e);
                throw new RuntimeException("保存映射配置失败: " + e.getMessage());
            }
        }
        
        log.info("成功保存{}条产品属性映射", mappings.size());
        return CommonResult.ok("保存成功");
    }

    @Operation(summary = "删除产品属性映射")
    @SaCheckPermission("/iot/product/edit")
    @DeleteMapping("/delete/{productId}")
    @Transactional(rollbackFor = Exception.class)
    public CommonResult<String> delete(@PathVariable String productId, @RequestBody List<String> ids) {
        log.info("删除产品{}的属性映射，ID列表：{}", productId, ids);
        
        // 1. 先删除关联的地址配置（级联删除）
        for (String mappingId : ids) {
            productAddressConfigService.remove(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<IotProductAddressConfig>()
                    .eq(IotProductAddressConfig::getMappingId, mappingId)
            );
        }
        
        // 2. 删除属性映射
        productPropertyMappingService.removeByIds(ids);
        
        log.info("成功删除{}条产品属性映射", ids.size());
        return CommonResult.ok("删除成功");
    }
}
