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
package vip.xiaonuo.iot.modular.register.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.xiaonuo.iot.modular.device.entity.IotDevice;
import vip.xiaonuo.iot.modular.devicedriver.entity.IotDeviceDriver;
import vip.xiaonuo.iot.modular.devicedriverrel.entity.IotDeviceDriverRel;
import vip.xiaonuo.iot.modular.register.entity.IotDeviceRegisterMapping;
import vip.xiaonuo.iot.modular.register.entity.IotProductRegisterMapping;
import vip.xiaonuo.iot.modular.register.mapper.IotDeviceRegisterMappingMapper;
import vip.xiaonuo.iot.modular.register.service.IotDeviceRegisterMappingService;
import vip.xiaonuo.iot.modular.register.service.IotProductRegisterMappingService;
import vip.xiaonuo.iot.modular.device.service.IotDeviceService;
import vip.xiaonuo.iot.modular.devicedriver.service.IotDeviceDriverService;
import vip.xiaonuo.iot.modular.devicedriverrel.service.IotDeviceDriverRelService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 设备寄存器映射Service实现类
 *
 * @author jetox
 * @date  2025/12/12 12:33
 **/
@Slf4j
@Service
public class IotDeviceRegisterMappingServiceImpl extends ServiceImpl<IotDeviceRegisterMappingMapper, IotDeviceRegisterMapping>
        implements IotDeviceRegisterMappingService {

    @Resource
    private IotDeviceService iotDeviceService;

    @Resource
    private IotDeviceDriverService iotDeviceDriverService;

    @Resource
    private IotDeviceDriverRelService iotDeviceDriverRelService;

    @Resource
    private IotProductRegisterMappingService iotProductRegisterMappingService;

    @Override
    public List<IotDeviceRegisterMapping> getDeviceRegisterMappings(String deviceId) {
        // 1. 优先查询设备级配置
        LambdaQueryWrapper<IotDeviceRegisterMapping> deviceQuery = new LambdaQueryWrapper<>();
        deviceQuery.eq(IotDeviceRegisterMapping::getDeviceId, deviceId)
                   .eq(IotDeviceRegisterMapping::getEnabled, true)
                   .orderByAsc(IotDeviceRegisterMapping::getRegisterAddress);
        List<IotDeviceRegisterMapping> deviceMappings = this.list(deviceQuery);

        if (!deviceMappings.isEmpty()) {
            log.debug("设备 {} 使用设备级寄存器映射，共 {} 条", deviceId, deviceMappings.size());
            return deviceMappings;
        }

        // 获取设备信息
        IotDevice device = iotDeviceService.getById(deviceId);
        if (device == null) {
            log.warn("设备不存在 - DeviceId: {}", deviceId);
            return new ArrayList<>();
        }

        // 2. 设备级不存在，查询产品级配置
        List<IotProductRegisterMapping> productMappings = iotProductRegisterMappingService.getProductRegisterMappings(device.getProductId());
        if (!productMappings.isEmpty()) {
            // 将产品级映射转换为设备级映射（用于统一返回格式）
            List<IotDeviceRegisterMapping> convertedMappings = convertFromProductMapping(device.getId(), productMappings);
            log.debug("设备 {} 使用产品级寄存器映射，共 {} 条", deviceId, convertedMappings.size());
            return convertedMappings;
        }

        // 3. 产品级也不存在
        // 检查设备是否绑定了 Modbus 驱动，只对绑定了 Modbus 驱动的设备打印警告
        LambdaQueryWrapper<IotDeviceDriverRel> relQuery = new LambdaQueryWrapper<>();
        relQuery.eq(IotDeviceDriverRel::getDeviceId, deviceId);
        List<IotDeviceDriverRel> driverRels = iotDeviceDriverRelService.list(relQuery);
        
        if (!driverRels.isEmpty()) {
            // 提取驱动ID列表
            List<String> driverIds = driverRels.stream()
                .map(IotDeviceDriverRel::getDriverId)
                .toList();
            
            // 查询这些驱动中是否有 MODBUS_TCP 类型的
            LambdaQueryWrapper<IotDeviceDriver> driverQuery = new LambdaQueryWrapper<>();
            driverQuery.in(IotDeviceDriver::getId, driverIds)
                       .eq(IotDeviceDriver::getDriverType, "MODBUS_TCP");
            long modbusDriverCount = iotDeviceDriverService.count(driverQuery);
            
            if (modbusDriverCount > 0) {
                log.warn("设备 {} 绑定了Modbus驱动但未配置寄存器映射（设备级和产品级均未配置）", deviceId);
            } else {
                log.debug("设备 {} 未配置寄存器映射（未绑定Modbus驱动，无需配置）", deviceId);
            }
        } else {
            log.debug("设备 {} 未配置寄存器映射（未绑定任何驱动，无需配置）", deviceId);
        }
        return new ArrayList<>();
    }

    @Override
    public Map<String, IotDeviceRegisterMapping> getDeviceRegisterMappingMap(String deviceId) {
        List<IotDeviceRegisterMapping> mappings = getDeviceRegisterMappings(deviceId);
        return mappings.stream()
                .collect(Collectors.toMap(
                        IotDeviceRegisterMapping::getIdentifier,
                        m -> m,
                        (existing, replacement) -> existing
                ));
    }

    @Override
    public Map<Integer, IotDeviceRegisterMapping> getRegisterMappingByFunctionCode(String deviceId, String functionCode) {
        List<IotDeviceRegisterMapping> mappings = getDeviceRegisterMappings(deviceId);
        return mappings.stream()
                .filter(m -> functionCode.equals(m.getFunctionCode()))
                .collect(Collectors.toMap(
                        IotDeviceRegisterMapping::getRegisterAddress,
                        m -> m,
                        (existing, replacement) -> existing
                ));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSaveDeviceRegisterMappings(String deviceId, List<IotDeviceRegisterMapping> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            return;
        }

        // 1. 查询现有的映射配置
        LambdaQueryWrapper<IotDeviceRegisterMapping> query = new LambdaQueryWrapper<>();
        query.eq(IotDeviceRegisterMapping::getDeviceId, deviceId);
        List<IotDeviceRegisterMapping> existingMappings = this.list(query);

        // 构建现有映射的Map（以thingModelId为key）
        java.util.Map<String, IotDeviceRegisterMapping> existingMap = new java.util.HashMap<>();
        for (IotDeviceRegisterMapping existing : existingMappings) {
            existingMap.put(existing.getThingModelId(), existing);
        }

        // 2. 区分新增和更新
        java.util.List<IotDeviceRegisterMapping> toInsert = new java.util.ArrayList<>();
        java.util.List<IotDeviceRegisterMapping> toUpdate = new java.util.ArrayList<>();

        for (IotDeviceRegisterMapping mapping : mappings) {
            // 设置默认值
            mapping.setDeviceId(deviceId);
            if (mapping.getEnabled() == null) {
                mapping.setEnabled(true);
            }
            if (mapping.getScaleFactor() == null) {
                mapping.setScaleFactor(new BigDecimal("1.0"));
            }
            if (mapping.getOffset() == null) {
                mapping.setOffset(new BigDecimal("0.0"));
            }
            if (StrUtil.isBlank(mapping.getByteOrder())) {
                mapping.setByteOrder("BIG_ENDIAN");
            }

            // 判断是新增还是更新
            IotDeviceRegisterMapping existing = existingMap.get(mapping.getThingModelId());
            if (existing != null) {
                // 更新：使用现有ID
                mapping.setId(existing.getId());
                toUpdate.add(mapping);
                // 从现有Map中移除，剩余的需要删除
                existingMap.remove(mapping.getThingModelId());
            } else {
                // 新增：生成新ID
                mapping.setId(IdWorker.getIdStr());
                toInsert.add(mapping);
            }
        }

        // 3. 执行数据库操作
        if (!toInsert.isEmpty()) {
            this.saveBatch(toInsert);
            log.info("批量新增设备寄存器映射 - DeviceId: {}, Count: {}", deviceId, toInsert.size());
        }
        if (!toUpdate.isEmpty()) {
            this.updateBatchById(toUpdate);
            log.info("批量更新设备寄存器映射 - DeviceId: {}, Count: {}", deviceId, toUpdate.size());
        }

        // 4. 删除不再使用的映射（物理删除）
        if (!existingMap.isEmpty()) {
            java.util.List<String> toDeleteIds = new java.util.ArrayList<>(existingMap.values().stream()
                .map(IotDeviceRegisterMapping::getId)
                .collect(java.util.stream.Collectors.toList()));
            this.removeByIds(toDeleteIds);
            log.info("批量删除设备寄存器映射 - DeviceId: {}, Count: {}", deviceId, toDeleteIds.size());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByDeviceId(String deviceId) {
        // 使用物理删除，避免逻辑删除导致的唯一键冲突
        int count = this.getBaseMapper().physicalDeleteByDeviceId(deviceId);
        log.info("物理删除设备寄存器映射 - DeviceId: {}, 删除数量: {}", deviceId, count);
    }

    /**
     * 从产品级映射转换为设备级映射（用于统一返回格式）
     */
    private List<IotDeviceRegisterMapping> convertFromProductMapping(String deviceId, List<IotProductRegisterMapping> productMappings) {
        List<IotDeviceRegisterMapping> deviceMappings = new ArrayList<>();
        for (IotProductRegisterMapping productMapping : productMappings) {
            IotDeviceRegisterMapping deviceMapping = new IotDeviceRegisterMapping();
            deviceMapping.setId(IdWorker.getIdStr());
            deviceMapping.setDeviceId(deviceId);
            deviceMapping.setThingModelId(productMapping.getThingModelId());
            deviceMapping.setIdentifier(productMapping.getIdentifier());
            deviceMapping.setRegisterAddress(productMapping.getRegisterAddress());
            deviceMapping.setFunctionCode(productMapping.getFunctionCode());
            deviceMapping.setDataType(productMapping.getDataType());
            deviceMapping.setScaleFactor(productMapping.getScaleFactor());
            deviceMapping.setOffset(productMapping.getOffset());
            deviceMapping.setBitIndex(productMapping.getBitIndex());
            deviceMapping.setByteOrder(productMapping.getByteOrder());
            deviceMapping.setEnabled(productMapping.getEnabled());
            deviceMapping.setRemark(productMapping.getRemark());
            deviceMapping.setSortCode(productMapping.getSortCode());
            deviceMapping.setExtJson(productMapping.getExtJson());
            deviceMappings.add(deviceMapping);
        }
        return deviceMappings;
    }
}
