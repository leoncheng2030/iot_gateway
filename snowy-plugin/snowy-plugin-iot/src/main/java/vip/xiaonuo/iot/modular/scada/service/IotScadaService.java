/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 */
package vip.xiaonuo.iot.modular.scada.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import vip.xiaonuo.iot.modular.scada.entity.IotScada;
import vip.xiaonuo.iot.modular.scada.param.IotScadaAddParam;
import vip.xiaonuo.iot.modular.scada.param.IotScadaEditParam;
import vip.xiaonuo.iot.modular.scada.param.IotScadaIdParam;
import vip.xiaonuo.iot.modular.scada.param.IotScadaPageParam;

import java.util.List;

/**
 * 组态Service接口
 *
 * @author jetox
 * @date 2025/12/14
 */
public interface IotScadaService extends IService<IotScada> {

    /**
     * 获取组态分页
     */
    Page<IotScada> page(IotScadaPageParam iotScadaPageParam);

    /**
     * 添加组态
     */
    void add(IotScadaAddParam iotScadaAddParam);

    /**
     * 编辑组态
     */
    void edit(IotScadaEditParam iotScadaEditParam);

    /**
     * 删除组态
     */
    void delete(List<IotScadaIdParam> iotScadaIdParamList);

    /**
     * 获取组态详情
     */
    IotScada detail(IotScadaIdParam iotScadaIdParam);
}
