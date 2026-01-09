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

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.iot.modular.protocol.entity.IotProtocol;
import vip.xiaonuo.iot.modular.protocol.service.IotProtocolService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 协议管理器 - 统一管理所有协议实例
 *
 * @author jetox
 * @date 2025/12/11 10:40
 **/
@Slf4j
@Component
public class ProtocolManager {

    @Resource
    private IotProtocolService iotProtocolService;

    @Resource
    private ProtocolServerFactory protocolServerFactory;

    /** 启动时是否自动加载协议 */
    @Value("${iot.protocol.auto-start:true}")
    private boolean autoStart;

    /** 协议实例缓存 Map<协议ID, 协议服务实例> */
    private final Map<String, ProtocolServer> protocolServers = new ConcurrentHashMap<>();

    /**
     * 系统启动时自动加载并启动所有启用的协议
     */
    @PostConstruct
    public void init() {
        // 检查是否开启自动启动
        if (!autoStart) {
            log.info(">>> 协议自动启动已禁用，跳过加载（可通过前端手动启动）");
            return;
        }
        
        log.info(">>> 开始加载协议配置...");
        try {
            LambdaQueryWrapper<IotProtocol> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(IotProtocol::getStatus, "ENABLE");
            
            iotProtocolService.list(queryWrapper).forEach(protocol -> {
                try {
                    startProtocol(protocol.getId());
                } catch (Exception e) {
                    log.error(">>> 协议 [{}] 启动失败: {}", protocol.getProtocolName(), e.getMessage());
                }
            });
            
            log.info(">>> 协议配置加载完成，已启动 {} 个协议服务", protocolServers.size());
        } catch (Exception e) {
            log.error(">>> 协议配置加载失败", e);
        }
    }

    /**
     * 启动协议服务
     *
     * @param protocolId 协议ID
     */
    public void startProtocol(String protocolId) {
        IotProtocol protocol = iotProtocolService.getById(protocolId);
        if (ObjectUtil.isEmpty(protocol)) {
            throw new CommonException("协议不存在");
        }

        if (!"ENABLE".equals(protocol.getStatus())) {
            throw new CommonException("协议未启用，无法启动");
        }

        // 检查是否已经启动
        if (protocolServers.containsKey(protocolId)) {
            log.warn("协议 [{}] 已在运行中，端口: {}", protocol.getProtocolName(), protocol.getProtocolPort());
            throw new CommonException("协议服务已在运行中");
        }

        // 检查端口是否被占用
        checkPortConflict(protocol);

        try {
            // 解析配置
            Map<String, Object> config = parseConfig(protocol);

            // 创建并启动协议服务
            ProtocolServer server = protocolServerFactory.createServer(protocol.getProtocolType());
            server.start(protocol.getProtocolPort(), config);

            // 缓存实例
            protocolServers.put(protocolId, server);

            log.info(">>> 协议 [{}] 启动成功 - 类型: {}, 端口: {}", 
                    protocol.getProtocolName(), 
                    protocol.getProtocolType(), 
                    protocol.getProtocolPort());

        } catch (Exception e) {
            // 启动失败，清理缓存
            protocolServers.remove(protocolId);
            log.error(">>> 协议 [{}] 启动失败", protocol.getProtocolName(), e);
            throw new CommonException("协议启动失败: {}", e.getMessage());
        }
    }

    /**
     * 停止协议服务
     *
     * @param protocolId 协议ID
     */
    public void stopProtocol(String protocolId) {
        ProtocolServer server = protocolServers.get(protocolId);
        if (ObjectUtil.isEmpty(server)) {
            throw new CommonException("协议服务未运行");
        }

        try {
            server.stop();
            protocolServers.remove(protocolId);

            IotProtocol protocol = iotProtocolService.getById(protocolId);
            log.info(">>> 协议 [{}] 已停止", protocol.getProtocolName());

        } catch (Exception e) {
            log.error(">>> 协议停止失败", e);
            throw new CommonException("协议停止失败: {}", e.getMessage());
        }
    }

    /**
     * 重启协议服务
     *
     * @param protocolId 协议ID
     */
    public void restartProtocol(String protocolId) {
        if (protocolServers.containsKey(protocolId)) {
            stopProtocol(protocolId);
        }
        startProtocol(protocolId);
    }

    /**
     * 获取协议运行状态
     *
     * @param protocolId 协议 ID
     * @return true-运行中 false-已停止
     */
    public boolean isRunning(String protocolId) {
        return protocolServers.containsKey(protocolId);
    }

    /**
     * 检查端口冲突
     */
    private void checkPortConflict(IotProtocol protocol) {
        for (Map.Entry<String, ProtocolServer> entry : protocolServers.entrySet()) {
            ProtocolServer server = entry.getValue();
            if (server.getPort().equals(protocol.getProtocolPort())) {
                IotProtocol existProtocol = iotProtocolService.getById(entry.getKey());
                throw new CommonException("端口 {} 已被协议 [{}] 占用", 
                        protocol.getProtocolPort(), 
                        existProtocol.getProtocolName());
            }
        }
    }

    /**
     * 解析协议配置
     */
    private Map<String, Object> parseConfig(IotProtocol protocol) {
        if (ObjectUtil.isEmpty(protocol.getConfigJson())) {
            return new ConcurrentHashMap<>();
        }
        try {
            return JSONUtil.toBean(protocol.getConfigJson(), Map.class);
        } catch (Exception e) {
            log.warn(">>> 协议配置解析失败，使用默认配置");
            return new ConcurrentHashMap<>();
        }
    }
}
