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
package vip.xiaonuo.iot.core.protocol;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Component;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.iot.core.protocol.impl.DynamicMqttServer;
import vip.xiaonuo.iot.core.protocol.impl.DynamicTcpServer;
import vip.xiaonuo.iot.core.protocol.impl.DynamicWebSocketServer;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 协议服务工厂
 * 使用注册中心实现自动发现和创建协议实例
 *
 * @author jetox
 * @date 2025/12/11 10:40
 **/
@Component
public class ProtocolServerFactory {

    @Resource
    private DynamicMqttServer.Factory mqttServerFactory;

    @Resource
    private DynamicWebSocketServer.Factory webSocketServerFactory;

    @Resource
    private DynamicTcpServer tcpServer;
    
    @Resource
    private AutowireCapableBeanFactory autowireCapableBeanFactory;

    /**
     * 创建协议服务实例
     * 使用注册中心自动发现协议类
     *
     * @param protocolType 协议类型
     * @return 协议服务实例
     */
    public ProtocolServer createServer(String protocolType) {
        // 从注册中心获取协议类
        String protocolClass = ProtocolRegistry.getProtocolClass(protocolType);
        if (protocolClass == null) {
            throw new CommonException("不支持的协议类型: {}，请确保协议类上已添加@Protocol注解", protocolType);
        }
        
        try {
            // 特殊处理: MQTT和WebSocket需要通过工厂创建（因为依赖Handler）
            if ("MQTT".equals(protocolType)) {
                return mqttServerFactory.create();
            } else if ("WEBSOCKET".equals(protocolType)) {
                return webSocketServerFactory.create();
            } else if ("TCP".equals(protocolType)) {
                // TCP是单例的Spring Bean，直接返回
                return tcpServer;
            }
            
            // 其他协议通过反射创建
            Class<?> clazz = Class.forName(protocolClass);
            ProtocolServer server = (ProtocolServer) clazz.getDeclaredConstructor().newInstance();
            
            // 手动注入Spring依赖
            autowireCapableBeanFactory.autowireBean(server);
            
            return server;
        } catch (Exception e) {
            throw new CommonException("创建协议实例失败: {}", e.getMessage());
        }
    }
    
    /**
     * 获取所有已注册的协议类型列表
     * 用于前端下拉选择
     *
     * @return 协议类型列表
     */
    public List<ProtocolTypeDTO> getAllProtocolTypes() {
        return ProtocolRegistry.getAllProtocolInfo().values().stream()
                .map(info -> new ProtocolTypeDTO(
                        info.getType(),
                        info.getName(),
                        info.getDescription()
                ))
                .sorted((a, b) -> a.getType().compareTo(b.getType()))
                .collect(Collectors.toList());
    }
    
    /**
     * 协议类型DTO
     */
    public static class ProtocolTypeDTO {
        private String type;
        private String name;
        private String description;
        
        public ProtocolTypeDTO(String type, String name, String description) {
            this.type = type;
            this.name = name;
            this.description = description;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
    }
}
