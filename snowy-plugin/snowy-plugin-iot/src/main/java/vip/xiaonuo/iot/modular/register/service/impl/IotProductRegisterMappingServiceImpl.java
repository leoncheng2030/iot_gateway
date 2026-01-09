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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.xiaonuo.iot.modular.register.entity.IotProductRegisterMapping;
import vip.xiaonuo.iot.modular.register.mapper.IotProductRegisterMappingMapper;
import vip.xiaonuo.iot.modular.register.service.IotProductRegisterMappingService;

import java.math.BigDecimal;
import java.util.List;

/**
 * 产品寄存器映射Service接口实现类
 *
 * @author jetox
 * @date  2025/12/13 07:45
 **/
@Slf4j
@Service
public class IotProductRegisterMappingServiceImpl extends ServiceImpl<IotProductRegisterMappingMapper, IotProductRegisterMapping>
        implements IotProductRegisterMappingService {

    @Override
    public List<IotProductRegisterMapping> getProductRegisterMappings(String productId) {
        LambdaQueryWrapper<IotProductRegisterMapping> query = new LambdaQueryWrapper<>();
        query.eq(IotProductRegisterMapping::getProductId, productId)
             .eq(IotProductRegisterMapping::getEnabled, true)
             .orderByAsc(IotProductRegisterMapping::getRegisterAddress);
        List<IotProductRegisterMapping> mappings = this.list(query);
        log.debug("产品 {} 的寄存器映射，共 {} 条", productId, mappings.size());
        return mappings;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSaveProductRegisterMappings(String productId, List<IotProductRegisterMapping> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            return;
        }

        // 1. 查询现有的映射配置
        LambdaQueryWrapper<IotProductRegisterMapping> query = new LambdaQueryWrapper<>();
        query.eq(IotProductRegisterMapping::getProductId, productId);
        List<IotProductRegisterMapping> existingMappings = this.list(query);

        // 构建现有映射的Map（以thingModelId为key）
        java.util.Map<String, IotProductRegisterMapping> existingMap = new java.util.HashMap<>();
        for (IotProductRegisterMapping existing : existingMappings) {
            existingMap.put(existing.getThingModelId(), existing);
        }

        // 2. 区分新增和更新
        java.util.List<IotProductRegisterMapping> toInsert = new java.util.ArrayList<>();
        java.util.List<IotProductRegisterMapping> toUpdate = new java.util.ArrayList<>();

        for (IotProductRegisterMapping mapping : mappings) {
            // 设置默认值
            mapping.setProductId(productId);
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
            IotProductRegisterMapping existing = existingMap.get(mapping.getThingModelId());
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
            log.info("批量新增产品寄存器映射 - ProductId: {}, Count: {}", productId, toInsert.size());
        }
        if (!toUpdate.isEmpty()) {
            this.updateBatchById(toUpdate);
            log.info("批量更新产品寄存器映射 - ProductId: {}, Count: {}", productId, toUpdate.size());
        }

        // 4. 删除不再使用的映射（物理删除）
        if (!existingMap.isEmpty()) {
            java.util.List<String> toDeleteIds = new java.util.ArrayList<>(existingMap.values().stream()
                .map(IotProductRegisterMapping::getId)
                .collect(java.util.stream.Collectors.toList()));
            this.removeByIds(toDeleteIds);
            log.info("批量删除产品寄存器映射 - ProductId: {}, Count: {}", productId, toDeleteIds.size());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByProductId(String productId) {
        // 使用物理删除，避免逻辑删除导致的唯一键冲突
        int count = this.getBaseMapper().physicalDeleteByProductId(productId);
        log.info("物理删除产品寄存器映射 - ProductId: {}, 删除数量: {}", productId, count);
    }
}
