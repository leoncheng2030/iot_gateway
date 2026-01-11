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
package vip.xiaonuo.iot.core.message;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vip.xiaonuo.iot.core.notification.IotNotificationService;
import vip.xiaonuo.iot.core.protocol.modbus.Modbus4jTcpClient;
import vip.xiaonuo.iot.core.util.DriverConfigUtil;
import vip.xiaonuo.iot.modular.device.entity.IotDevice;
import vip.xiaonuo.iot.modular.device.service.IotDeviceService;
import vip.xiaonuo.iot.modular.devicedriverrel.entity.IotDeviceDriverRel;
import vip.xiaonuo.iot.modular.devicedriverrel.service.IotDeviceDriverRelService;
import vip.xiaonuo.iot.modular.device.entity.IotDeviceAddressConfig;
import vip.xiaonuo.iot.modular.device.service.IotDevicePropertyMappingService;
import vip.xiaonuo.iot.modular.rule.entity.IotRule;
import vip.xiaonuo.iot.modular.rule.service.IotRuleService;
import vip.xiaonuo.iot.modular.rulelog.entity.IotRuleLog;
import vip.xiaonuo.iot.modular.rulelog.service.IotRuleLogService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 规则引擎服务实现
 *
 * @author yubaoshan
 * @date 2024/12/11 07:40
 **/
@Slf4j
@Service
public class RuleEngineServiceImpl implements RuleEngineService {

    @Resource
    private IotRuleService iotRuleService;

    @Resource
    private IotRuleLogService iotRuleLogService;

    @Resource
    private IotNotificationService iotNotificationService;

    @Resource
    private IotDeviceService iotDeviceService;

    @Resource
    private DeviceMessageService deviceMessageService;
    
    @Resource
    private Modbus4jTcpClient modbus4jTcpClient;

    @Resource
    private IotDevicePropertyMappingService iotDevicePropertyMappingService;
    
    @Resource
    private IotDeviceDriverRelService iotDeviceDriverRelService;

    @Override
    public void triggerByDeviceData(String deviceId, JSONObject data) {
        try {
            // 使用MyBatis-Plus查询启用的设备触发规则
            LambdaQueryWrapper<IotRule> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(IotRule::getRuleType, "DEVICE")
                    .eq(IotRule::getStatus, "ENABLE");  // 修复：数据库中是ENABLE不ENABLED
            List<IotRule> rules = iotRuleService.list(queryWrapper);
            for (IotRule rule : rules) {
                try {
                    // 解析工作流数据
                    String workflowData = rule.getWorkflowData();
                    if (StrUtil.isBlank(workflowData)) {
                        log.warn("规则工作流数据为空 - RuleId: {}", rule.getId());
                        continue;
                    }
                    
                    JSONObject workflow = JSONUtil.parseObj(workflowData);
                    JSONArray nodes = workflow.getJSONArray("nodes");
                    JSONArray edges = workflow.getJSONArray("edges");
                    if (nodes == null || nodes.isEmpty()) {
                        continue;
                    }
                    
                    // 查找触发器节点
                    for (int i = 0; i < nodes.size(); i++) {
                        JSONObject node = nodes.getJSONObject(i);
                        if (!"trigger".equals(node.getStr("type"))) {
                            continue;
                        }
                        
                        JSONObject properties = node.getJSONObject("properties");
                        if (properties == null) {
                            continue;
                        }
                        
                        // 检查是否为设备触发器
                        if (!"device".equals(properties.getStr("triggerType"))) {
                            continue;
                        }
                        
                        // 检查设备ID是否匹配
                        String triggerDeviceId = properties.getStr("deviceId");
                        if (!deviceId.equals(triggerDeviceId)) {
                            log.debug("设备ID不匹配，跳过此规则 - 期望: {}, 实际: {}", triggerDeviceId, deviceId);
                            continue;
                        }
                        
                        // 找到匹配的触发器，查找连接的条件节点
                        String triggerNodeId = node.getStr("id");
                        boolean conditionMet = evaluateWorkflowConditions(nodes, edges, triggerNodeId, data);
                        
                        if (conditionMet) {
                            log.info("规则条件满足，执行工作流 - RuleId: {}", rule.getId());
                            try {
                                executeWorkflow(workflow, data);
                                // 执行成功，保存日志
                                saveRuleLog(rule, data.toString(), "SUCCESS", null);
                            } catch (Exception workflowException) {
                                // 执行失败，保存错误日志
                                log.error("工作流执行失败 - RuleId: {}", rule.getId(), workflowException);
                                saveRuleLog(rule, data.toString(), "FAILED", workflowException.getMessage());
                            }
                        } else {
                            log.info("规则条件不满足 - RuleId: {}", rule.getId());
                        }
                    }
                } catch (Exception e) {
                    log.error("规则执行异常 - RuleId: {}", rule.getId(), e);
                    saveRuleLog(rule, data.toString(), "FAILED", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("规则引擎处理异常 - DeviceId: {}", deviceId, e);
        }
    }

    @Override
    public void triggerByTimer() {
        try {
            // 使用MyBatis-Plus查询启用的定时触发规则
            LambdaQueryWrapper<IotRule> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(IotRule::getRuleType, "TIMER")
                    .eq(IotRule::getStatus, "ENABLE");  // 修复：数据库中是ENABLE不ENABLED
            List<IotRule> rules = iotRuleService.list(queryWrapper);
            
            for (IotRule rule : rules) {
                try {
                    log.info("定时规则触发 - RuleId: {}, RuleName: {}", rule.getId(), rule.getRuleName());
                    
                    // 解析工作流数据
                    String workflowData = rule.getWorkflowData();
                    if (StrUtil.isBlank(workflowData)) {
                        log.warn("规则工作流数据为空 - RuleId: {}", rule.getId());
                        continue;
                    }
                    
                    JSONObject workflow = JSONUtil.parseObj(workflowData);
                    try {
                        executeWorkflow(workflow, null);
                        // 执行成功，保存日志
                        saveRuleLog(rule, null, "SUCCESS", null);
                    } catch (Exception workflowException) {
                        // 执行失败，保存错误日志
                        log.error("工作流执行失败 - RuleId: {}", rule.getId(), workflowException);
                        saveRuleLog(rule, null, "FAILED", workflowException.getMessage());
                    }
                } catch (Exception e) {
                    log.error("定时规则执行异常 - RuleId: {}", rule.getId(), e);
                    saveRuleLog(rule, null, "FAILED", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("定时规则引擎处理异常", e);
        }
    }

    /**
     * 评估工作流条件是否满足
     * @param nodes 所有节点
     * @param edges 所有连线
     * @param triggerNodeId 触发器节点ID
     * @param data 设备数据
     * @return 条件是否满足
     */
    private boolean evaluateWorkflowConditions(JSONArray nodes, JSONArray edges, String triggerNodeId, JSONObject data) {
        // 查找从触发器连接出来的条件节点
        if (edges == null || edges.isEmpty()) {
            log.warn("工作流没有连线，默认条件满足");
            return true; // 没有连线则直接执行
        }
        
        // 查找所有从触发器连接的条件节点
        List<String> conditionNodeIds = new ArrayList<>();
        for (int i = 0; i < edges.size(); i++) {
            JSONObject edge = edges.getJSONObject(i);
            String sourceNodeId = edge.getStr("sourceNodeId");
            String targetNodeId = edge.getStr("targetNodeId");
            
            if (triggerNodeId.equals(sourceNodeId)) {
                // 找到触发器的下游节点
                conditionNodeIds.add(targetNodeId);
            }
        }
        
        if (conditionNodeIds.isEmpty()) {
            log.warn("触发器没有连接条件节点，默认条件满足");
            return true; // 没有条件节点则直接执行
        }
        
        // 评估所有条件节点
        for (String conditionNodeId : conditionNodeIds) {
            // 查找条件节点
            JSONObject conditionNode = null;
            for (int i = 0; i < nodes.size(); i++) {
                JSONObject node = nodes.getJSONObject(i);
                if (conditionNodeId.equals(node.getStr("id"))) {
                    conditionNode = node;
                    break;
                }
            }
            
            if (conditionNode == null) {
                log.warn("未找到条件节点: {}", conditionNodeId);
                continue;
            }
            
            String nodeType = conditionNode.getStr("type");
            if (!"condition".equals(nodeType)) {
                log.debug("节点不是条件节点，跳过: {}", nodeType);
                continue;
            }
            
            JSONObject properties = conditionNode.getJSONObject("properties");
            if (properties == null) {
                log.warn("条件节点属性为null");
                continue;
            }
            
            // 检查条件类型
            String conditionType = properties.getStr("conditionType");
            if ("group".equals(conditionType)) {
                // 条件组：递归评估子条件
                String logic = properties.getStr("logic"); // AND 或 OR
                boolean groupResult = evaluateConditionGroup(nodes, edges, conditionNodeId, data, logic);
                if (!groupResult) {
                    log.info("条件组不满足: {}", conditionNodeId);
                    return false;
                }
            } else {
                // 简单条件：直接评估
                boolean conditionMet = evaluateCondition(properties, data);
                if (!conditionMet) {
                    log.info("条件节点不满足: {}", conditionNodeId);
                    return false;
                }
            }
        }
        
        log.info("所有条件节点都满足");
        return true;
    }

    /**
     * 评估条件组是否满足
     * @param nodes 所有节点
     * @param edges 所有连线
     * @param groupNodeId 条件组节点ID
     * @param data 设备数据
     * @param logic 逻辑关系 (AND/OR)
     * @return 条件组是否满足
     */
    private boolean evaluateConditionGroup(JSONArray nodes, JSONArray edges, String groupNodeId, JSONObject data, String logic) {
        log.info("评估条件组 - GroupId: {}, Logic: {}", groupNodeId, logic);
        
        // 查找从条件组连接出来的子条件节点
        List<String> childConditionIds = new ArrayList<>();
        for (int i = 0; i < edges.size(); i++) {
            JSONObject edge = edges.getJSONObject(i);
            String sourceNodeId = edge.getStr("sourceNodeId");
            String targetNodeId = edge.getStr("targetNodeId");
            
            if (groupNodeId.equals(sourceNodeId)) {
                childConditionIds.add(targetNodeId);
            }
        }
        
        if (childConditionIds.isEmpty()) {
            log.warn("条件组没有子条件节点: {}", groupNodeId);
            return false;
        }
        
        log.info("条件组包含 {} 个子条件", childConditionIds.size());
        
        // 根据逻辑关系评估子条件
        if ("AND".equals(logic)) {
            // AND 逻辑：所有子条件都必须满足
            for (String childId : childConditionIds) {
                if (!evaluateChildCondition(nodes, edges, childId, data)) {
                    log.info("AND 逻辑 - 子条件不满足: {}", childId);
                    return false;
                }
            }
            log.info("AND 逻辑 - 所有子条件都满足");
            return true;
        } else if ("OR".equals(logic)) {
            // OR 逻辑：任一子条件满足即可
            for (String childId : childConditionIds) {
                if (evaluateChildCondition(nodes, edges, childId, data)) {
                    log.info("OR 逻辑 - 子条件满足: {}", childId);
                    return true;
                }
            }
            log.info("OR 逻辑 - 所有子条件都不满足");
            return false;
        } else {
            log.warn("未知的逻辑关系: {}", logic);
            return false;
        }
    }
    
    /**
     * 评估子条件节点
     * @param nodes 所有节点
     * @param edges 所有连线
     * @param conditionNodeId 条件节点ID
     * @param data 设备数据
     * @return 条件是否满足
     */
    private boolean evaluateChildCondition(JSONArray nodes, JSONArray edges, String conditionNodeId, JSONObject data) {
        // 查找条件节点
        JSONObject conditionNode = null;
        for (int i = 0; i < nodes.size(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            if (conditionNodeId.equals(node.getStr("id"))) {
                conditionNode = node;
                break;
            }
        }
        
        if (conditionNode == null) {
            log.warn("未找到子条件节点: {}", conditionNodeId);
            return false;
        }
        
        String nodeType = conditionNode.getStr("type");
        if (!"condition".equals(nodeType)) {
            log.warn("节点不是条件节点: {}", nodeType);
            return false;
        }
        
        JSONObject properties = conditionNode.getJSONObject("properties");
        if (properties == null) {
            log.warn("子条件节点属性为null");
            return false;
        }
        
        // 检查是否是嵌套的条件组
        String conditionType = properties.getStr("conditionType");
        if ("group".equals(conditionType)) {
            // 递归处理嵌套条件组
            String logic = properties.getStr("logic");
            return evaluateConditionGroup(nodes, edges, conditionNodeId, data, logic);
        } else {
            // 简单条件：直接评估
            return evaluateCondition(properties, data);
        }
    }

    /**
     * 评估条件是否满足
     */
    private boolean evaluateCondition(JSONObject condition, JSONObject data) {
        String property = condition.getStr("property");
        String operator = condition.getStr("operator");
        Object expectedValue = condition.get("value");

        Object actualValue = data.get(property);
        
        log.info("评估规则条件 - property: {}, operator: {}, expected: {} ({}), actual: {} ({})", 
                property, operator, expectedValue, expectedValue != null ? expectedValue.getClass().getSimpleName() : "null",
                actualValue, actualValue != null ? actualValue.getClass().getSimpleName() : "null");
        
        if (actualValue == null) {
            log.warn("属性值为null - property: {}, data: {}", property, data);
            return false;
        }

        boolean result = false;
        
        // 特殊处理：布尔类型直接比较
        if (actualValue instanceof Boolean || expectedValue instanceof Boolean) {
            boolean actualBool = Convert.toBool(actualValue, false);
            boolean expectedBool = Convert.toBool(expectedValue, false);
            
            log.info("布尔类型比较 - actualValue原值: {}, expectedValue原值: {}", actualValue, expectedValue);
            log.info("布尔类型比较 - actualBool转换后: {}, expectedBool转换后: {}, operator: {}", actualBool, expectedBool, operator);
            
            switch (operator) {
                case "==":
                    result = actualBool == expectedBool;
                    log.info("布尔相等比较 - {} == {} = {}", actualBool, expectedBool, result);
                    break;
                case "!=":
                    result = actualBool != expectedBool;
                    log.info("布尔不等比较 - {} != {} = {}", actualBool, expectedBool, result);
                    break;
                default:
                    log.warn("布尔类型不支持操作符: {}", operator);
                    return false;
            }
        } else {
            // 数值类型或字符串类型比较
            switch (operator) {
                case ">":
                    result = compare(actualValue, expectedValue) > 0;
                    break;
                case ">=":
                    result = compare(actualValue, expectedValue) >= 0;
                    break;
                case "<":
                    result = compare(actualValue, expectedValue) < 0;
                    break;
                case "<=":
                    result = compare(actualValue, expectedValue) <= 0;
                    break;
                case "==":
                    result = compare(actualValue, expectedValue) == 0;
                    break;
                case "!=":
                    result = compare(actualValue, expectedValue) != 0;
                    break;
                default:
                    log.warn("未知的操作符: {}", operator);
                    return false;
            }
        }
        
        log.info("条件评估结果: {}", result);
        return result;
    }

    /**
     * 比较两个值
     */
    private int compare(Object actual, Object expected) {
        try {
            BigDecimal actualNum = Convert.toBigDecimal(actual);
            BigDecimal expectedNum = Convert.toBigDecimal(expected);
            return actualNum.compareTo(expectedNum);
        } catch (Exception e) {
            return actual.toString().compareTo(expected.toString());
        }
    }

    /**
     * 执行工作流
     */
    private void executeWorkflow(JSONObject workflow, JSONObject triggerData) {
        try {
            JSONArray nodes = workflow.getJSONArray("nodes");
            if (nodes == null || nodes.isEmpty()) {
                return;
            }
            
            // 查找并执行所有 action 节点
            for (int i = 0; i < nodes.size(); i++) {
                JSONObject node = nodes.getJSONObject(i);
                if (!"action".equals(node.getStr("type"))) {
                    continue;
                }
                
                JSONObject properties = node.getJSONObject("properties");
                if (properties == null) {
                    continue;
                }
                
                String actionType = properties.getStr("actionType");
                if (StrUtil.isBlank(actionType)) {
                    continue;
                }
                
                // 执行动作
                switch (actionType) {
                    case "deviceCommand":
                        executeDeviceCommandFromWorkflow(properties);
                        break;
                    case "notification":
                        executeNotificationFromWorkflow(properties);
                        break;
                    case "webhook":
                        executeWebhookFromWorkflow(properties);
                        break;
                    default:
                        log.warn("未知动作类型: {}", actionType);
                }
            }
            
        } catch (Exception e) {
            log.error("执行工作流异常", e);
            throw e;
        }
    }
    
    /**
     * 从工作流节点执行设备指令
     */
    private void executeDeviceCommandFromWorkflow(JSONObject properties) {
        try {
            String targetDeviceId = properties.getStr("targetDeviceId");
            String command = properties.getStr("command");
            String paramsStr = properties.getStr("params");
            
            if (StrUtil.isBlank(paramsStr)) {
                log.warn("参数为空 - Command: {}", command);
                return;
            }
            
            JSONObject params = JSONUtil.parseObj(paramsStr);
            
            // 获取目标设备
            IotDevice device = iotDeviceService.getById(targetDeviceId);
            if (ObjectUtil.isNull(device)) {
                log.warn("目标设备不存在 - DeviceId: {}", targetDeviceId);
                return;
            }
            
            // 查询设备驱动关联（获取设备配置）
            LambdaQueryWrapper<IotDeviceDriverRel> relQuery = new LambdaQueryWrapper<>();
            relQuery.eq(IotDeviceDriverRel::getDeviceId, targetDeviceId);
            IotDeviceDriverRel driverRel = iotDeviceDriverRelService.getOne(relQuery);

            // 根据设备类型选择不同的执行方式
            if (ObjectUtil.isNotEmpty(DriverConfigUtil.getIpAddress(driverRel, device))) {
                // Modbus设备
                executeModbusCommand(device, driverRel, command, params);
            } else {
                // MQTT/其他协议设备
                executeMqttCommand(device, command, params);
            }
            
            log.info("执行设备指令成功 - DeviceKey: {}, Command: {}", device.getDeviceKey(), command);
        } catch (Exception e) {
            log.error("执行设备指令失败", e);
        }
    }
    
    /**
     * 从工作流节点执行通知
     */
    private void executeNotificationFromWorkflow(JSONObject properties) {
        // TODO: 实现通知功能
        log.info("执行通知动作: {}", properties);
    }
    
    /**
     * 从工作流节点执行Webhook
     */
    private void executeWebhookFromWorkflow(JSONObject properties) {
        // TODO: 实现Webhook功能
        log.info("执行Webhook动作: {}", properties);
    }

    /**
     * 执行规则动作（旧方法，保留兼容）
     * @deprecated 使用 executeWorkflow 替代
     */
    @Deprecated
    private void executeRule(IotRule rule, JSONObject triggerData) {
        log.warn("使用了已弃用的 executeRule 方法，请使用 executeWorkflow");
        // 旧逻辑已移除，请使用工作流方式
    }

    /**
     * 执行设备指令
     */
    private void executeDeviceCommand(JSONObject action) {
        try {
            String targetDeviceId = action.getStr("deviceId");
            String command = action.getStr("command");
            JSONObject params = action.getJSONObject("params");

            // 获取目标设备
            IotDevice device = iotDeviceService.getById(targetDeviceId);
            if (ObjectUtil.isNull(device)) {
                log.warn("目标设备不存在 - DeviceId: {}", targetDeviceId);
                return;
            }
            
            // 查询设备驱动关联（获取设备配置）
            LambdaQueryWrapper<IotDeviceDriverRel> relQuery = new LambdaQueryWrapper<>();
            relQuery.eq(IotDeviceDriverRel::getDeviceId, targetDeviceId);
            IotDeviceDriverRel driverRel = iotDeviceDriverRelService.getOne(relQuery);

            // 根据设备类型选择不同的执行方式
            // 使用驱动关联配置判断IP地址
            if (ObjectUtil.isNotEmpty(DriverConfigUtil.getIpAddress(driverRel, device))) {
                // Modbus设备: 通过Modbus协议执行
                executeModbusCommand(device, driverRel, command, params);
            } else {
                // MQTT/其他协议设备: 通过MQTT执行
                executeMqttCommand(device, command, params);
            }
            
            log.info("执行设备指令成功 - DeviceKey: {}, Command: {}", device.getDeviceKey(), command);
        } catch (Exception e) {
            log.error("执行设备指令失败", e);
        }
    }

    /**
     * 执行Modbus设备指令
     */
    private void executeModbusCommand(IotDevice device, 
                                      IotDeviceDriverRel driverRel,
                                      String command, 
                                      JSONObject params) {
        try {
            log.info("Modbus设备指令执行 - DeviceKey: {}, Command: {}, Params: {}", 
                device.getDeviceKey(), command, params);
            
            // 根据命令类型执行不同操作
            switch (command) {
                case "setOutput":
                    // 设置单个输出
                    // output参数支持两种格式：整数(1-8) 或 identifier字符串("DO1"-"DO8")
                    Object outputValue = params.get("output");
                    String identifier;
                    
                    if (outputValue instanceof Integer) {
                        // 整数格式：转identifier (1 -> "DO1")
                        int num = (Integer) outputValue;
                        identifier = "DO" + num;
                    } else if (outputValue instanceof String) {
                        // 字符串格式：直接使用identifier
                        identifier = (String) outputValue;
                    } else {
                        throw new RuntimeException("output参数格式错误: " + outputValue);
                    }
                    
                    boolean value = params.getBool("value");
                    
                    // 从属性映射中查找对应的寄存器地址
                    Map<String, Map<String, Object>> mappingMap = iotDevicePropertyMappingService.getDevicePropertyMappingMap(device.getId());
                    Map<String, Object> mappingInfo = mappingMap.get(identifier);
                    
                    Integer registerAddress = null;
                    if (mappingInfo != null) {
                        IotDeviceAddressConfig config = (IotDeviceAddressConfig) mappingInfo.get("addressConfig");
                        if (config != null && config.getExtConfig() != null) {
                            try {
                                cn.hutool.json.JSONObject extConfig = cn.hutool.json.JSONUtil.parseObj(config.getExtConfig());
                                registerAddress = extConfig.getInt("registerAddress");
                            } catch (Exception e) {
                                log.warn("解析registerAddress失败: {}", config.getExtConfig());
                            }
                        }
                    }
                    
                    if (registerAddress == null) {
                        throw new RuntimeException("属性 " + identifier + " 未配置寄存器映射");
                    }
                    
                    modbus4jTcpClient.writeSingleRegister(device, registerAddress, value ? 1 : 0);
                    log.info("Modbus设备输出控制 - {} (寄存器地址{}) -> {}", identifier, registerAddress, value ? "打开" : "关闭");
                    break;
                    
                case "setBatchOutputs":
                    // 批量设置输出
                    List<Integer> outputs = (List<Integer>) params.get("outputs");
                    boolean batchValue = params.getBool("value");
                    for (Integer out : outputs) {
                        int addr = out - 1; // PDU地址
                        modbus4jTcpClient.writeSingleRegister(device, addr, batchValue ? 1 : 0);
                    }
                    log.info("Modbus设备批量输出控制 - {} 个输出 -> {}", outputs.size(), batchValue ? "打开" : "关闭");
                    break;
                    
                default:
                    log.warn("未知的Modbus命令: {}", command);
            }
        } catch (Exception e) {
            log.error("Modbus设备指令执行失败 - Command: {}", command, e);
            throw new RuntimeException("指令执行失败: " + e.getMessage());
        }
    }

    /**
     * 执行MQTT设备指令
     */
    private void executeMqttCommand(IotDevice device, String command, JSONObject params) {
        // 构建MQTT消息
        String topic = String.format("/%s/%s/command/down", device.getProductId(), device.getDeviceKey());
        JSONObject message = new JSONObject();
        message.set("command", command);
        message.set("params", params);
        message.set("timestamp", System.currentTimeMillis());

        // 下发指令
        boolean success = deviceMessageService.sendToDevice(device.getDeviceKey(), topic, message.toString());
        if (!success) {
            log.warn("MQTT设备指令下发失败 - DeviceKey: {}", device.getDeviceKey());
        }
    }

    /**
     * 执行通知动作
     */
    private void executeNotification(JSONObject action) {
        try {
            String channel = action.getStr("channel"); // sms/email/dingtalk/alert
            String title = action.getStr("title");
            String content = action.getStr("content");
            String target = action.getStr("target");

            log.info("发送通知 - Channel: {}, Title: {}, Target: {}", channel, title, target);
            
            boolean success = false;
            switch (channel) {
                case "sms":
                    // 发送短信
                    List<String> phones = StrUtil.split(target, ',');
                    Map<String, String> smsParams = new LinkedHashMap<>();
                    smsParams.put("content", content);
                    success = iotNotificationService.sendSms(phones, "IOT_ALERT", smsParams);
                    break;
                    
                case "email":
                    // 发送邮件
                    List<String> emails = StrUtil.split(target, ',');
                    success = iotNotificationService.sendEmail(emails, title, content);
                    break;
                    
                case "dingtalk":
                    // 发送钉钉通知
                    success = iotNotificationService.sendDingTalk(target, title, content, null, false);
                    break;
                    
                case "alert":
                    // 发送告警通知(自动选择渠道)
                    String level = action.getStr("level", "WARNING");
                    success = iotNotificationService.sendAlert(title, content, level);
                    break;
                    
                default:
                    log.warn("未知通知渠道: {}", channel);
            }
            
            if (success) {
                log.info("通知发送成功 - Channel: {}", channel);
            } else {
                log.warn("通知发送失败 - Channel: {}", channel);
            }
            
        } catch (Exception e) {
            log.error("执行通知动作失败", e);
        }
    }

    /**
     * 执行Webhook
     */
    private void executeWebhook(JSONObject action) {
        try {
            String url = action.getStr("url");
            String method = action.getStr("method", "POST");
            JSONObject body = action.getJSONObject("body");

            log.info("执行Webhook - URL: {}, Method: {}", url, method);
            
            String response;
            if ("GET".equalsIgnoreCase(method)) {
                response = cn.hutool.http.HttpUtil.get(url);
            } else {
                response = cn.hutool.http.HttpUtil.post(url, body.toString());
            }
            
            log.info("Webhook执行成功 - Response: {}", response);
            
        } catch (Exception e) {
            log.error("执行Webhook失败", e);
        }
    }

    /**
     * 保存规则执行日志
     */
    private void saveRuleLog(IotRule rule, String triggerData, String result, String errorMsg) {
        try {
            IotRuleLog log = new IotRuleLog();
            log.setRuleId(rule.getId());
            log.setRuleName(rule.getRuleName());
            log.setTriggerData(triggerData);
            log.setExecuteResult(result);
            log.setErrorMsg(errorMsg);
            log.setExecuteTime(DateUtil.date());
            
            iotRuleLogService.save(log);
        } catch (Exception e) {
            log.error("保存规则日志失败 - RuleId: {}", rule.getId(), e);
        }
    }
}
