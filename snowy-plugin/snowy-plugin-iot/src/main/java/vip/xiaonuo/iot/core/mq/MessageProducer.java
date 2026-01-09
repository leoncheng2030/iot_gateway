/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 */
package vip.xiaonuo.iot.core.mq;

import cn.hutool.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

/**
 * 消息生产者服务
 * 使用内存队列处理消息
 * 
 * @author jetox
 * @date 2025/12/13
 */
@Slf4j
@Service
public class MessageProducer {

	@Resource
	private InMemoryMessageQueue inMemoryMessageQueue;
	
	/**
	 * 发送设备数据到消息队列
	 * 
	 * @param deviceKey 设备标识
	 * @param data 设备数据
	 */
	public void sendDeviceData(String deviceKey, JSONObject data) {
		try {
			inMemoryMessageQueue.sendDeviceData(deviceKey, data);
		} catch (Exception e) {
			log.error("发送设备数据到消息队列失败 - DeviceKey: {}", deviceKey, e);
		}
	}
	
	/**
	 * 发送设备指令到消息队列
	 * 
	 * @param deviceKey 设备标识
	 * @param command 指令内容
	 */
	public void sendDeviceCommand(String deviceKey, JSONObject command) {
		try {
			inMemoryMessageQueue.sendDeviceCommand(deviceKey, command);
		} catch (Exception e) {
			log.error("发送设备指令到消息队列失败 - DeviceKey: {}", deviceKey, e);
		}
	}
	
	/**
	 * 触发规则引擎
	 * 
	 * @param ruleId 规则ID
	 * @param triggerData 触发数据
	 */
	public void triggerRule(String ruleId, JSONObject triggerData) {
		try {
			inMemoryMessageQueue.sendRuleTrigger(ruleId, triggerData);
		} catch (Exception e) {
			log.error("发送规则触发消息失败 - RuleId: {}", ruleId, e);
		}
	}
	
	/**
	 * 发送告警消息（高优先级）
	 * 
	 * @param alarmType 告警类型
	 * @param alarmData 告警数据
	 * @param priority 优先级 0-10
	 */
	public void sendAlarm(String alarmType, JSONObject alarmData, int priority) {
		try {
			inMemoryMessageQueue.sendAlarm(alarmType, alarmData, priority);
		} catch (Exception e) {
			log.error("发送告警消息失败 - 类型: {}", alarmType, e);
		}
	}
	
	/**
	 * 发送通知消息
	 * 
	 * @param notificationType 通知类型 (sms/email/dingtalk)
	 * @param notificationData 通知数据
	 */
	public void sendNotification(String notificationType, JSONObject notificationData) {
		try {
			inMemoryMessageQueue.sendNotification(notificationType, notificationData);
		} catch (Exception e) {
			log.error("发送通知消息失败 - 类型: {}", notificationType, e);
		}
	}
}
