/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 */
package vip.xiaonuo.iot.modular.scada.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.common.page.CommonPageRequest;
import vip.xiaonuo.iot.modular.scada.entity.IotScada;
import vip.xiaonuo.iot.modular.scada.mapper.IotScadaMapper;
import vip.xiaonuo.iot.modular.scada.param.IotScadaAddParam;
import vip.xiaonuo.iot.modular.scada.param.IotScadaEditParam;
import vip.xiaonuo.iot.modular.scada.param.IotScadaIdParam;
import vip.xiaonuo.iot.modular.scada.param.IotScadaPageParam;
import vip.xiaonuo.iot.modular.scada.service.IotScadaService;

import java.util.List;

/**
 * 组态Service实现类
 *
 * @author jetox
 * @date 2025/12/14
 */
@Service
public class IotScadaServiceImpl extends ServiceImpl<IotScadaMapper, IotScada> implements IotScadaService {

    @Override
    public Page<IotScada> page(IotScadaPageParam iotScadaPageParam) {
        LambdaQueryWrapper<IotScada> queryWrapper = new LambdaQueryWrapper<>();
        if (ObjectUtil.isNotEmpty(iotScadaPageParam.getName())) {
            queryWrapper.like(IotScada::getName, iotScadaPageParam.getName());
        }
        queryWrapper.orderByDesc(IotScada::getCreateTime);
        return this.page(CommonPageRequest.defaultPage(), queryWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(IotScadaAddParam iotScadaAddParam) {
        IotScada iotScada = BeanUtil.toBean(iotScadaAddParam, IotScada.class);
        this.save(iotScada);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void edit(IotScadaEditParam iotScadaEditParam) {
        IotScada iotScada = this.queryEntity(iotScadaEditParam.getId());
        BeanUtil.copyProperties(iotScadaEditParam, iotScada);
        this.updateById(iotScada);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<IotScadaIdParam> iotScadaIdParamList) {
        List<String> iotScadaIdList = CollStreamUtil.toList(iotScadaIdParamList, IotScadaIdParam::getId);
        if (ObjectUtil.isNotEmpty(iotScadaIdList)) {
            this.removeByIds(iotScadaIdList);
        }
    }

    @Override
    public IotScada detail(IotScadaIdParam iotScadaIdParam) {
        return this.queryEntity(iotScadaIdParam.getId());
    }

    /**
     * 获取组态实体
     */
    private IotScada queryEntity(String id) {
        IotScada iotScada = this.getById(id);
        if (ObjectUtil.isEmpty(iotScada)) {
            throw new CommonException("组态不存在，id值为：{}", id);
        }
        return iotScada;
    }
}
