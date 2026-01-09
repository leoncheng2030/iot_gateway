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
package vip.xiaonuo.iot.core.handler;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vip.xiaonuo.iot.core.message.DeviceMessageService;
import vip.xiaonuo.iot.modular.device.entity.IotDevice;
import vip.xiaonuo.iot.modular.device.service.IotDeviceService;
import vip.xiaonuo.iot.modular.gatewaytopo.entity.IotGatewayTopo;
import vip.xiaonuo.iot.modular.gatewaytopo.service.IotGatewayTopoService;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关消息处理器
 *
 * @author yubaoshan
 * @date 2024/12/11 07:55
 **/
@Slf4j
@Component
public class GatewayMessageHandler {

    @Resource
    private IotDeviceService iotDeviceService;

    @Resource
    private IotGatewayTopoService iotGatewayTopoService;

    @Resource
    private DeviceMessageService deviceMessageService;

    /**
     * 处理网关消息
     * 网关Topic格式: /{productKey}/{gatewayKey}/gateway/post
     */
    public void handleGatewayMessage(String topic, String message) {
        try {
            // 解析Topic
            String[] parts = topic.split("/");
            if (parts.length < 4) {
                log.warn("网关消息Topic格式错误: {}", topic);
                return;
            }

            String gatewayKey = parts[2];
            String messageType = parts[3];

            // 获取网关设备
            IotDevice gateway = getDeviceByKey(gatewayKey);
            if (ObjectUtil.isNull(gateway)) {
                log.warn("网关设备不存在 - GatewayKey: {}", gatewayKey);
                return;
            }

            // 解析消息
            JSONObject jsonMessage = JSONUtil.parseObj(message);

            switch (messageType) {
                case "gateway":
                    handleGatewayData(gateway, jsonMessage);
                    break;
                case "topo":
                    handleTopoManage(gateway, jsonMessage);
                    break;
                default:
                    log.warn("未知网关消息类型: {}", messageType);
            }
        } catch (Exception e) {
            log.error("处理网关消息异常 - Topic: {}, Message: {}", topic, message, e);
        }
    }

    /**
     * 处理网关数据(包含子设备数据)
     */
    private void handleGatewayData(IotDevice gateway, JSONObject data) {
        try {
            // 判断是网关自身数据还是子设备数据
            String subDeviceKey = data.getStr("subDeviceKey");
            
            if (subDeviceKey == null) {
                // 网关自身数据
                log.info("处理网关自身数据 - GatewayKey: {}", gateway.getDeviceKey());
                // 处理网关数据
                String topic = String.format("/%s/%s/property/post", 
                        gateway.getProductId(), gateway.getDeviceKey());
                deviceMessageService.handleDeviceMessage(topic, data.toString());
            } else {
                // 子设备数据
                handleSubDeviceData(gateway, subDeviceKey, data);
            }
        } catch (Exception e) {
            log.error("处理网关数据异常 - GatewayId: {}", gateway.getId(), e);
        }
    }

    /**
     * 处理子设备数据
     */
    private void handleSubDeviceData(IotDevice gateway, String subDeviceKey, JSONObject data) {
        try {
            // 查询子设备
            IotDevice subDevice = getDeviceByKey(subDeviceKey);
            if (ObjectUtil.isNull(subDevice)) {
                log.warn("子设备不存在 - SubDeviceKey: {}", subDeviceKey);
                return;
            }

            // 验证拓扑关系
            boolean isValid = iotGatewayTopoService.checkTopoRelation(gateway.getId(), subDevice.getId());
            if (!isValid) {
                log.warn("子设备与网关无拓扑关系 - GatewayKey: {}, SubDeviceKey: {}", 
                        gateway.getDeviceKey(), subDeviceKey);
                return;
            }

            log.info("处理子设备数据 - GatewayKey: {}, SubDeviceKey: {}", 
                    gateway.getDeviceKey(), subDeviceKey);

            // 提取子设备真实数据
            JSONObject subDeviceData = data.getJSONObject("data");
            
            // 构建Topic并处理
            String topic = String.format("/%s/%s/property/post", 
                    subDevice.getProductId(), subDeviceKey);
            deviceMessageService.handleDeviceMessage(topic, subDeviceData.toString());

        } catch (Exception e) {
            log.error("处理子设备数据异常 - SubDeviceKey: {}", subDeviceKey, e);
        }
    }

    /**
     * 处理拓扑管理
     */
    private void handleTopoManage(IotDevice gateway, JSONObject data) {
        try {
            String action = data.getStr("action"); // add/delete/get
            String subDeviceKey = data.getStr("subDeviceKey");

            switch (action) {
                case "add":
                    addSubDevice(gateway, subDeviceKey);
                    break;
                case "delete":
                    deleteSubDevice(gateway, subDeviceKey);
                    break;
                case "get":
                    getSubDevices(gateway);
                    break;
                default:
                    log.warn("未知拓扑管理动作: {}", action);
            }
        } catch (Exception e) {
            log.error("处理拓扑管理异常 - GatewayId: {}", gateway.getId(), e);
        }
    }

    /**
     * 添加子设备
     */
    private void addSubDevice(IotDevice gateway, String subDeviceKey) {
        try {
            IotDevice subDevice = getDeviceByKey(subDeviceKey);
            if (ObjectUtil.isNull(subDevice)) {
                log.warn("子设备不存在 - SubDeviceKey: {}", subDeviceKey);
                return;
            }

            iotGatewayTopoService.bindSubDevice(gateway.getId(), subDevice.getId());
            log.info("添加子设备成功 - GatewayKey: {}, SubDeviceKey: {}", 
                    gateway.getDeviceKey(), subDeviceKey);

            // 下发添加成功响应
            sendTopoResponse(gateway.getDeviceKey(), "add", "success", subDeviceKey);

        } catch (Exception e) {
            log.error("添加子设备失败", e);
            sendTopoResponse(gateway.getDeviceKey(), "add", "failed", subDeviceKey);
        }
    }

    /**
     * 删除子设备
     */
    private void deleteSubDevice(IotDevice gateway, String subDeviceKey) {
        try {
            IotDevice subDevice = getDeviceByKey(subDeviceKey);
            if (ObjectUtil.isNotNull(subDevice)) {
                iotGatewayTopoService.unbindSubDevice(gateway.getId(), subDevice.getId());
                log.info("删除子设备成功 - GatewayKey: {}, SubDeviceKey: {}", 
                        gateway.getDeviceKey(), subDeviceKey);
            }

            sendTopoResponse(gateway.getDeviceKey(), "delete", "success", subDeviceKey);

        } catch (Exception e) {
            log.error("删除子设备失败", e);
            sendTopoResponse(gateway.getDeviceKey(), "delete", "failed", subDeviceKey);
        }
    }

    /**
     * 获取子设备列表
     */
    private void getSubDevices(IotDevice gateway) {
        try {
            // 查询网关下的所有子设备
            List<IotGatewayTopo> topoList = iotGatewayTopoService.getSubDevicesByGatewayId(gateway.getId());
            
            if (ObjectUtil.isEmpty(topoList)) {
                log.info("网关暂无子设备 - GatewayKey: {}", gateway.getDeviceKey());
                sendSubDeviceListResponse(gateway.getDeviceKey(), new ArrayList<>());
                return;
            }

            // 构建子设备列表
            List<JSONObject> subDeviceList = new ArrayList<>();
            for (IotGatewayTopo topo : topoList) {
                IotDevice subDevice = iotDeviceService.getById(topo.getSubDeviceId());
                if (ObjectUtil.isNotNull(subDevice)) {
                    JSONObject deviceInfo = new JSONObject();
                    deviceInfo.set("deviceKey", subDevice.getDeviceKey());
                    deviceInfo.set("deviceName", subDevice.getDeviceName());
                    deviceInfo.set("deviceStatus", subDevice.getDeviceStatus());
                    deviceInfo.set("productId", subDevice.getProductId());
                    subDeviceList.add(deviceInfo);
                }
            }

            // 下发子设备列表
            sendSubDeviceListResponse(gateway.getDeviceKey(), subDeviceList);
            log.info("获取子设备列表成功 - GatewayKey: {}, 子设备数: {}", 
                gateway.getDeviceKey(), subDeviceList.size());
                
        } catch (Exception e) {
            log.error("获取子设备列表失败 - GatewayKey: {}", gateway.getDeviceKey(), e);
        }
    }

    /**
     * 下发子设备列表响应
     */
    private void sendSubDeviceListResponse(String gatewayKey, List<JSONObject> subDeviceList) {
        try {
            JSONObject response = new JSONObject();
            response.set("action", "get");
            response.set("result", "success");
            response.set("subDevices", subDeviceList);
            response.set("count", subDeviceList.size());
            response.set("timestamp", System.currentTimeMillis());

            String topic = String.format("/%s/topo/response", gatewayKey);
            deviceMessageService.sendToDevice(gatewayKey, topic, response.toString());

        } catch (Exception e) {
            log.error("下发子设备列表失败 - GatewayKey: {}", gatewayKey, e);
        }
    }

    /**
     * 下发拓扑响应
     */
    private void sendTopoResponse(String gatewayKey, String action, String result, String subDeviceKey) {
        try {
            JSONObject response = new JSONObject();
            response.set("action", action);
            response.set("result", result);
            response.set("subDeviceKey", subDeviceKey);
            response.set("timestamp", System.currentTimeMillis());

            String topic = String.format("/%s/topo/response", gatewayKey);
            deviceMessageService.sendToDevice(gatewayKey, topic, response.toString());

        } catch (Exception e) {
            log.error("下发拓扑响应失败", e);
        }
    }

    /**
     * 根据DeviceKey查询设备
     */
    private IotDevice getDeviceByKey(String deviceKey) {
        LambdaQueryWrapper<IotDevice> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(IotDevice::getDeviceKey, deviceKey);
        return iotDeviceService.getOne(queryWrapper);
    }
}
