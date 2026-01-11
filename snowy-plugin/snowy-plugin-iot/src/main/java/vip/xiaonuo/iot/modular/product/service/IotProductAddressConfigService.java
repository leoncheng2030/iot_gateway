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
package vip.xiaonuo.iot.modular.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import vip.xiaonuo.iot.modular.product.entity.IotProductAddressConfig;

import java.util.List;

/**
 * 产品级地址配置Service接口
 *
 * @author gtc
 * @date  2026/01/11
 **/
public interface IotProductAddressConfigService extends IService<IotProductAddressConfig> {

    /**
     * 根据产品属性映射ID查询地址配置列表
     * @param mappingId 产品属性映射ID
     * @return 地址配置列表
     */
    List<IotProductAddressConfig> listByMappingId(String mappingId);

    /**
     * 根据映射ID和协议类型查询
     * @param mappingId 产品属性映射ID
     * @param protocolType 协议类型
     * @return 地址配置
     */
    IotProductAddressConfig getByMappingIdAndProtocol(String mappingId, String protocolType);

    /**
     * 批量保存产品地址配置
     * @param mappingId 产品属性映射ID
     * @param configs 地址配置列表
     */
    void saveBatch(String mappingId, List<IotProductAddressConfig> configs);

    /**
     * 复制产品地址配置到设备
     * @param productMappingId 产品属性映射ID
     * @param deviceMappingId 设备属性映射ID
     * @return 复制的配置数量
     */
    int copyToDevice(String productMappingId, String deviceMappingId);
}
