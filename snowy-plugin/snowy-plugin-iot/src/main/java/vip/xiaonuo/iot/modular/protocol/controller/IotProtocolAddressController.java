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
package vip.xiaonuo.iot.modular.protocol.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import vip.xiaonuo.common.pojo.CommonResult;
import vip.xiaonuo.iot.core.protocol.ProtocolRegistry;
import vip.xiaonuo.iot.core.protocol.ProtocolServer;
import vip.xiaonuo.iot.core.protocol.ProtocolServerFactory;
import vip.xiaonuo.iot.core.protocol.address.AddressConfigProvider;
import vip.xiaonuo.iot.core.protocol.address.AddressConfigTemplate;
import vip.xiaonuo.iot.core.protocol.address.ModbusAddressConfigProvider;

/**
 * 协议地址配置控制器
 *
 * @author jetox
 * @date 2026/01/10 17:40
 **/
@Tag(name = "协议地址配置")
@RestController
public class IotProtocolAddressController {

    @Resource
    private ProtocolServerFactory protocolServerFactory;

    @Resource
    private ModbusAddressConfigProvider modbusAddressConfigProvider;

    /**
     * 获取协议的地址配置模板
     *
     * @param protocolType 协议类型
     * @return 地址配置模板
     */
    @Operation(summary = "获取协议地址配置模板")
    @GetMapping("/iot/protocol/address/template/{protocolType}")
    public CommonResult<AddressConfigTemplate> getAddressTemplate(@PathVariable String protocolType) {
        try {
            // 特殊处理：Modbus 使用 Driver 机制，单独提供配置
            if ("MODBUS_TCP".equalsIgnoreCase(protocolType)) {
                return CommonResult.data(modbusAddressConfigProvider.getModbusTcpTemplate());
            } else if ("MODBUS_RTU".equalsIgnoreCase(protocolType)) {
                return CommonResult.data(modbusAddressConfigProvider.getModbusRtuTemplate());
            }
            
            // 其他协议：从 ProtocolServer 获取
            // 创建协议服务实例
            ProtocolServer server = protocolServerFactory.createServer(protocolType);
            
            // 检查是否实现了 AddressConfigProvider 接口
            if (server instanceof AddressConfigProvider) {
                AddressConfigProvider provider = (AddressConfigProvider) server;
                return CommonResult.data(provider.getAddressConfigTemplate());
            } else {
                return CommonResult.error("协议 " + protocolType + " 不支持地址配置");
            }
        } catch (Exception e) {
            return CommonResult.error("获取地址配置模板失败: " + e.getMessage());
        }
    }
}
