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
import vip.xiaonuo.iot.modular.product.entity.IotProductPropertyMapping;

import java.util.List;

/**
 * 产品属性映射Service接口
 *
 * @author gtc
 * @date  2026/01/11
 **/
public interface IotProductPropertyMappingService extends IService<IotProductPropertyMapping> {

    /**
     * 根据产品ID查询所有属性映射
     * @param productId 产品ID
     * @return 属性映射列表
     */
    List<IotProductPropertyMapping> listByProductId(String productId);

    /**
     * 根据产品ID和标识符查询
     * @param productId 产品ID
     * @param identifier 属性标识符
     * @return 属性映射
     */
    IotProductPropertyMapping getByProductIdAndIdentifier(String productId, String identifier);

    /**
     * 批量保存产品属性映射
     * @param productId 产品ID
     * @param mappings 属性映射列表
     */
    void saveBatch(String productId, List<IotProductPropertyMapping> mappings);

    /**
     * 复制产品属性映射到设备
     * @param productId 产品ID
     * @param deviceId 设备ID
     * @return 复制的映射数量
     */
    int copyToDevice(String productId, String deviceId);
}
