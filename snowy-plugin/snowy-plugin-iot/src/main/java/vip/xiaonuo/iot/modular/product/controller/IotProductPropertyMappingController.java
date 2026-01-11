/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 */
package vip.xiaonuo.iot.modular.product.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import vip.xiaonuo.iot.modular.thingmodel.entity.IotThingModel;
import vip.xiaonuo.iot.modular.thingmodel.service.IotThingModelService;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    
    @Resource
    private IotThingModelService thingModelService;

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
                                
                // 通用化处理：自动提取 extConfig 中的所有字段到顶层
                // 这样新增协议时，前端不需要修改代码
                if (config.getExtConfig() != null && !config.getExtConfig().isEmpty()) {
                    try {
                        cn.hutool.json.JSONObject extConfig = cn.hutool.json.JSONUtil.parseObj(config.getExtConfig());
                        // 将 extConfig 中的所有字段提取到顶层
                        extConfig.forEach((key, value) -> {
                            // 避免覆盖已有的顶层字段
                            if (!item.containsKey(key)) {
                                // 将 Hutool 的 JSONNull 转换为 Java 的 null，避免 Jackson 序列化错误
                                if (value instanceof cn.hutool.json.JSONNull) {
                                    item.put(key, null);
                                } else {
                                    item.put(key, value);
                                }
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
            
            // 批量删除地址配置
            productAddressConfigService.remove(
                new LambdaQueryWrapper<IotProductAddressConfig>()
                    .in(IotProductAddressConfig::getMappingId, mappingIds)
            );
            
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
                addressConfig.setProtocolType(protocolType);
                
                // 设置deviceAddress（必填字段）
                // 处理前端可能传递 String 或 Integer 类型的地址
                Object registerAddressObj = item.get("registerAddress");
                String registerAddress = null;
                if (registerAddressObj != null) {
                    registerAddress = registerAddressObj.toString();
                }
                addressConfig.setDeviceAddress(registerAddress != null && !registerAddress.isEmpty() ? registerAddress : "0");
                
                // 自动从物模型获取dataType（如果前端没有传）
                String dataType = (String) item.get("dataType");
                if (dataType == null || dataType.isEmpty()) {
                    // 从物模型查询valueType
                    String thingModelId = (String) item.get("thingModelId");
                    if (thingModelId != null) {
                        IotThingModel thingModel = thingModelService.getById(thingModelId);
                        if (thingModel != null && thingModel.getValueType() != null) {
                            // 映射物模型valueType到协议数据类型
                            dataType = mapValueTypeToProtocolDataType(thingModel.getValueType(), protocolType);
                        }
                    }
                }
                addressConfig.setDataType(dataType);
                
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
                    "id", "productId", "createTime", "updateTime"
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
        
        // 1. 批量删除关联的地址配置（级联删除）
        productAddressConfigService.remove(
            new LambdaQueryWrapper<IotProductAddressConfig>()
                .in(IotProductAddressConfig::getMappingId, ids)
        );
        
        // 2. 删除属性映射
        productPropertyMappingService.removeByIds(ids);
        
        log.info("成功删除{}条产品属性映射", ids.size());
        return CommonResult.ok("删除成功");
    }
    
    /**
     * 映射物模型 valueType 到协议数据类型
     * @param valueType 物模型数据类型 (int32, float, double, bool, string)
     * @param protocolType 协议类型 (S7, MODBUS_TCP, OPC_UA等)
     * @return 协议特定的数据类型
     */
    private String mapValueTypeToProtocolDataType(String valueType, String protocolType) {
        if (valueType == null || valueType.isEmpty()) {
            return "int"; // 默认值
        }
        
        // S7协议的映射
        if ("S7".equalsIgnoreCase(protocolType)) {
            switch (valueType.toLowerCase()) {
                case "int32":
                case "int":
                    return "int";      // 2字节有符号整数
                case "dint":
                    return "dint";     // 4字节整数
                case "float":
                case "double":
                    return "float";    // 4字节浮点数 (S7中的REAL类型)
                case "bool":
                case "boolean":
                    return "bool";
                case "word":
                    return "word";     // 2字节无符号整数
                case "byte":
                    return "byte";
                default:
                    log.warn("未知的valueType: {}, 默认使用int", valueType);
                    return "int";
            }
        }
        
        // Modbus协议的映射
        if ("MODBUS_TCP".equalsIgnoreCase(protocolType) || "MODBUS_RTU".equalsIgnoreCase(protocolType)) {
            switch (valueType.toLowerCase()) {
                case "int32":
                case "int":
                    return "int16";
                case "float":
                case "double":
                    return "float32";
                case "bool":
                case "boolean":
                    return "bool";
                default:
                    return "int16";
            }
        }
        
        // 其他协议直接使用valueType
        return valueType;
    }
}
