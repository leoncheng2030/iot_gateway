/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 */
package vip.xiaonuo.iot.core.mq;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vip.xiaonuo.iot.core.message.DeviceMessageService;
import vip.xiaonuo.iot.core.message.RuleEngineService;
import vip.xiaonuo.iot.core.storage.InfluxDBService;
import vip.xiaonuo.iot.core.notification.IotNotificationService;
import vip.xiaonuo.iot.modular.device.entity.IotDevice;
import vip.xiaonuo.iot.modular.device.service.IotDeviceService;

/**
 * 内存消息队列处理器适配器
 * 将原MessageConsumer的处理逻辑适配到内存队列
 *
 * @author yubaoshan
 * @date 2026/01/09
 */
@Slf4j
@Component
public class InMemoryMessageHandlers {

	@Resource
	private InMemoryMessageQueue inMemoryMessageQueue;

	@Resource
	private InfluxDBService influxDBService;

	@Resource
	private IotDeviceService iotDeviceService;

	@Resource
	private DeviceMessageService deviceMessageService;

	@Resource
	private RuleEngineService ruleEngineService;

	@Resource
	private IotNotificationService iotNotificationService;

	@Resource
	private MessageProducer messageProducer;

	@PostConstruct
	public void init() {
		// 注册所有消息处理器
		inMemoryMessageQueue.registerDeviceDataHandler(new DeviceDataHandler());
		inMemoryMessageQueue.registerAlarmHandler(new AlarmHandler());
		inMemoryMessageQueue.registerRuleHandler(new RuleHandler());
		inMemoryMessageQueue.registerNotificationHandler(new NotificationHandler());
		inMemoryMessageQueue.registerCommandHandler(new CommandHandler());

		log.info("内存消息队列处理器已注册");
	}

	/**
	 * 设备数据处理器
	 */
	private class DeviceDataHandler implements InMemoryMessageQueue.DeviceDataMessageHandler {
		@Override
		public void handle(InMemoryMessageQueue.DeviceDataMessage message) {
			log.trace("开始消费设备数据消息: DeviceKey={}", message.getDeviceKey());
			try {
				String deviceKey = message.getDeviceKey();
				JSONObject data = message.getData();

				// 查询设备信息
				LambdaQueryWrapper<IotDevice> queryWrapper = new LambdaQueryWrapper<>();
				queryWrapper.eq(IotDevice::getDeviceKey, deviceKey);
				IotDevice device = iotDeviceService.getOne(queryWrapper);

				if (device != null) {
					// 写入InfluxDB时序数据
					influxDBService.writeDeviceData(device, data);
					log.debug("设备数据写入InfluxDB成功 - DeviceKey: {}", deviceKey);
				} else {
					log.warn("设备不存在 - DeviceKey: {}", deviceKey);
				}

			} catch (Exception e) {
				log.error("处理设备数据消息失败", e);
			}
		}
	}

	/**
	 * 告警消息处理器
	 */
	private class AlarmHandler implements InMemoryMessageQueue.AlarmMessageHandler {
		@Override
		public void handle(InMemoryMessageQueue.AlarmMessage message) {
			log.info("开始消费告警消息: 类型={}, 优先级={}", message.getAlarmType(), message.getPriority());
			try {
				String alarmType = message.getAlarmType();
				JSONObject alarmData = message.getAlarmData();

				// 根据告警类型处理
				switch (alarmType) {
					case "DEVICE_OFFLINE":
						String deviceName = alarmData.getStr("deviceName");
						String ipAddress = alarmData.getStr("ipAddress");
						log.warn("设备离线告警 - 设备: {}, IP: {}", deviceName, ipAddress);

						// 发送通知给管理员
						JSONObject notificationData = new JSONObject();
						notificationData.set("subject", "设备离线告警");
						notificationData.set("content", String.format("设备[%s]已离线，IP地址：%s", deviceName, ipAddress));
						messageProducer.sendNotification("email", notificationData);
						break;

					case "alarm":
						log.warn("设备告警事件 - 数据: {}", alarmData);
						// 记录告警日志、发送通知
						break;

					default:
						log.warn("未知告警类型: {}", alarmType);
				}

				log.warn("告警消息处理完成 - 类型: {}", alarmType);

			} catch (Exception e) {
				log.error("处理告警消息失败", e);
			}
		}
	}

	/**
	 * 规则触发处理器
	 */
	private class RuleHandler implements InMemoryMessageQueue.RuleTriggerMessageHandler {
		@Override
		public void handle(InMemoryMessageQueue.RuleTriggerMessage message) {
			log.trace("开始消费规则触发消息: RuleId={}", message.getRuleId());
			try {
				String ruleId = message.getRuleId();
				JSONObject triggerData = message.getTriggerData();
			
				// 集成规则引擎(如果存在)
				if (ruleEngineService != null) {
					ruleEngineService.triggerByDeviceData(ruleId, triggerData);
					log.info("规则触发消息处理完成 - RuleId: {}", ruleId);
				} else {
					log.debug("规则引擎服务未启用,跳过规则触发");
				}

			} catch (Exception e) {
				log.error("处理规则触发消息失败", e);
			}
		}
	}

	/**
	 * 通知消息处理器
	 */
	private class NotificationHandler implements InMemoryMessageQueue.NotificationMessageHandler {
		@Override
		public void handle(InMemoryMessageQueue.NotificationMessage message) {
			log.trace("开始消费通知消息: 类型={}", message.getType());
			try {
				String type = message.getType();
				JSONObject data = message.getData();
			
				// 检查通知服务是否可用
				if (iotNotificationService == null) {
					log.warn("通知服务未配置,无法发送通知 - 类型: {}", type);
					return;
				}
			
				// 根据类型发送不同通知
				switch (type) {
					case "sms":
						// 发送短信通知
						iotNotificationService.sendSms(
							java.util.Arrays.asList(data.getStr("phone")),
							data.getStr("templateCode"),
							data.getJSONObject("params").toBean(java.util.HashMap.class)
						);
						break;
					case "email":
						// 发送邮件通知
						iotNotificationService.sendEmail(
							java.util.Arrays.asList(data.getStr("email")),
							data.getStr("subject"),
							data.getStr("content")
						);
						break;
					case "dingtalk":
						// 发送钉钉通知
						iotNotificationService.sendDingTalk(
							data.getStr("webhook"),
							data.getStr("title"),
							data.getStr("content"),
							data.getJSONArray("atMobiles").toList(String.class),
							data.getBool("isAtAll", false)
						);
						break;
					default:
						log.warn("未知的通知类型: {}", type);
				}

				log.debug("通知消息处理完成 - 类型: {}", type);

			} catch (Exception e) {
				log.error("处理通知消息失败", e);
			}
		}
	}

	/**
	 * 设备指令处理器
	 */
	private class CommandHandler implements InMemoryMessageQueue.DeviceCommandMessageHandler {
		@Override
		public void handle(InMemoryMessageQueue.DeviceCommandMessage message) {
			log.trace("开始消费设备指令消息: DeviceKey={}", message.getDeviceKey());
			try {
				String deviceKey = message.getDeviceKey();
				JSONObject command = message.getCommand();
			
				// 检查消息服务是否可用
				if (deviceMessageService == null) {
					log.warn("设备消息服务未配置,无法下发指令 - DeviceKey: {}", deviceKey);
					return;
				}
			
				// 实现设备指令下发逻辑
				deviceMessageService.sendToDevice(deviceKey, "command", command.toString());

				log.info("设备指令消息处理完成 - DeviceKey: {}", deviceKey);

			} catch (Exception e) {
				log.error("处理设备指令消息失败", e);
			}
		}
	}
}
