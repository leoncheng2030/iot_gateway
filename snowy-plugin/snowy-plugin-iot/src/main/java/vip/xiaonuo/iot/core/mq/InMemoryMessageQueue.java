/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 */
package vip.xiaonuo.iot.core.mq;

import cn.hutool.json.JSONObject;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 内存消息队列管理器
 * 轻量级消息队列实现,替代RabbitMQ,适用于边缘网关场景
 * 
 * 功能:
 * 1. 设备数据异步处理队列
 * 2. 告警消息队列(支持优先级)
 * 3. 规则触发队列
 * 4. 通知消息队列
 * 5. 设备指令队列
 *
 * @author yubaoshan
 * @date 2026/01/09
 */
@Slf4j
@Component
public class InMemoryMessageQueue {

	/** 设备数据队列容量 */
	@Value("${iot.mq.queue.device-data-size:10000}")
	private int deviceDataQueueSize;

	/** 告警队列容量 */
	@Value("${iot.mq.queue.alarm-size:5000}")
	private int alarmQueueSize;

	/** 规则触发队列容量 */
	@Value("${iot.mq.queue.rule-size:5000}")
	private int ruleQueueSize;

	/** 通知队列容量 */
	@Value("${iot.mq.queue.notification-size:3000}")
	private int notificationQueueSize;

	/** 指令队列容量 */
	@Value("${iot.mq.queue.command-size:3000}")
	private int commandQueueSize;

	/** 消费线程数 */
	@Value("${iot.mq.consumer-threads:4}")
	private int consumerThreads;

	// ==================== 消息队列 ====================

	/** 设备数据队列 */
	private BlockingQueue<DeviceDataMessage> deviceDataQueue;

	/** 告警队列(支持优先级) */
	private BlockingQueue<AlarmMessage> alarmQueue;

	/** 规则触发队列 */
	private BlockingQueue<RuleTriggerMessage> ruleQueue;

	/** 通知队列 */
	private BlockingQueue<NotificationMessage> notificationQueue;

	/** 设备指令队列 */
	private BlockingQueue<DeviceCommandMessage> commandQueue;

	// ==================== 线程池 ====================

	/** 设备数据消费线程池 */
	private ExecutorService deviceDataExecutor;

	/** 告警消费线程池(高优先级) */
	private ExecutorService alarmExecutor;

	/** 规则触发消费线程池 */
	private ExecutorService ruleExecutor;

	/** 通知消费线程池 */
	private ExecutorService notificationExecutor;

	/** 指令消费线程池 */
	private ExecutorService commandExecutor;

	// ==================== 消息处理器(由外部注入) ====================

	private DeviceDataMessageHandler deviceDataHandler;
	private AlarmMessageHandler alarmHandler;
	private RuleTriggerMessageHandler ruleHandler;
	private NotificationMessageHandler notificationHandler;
	private DeviceCommandMessageHandler commandHandler;

	@PostConstruct
	public void init() {
		log.info("初始化内存消息队列...");

		// 初始化队列
		deviceDataQueue = new LinkedBlockingQueue<>(deviceDataQueueSize);
		alarmQueue = new PriorityBlockingQueue<>(alarmQueueSize);  // 支持优先级
		ruleQueue = new LinkedBlockingQueue<>(ruleQueueSize);
		notificationQueue = new LinkedBlockingQueue<>(notificationQueueSize);
		commandQueue = new LinkedBlockingQueue<>(commandQueueSize);

		// 初始化消费线程池
		initExecutors();

		// 启动消费者
		startConsumers();

		log.info("内存消息队列启动完成 - 消费线程: {}", consumerThreads);
		log.info("队列容量 - 设备数据: {}, 告警: {}, 规则: {}, 通知: {}, 指令: {}", 
			deviceDataQueueSize, alarmQueueSize, ruleQueueSize, notificationQueueSize, commandQueueSize);
	}

	/**
	 * 初始化线程池
	 */
	private void initExecutors() {
		// 设备数据处理线程池
		deviceDataExecutor = createExecutor("device-data-consumer", consumerThreads);

		// 告警处理线程池(高优先级,线程数稍少)
		alarmExecutor = createExecutor("alarm-consumer", Math.max(2, consumerThreads / 2));

		// 规则引擎线程池
		ruleExecutor = createExecutor("rule-consumer", Math.max(2, consumerThreads / 2));

		// 通知线程池
		notificationExecutor = createExecutor("notification-consumer", 2);

		// 指令下发线程池
		commandExecutor = createExecutor("command-consumer", 2);
	}

	/**
	 * 创建线程池
	 */
	private ExecutorService createExecutor(String namePrefix, int threads) {
		return new ThreadPoolExecutor(
			threads,
			threads * 2,
			60L,
			TimeUnit.SECONDS,
			new LinkedBlockingQueue<>(1000),
			new ThreadFactory() {
				private final AtomicInteger counter = new AtomicInteger(1);
				@Override
				public Thread newThread(Runnable r) {
					Thread thread = new Thread(r, namePrefix + "-" + counter.getAndIncrement());
					thread.setDaemon(true);
					return thread;
				}
			},
			new ThreadPoolExecutor.CallerRunsPolicy()
		);
	}

	/**
	 * 启动所有消费者
	 */
	private void startConsumers() {
		// 启动设备数据消费者
		for (int i = 0; i < consumerThreads; i++) {
			deviceDataExecutor.submit(this::consumeDeviceData);
		}

		// 启动告警消费者
		for (int i = 0; i < Math.max(2, consumerThreads / 2); i++) {
			alarmExecutor.submit(this::consumeAlarm);
		}

		// 启动规则消费者
		for (int i = 0; i < Math.max(2, consumerThreads / 2); i++) {
			ruleExecutor.submit(this::consumeRule);
		}

		// 启动通知消费者
		notificationExecutor.submit(this::consumeNotification);
		notificationExecutor.submit(this::consumeNotification);

		// 启动指令消费者
		commandExecutor.submit(this::consumeCommand);
		commandExecutor.submit(this::consumeCommand);
	}

	// ==================== 消息生产方法 ====================

	/**
	 * 发送设备数据消息
	 */
	public boolean sendDeviceData(String deviceKey, JSONObject data) {
		DeviceDataMessage message = new DeviceDataMessage();
		message.setDeviceKey(deviceKey);
		message.setData(data);
		message.setTimestamp(System.currentTimeMillis());

		boolean success = deviceDataQueue.offer(message);
		if (!success) {
			log.warn("设备数据队列已满,消息丢失 - DeviceKey: {}", deviceKey);
		}
		return success;
	}

	/**
	 * 发送告警消息
	 */
	public boolean sendAlarm(String alarmType, JSONObject alarmData, int priority) {
		AlarmMessage message = new AlarmMessage();
		message.setAlarmType(alarmType);
		message.setAlarmData(alarmData);
		message.setPriority(priority);
		message.setTimestamp(System.currentTimeMillis());

		boolean success = alarmQueue.offer(message);
		if (!success) {
			log.warn("告警队列已满,消息丢失 - 类型: {}", alarmType);
		}
		return success;
	}

	/**
	 * 发送规则触发消息
	 */
	public boolean sendRuleTrigger(String ruleId, JSONObject triggerData) {
		RuleTriggerMessage message = new RuleTriggerMessage();
		message.setRuleId(ruleId);
		message.setTriggerData(triggerData);
		message.setTimestamp(System.currentTimeMillis());

		boolean success = ruleQueue.offer(message);
		if (!success) {
			log.warn("规则触发队列已满,消息丢失 - RuleId: {}", ruleId);
		}
		return success;
	}

	/**
	 * 发送通知消息
	 */
	public boolean sendNotification(String type, JSONObject data) {
		NotificationMessage message = new NotificationMessage();
		message.setType(type);
		message.setData(data);
		message.setTimestamp(System.currentTimeMillis());

		boolean success = notificationQueue.offer(message);
		if (!success) {
			log.warn("通知队列已满,消息丢失 - 类型: {}", type);
		}
		return success;
	}

	/**
	 * 发送设备指令消息
	 */
	public boolean sendDeviceCommand(String deviceKey, JSONObject command) {
		DeviceCommandMessage message = new DeviceCommandMessage();
		message.setDeviceKey(deviceKey);
		message.setCommand(command);
		message.setTimestamp(System.currentTimeMillis());

		boolean success = commandQueue.offer(message);
		if (!success) {
			log.warn("指令队列已满,消息丢失 - DeviceKey: {}", deviceKey);
		}
		return success;
	}

	// ==================== 消息消费方法 ====================

	/**
	 * 消费设备数据
	 */
	private void consumeDeviceData() {
		while (!Thread.currentThread().isInterrupted()) {
			try {
				DeviceDataMessage message = deviceDataQueue.poll(1, TimeUnit.SECONDS);
				if (message != null && deviceDataHandler != null) {
					deviceDataHandler.handle(message);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			} catch (Exception e) {
				log.error("消费设备数据消息失败", e);
			}
		}
	}

	/**
	 * 消费告警消息
	 */
	private void consumeAlarm() {
		while (!Thread.currentThread().isInterrupted()) {
			try {
				AlarmMessage message = alarmQueue.poll(1, TimeUnit.SECONDS);
				if (message != null && alarmHandler != null) {
					alarmHandler.handle(message);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			} catch (Exception e) {
				log.error("消费告警消息失败", e);
			}
		}
	}

	/**
	 * 消费规则触发消息
	 */
	private void consumeRule() {
		while (!Thread.currentThread().isInterrupted()) {
			try {
				RuleTriggerMessage message = ruleQueue.poll(1, TimeUnit.SECONDS);
				if (message != null && ruleHandler != null) {
					ruleHandler.handle(message);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			} catch (Exception e) {
				log.error("消费规则触发消息失败", e);
			}
		}
	}

	/**
	 * 消费通知消息
	 */
	private void consumeNotification() {
		while (!Thread.currentThread().isInterrupted()) {
			try {
				NotificationMessage message = notificationQueue.poll(1, TimeUnit.SECONDS);
				if (message != null && notificationHandler != null) {
					notificationHandler.handle(message);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			} catch (Exception e) {
				log.error("消费通知消息失败", e);
			}
		}
	}

	/**
	 * 消费指令消息
	 */
	private void consumeCommand() {
		while (!Thread.currentThread().isInterrupted()) {
			try {
				DeviceCommandMessage message = commandQueue.poll(1, TimeUnit.SECONDS);
				if (message != null && commandHandler != null) {
					commandHandler.handle(message);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			} catch (Exception e) {
				log.error("消费指令消息失败", e);
			}
		}
	}

	// ==================== 注册消息处理器 ====================

	public void registerDeviceDataHandler(DeviceDataMessageHandler handler) {
		this.deviceDataHandler = handler;
		log.info("设备数据消息处理器已注册");
	}

	public void registerAlarmHandler(AlarmMessageHandler handler) {
		this.alarmHandler = handler;
		log.info("告警消息处理器已注册");
	}

	public void registerRuleHandler(RuleTriggerMessageHandler handler) {
		this.ruleHandler = handler;
		log.info("规则触发消息处理器已注册");
	}

	public void registerNotificationHandler(NotificationMessageHandler handler) {
		this.notificationHandler = handler;
		log.info("通知消息处理器已注册");
	}

	public void registerCommandHandler(DeviceCommandMessageHandler handler) {
		this.commandHandler = handler;
		log.info("指令消息处理器已注册");
	}

	// ==================== 监控方法 ====================

	/**
	 * 获取队列状态
	 */
	public QueueStatus getQueueStatus() {
		QueueStatus status = new QueueStatus();
		status.setDeviceDataSize(deviceDataQueue.size());
			status.setAlarmSize(alarmQueue.size());
			status.setRuleSize(ruleQueue.size());
			status.setNotificationSize(notificationQueue.size());
			status.setCommandSize(commandQueue.size());
		return status;
	}

	/**
	 * 清空所有队列
	 */
	public void clearAll() {
		deviceDataQueue.clear();
			alarmQueue.clear();
			ruleQueue.clear();
			notificationQueue.clear();
			commandQueue.clear();
		log.warn("所有消息队列已清空");
	}

	@PreDestroy
	public void destroy() {
		log.info("关闭内存消息队列...");

		// 关闭所有线程池
		shutdownExecutor(deviceDataExecutor, "设备数据消费线程池");
		shutdownExecutor(alarmExecutor, "告警消费线程池");
		shutdownExecutor(ruleExecutor, "规则消费线程池");
		shutdownExecutor(notificationExecutor, "通知消费线程池");
		shutdownExecutor(commandExecutor, "指令消费线程池");

		log.info("内存消息队列已关闭");
	}

	/**
	 * 优雅关闭线程池
	 */
	private void shutdownExecutor(ExecutorService executor, String name) {
		if (executor != null) {
			executor.shutdown();
			try {
				if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
					executor.shutdownNow();
					log.warn("{} 强制关闭", name);
				} else {
					log.info("{} 已关闭", name);
				}
			} catch (InterruptedException e) {
				executor.shutdownNow();
				Thread.currentThread().interrupt();
			}
		}
	}

	// ==================== 内部类:消息对象 ====================

	public static class DeviceDataMessage {
		private String deviceKey;
		private JSONObject data;
		private Long timestamp;

		public String getDeviceKey() { return deviceKey; }
		public void setDeviceKey(String deviceKey) { this.deviceKey = deviceKey; }
		public JSONObject getData() { return data; }
		public void setData(JSONObject data) { this.data = data; }
		public Long getTimestamp() { return timestamp; }
		public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
	}

	public static class AlarmMessage implements Comparable<AlarmMessage> {
		private String alarmType;
		private JSONObject alarmData;
		private Integer priority;  // 0-10, 数字越大优先级越高
		private Long timestamp;

		@Override
		public int compareTo(AlarmMessage o) {
			// 优先级高的排在前面
			return o.priority.compareTo(this.priority);
		}

		public String getAlarmType() { return alarmType; }
		public void setAlarmType(String alarmType) { this.alarmType = alarmType; }
		public JSONObject getAlarmData() { return alarmData; }
		public void setAlarmData(JSONObject alarmData) { this.alarmData = alarmData; }
		public Integer getPriority() { return priority; }
		public void setPriority(Integer priority) { this.priority = priority; }
		public Long getTimestamp() { return timestamp; }
		public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
	}

	public static class RuleTriggerMessage {
		private String ruleId;
		private JSONObject triggerData;
		private Long timestamp;

		public String getRuleId() { return ruleId; }
		public void setRuleId(String ruleId) { this.ruleId = ruleId; }
		public JSONObject getTriggerData() { return triggerData; }
		public void setTriggerData(JSONObject triggerData) { this.triggerData = triggerData; }
		public Long getTimestamp() { return timestamp; }
		public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
	}

	public static class NotificationMessage {
		private String type;  // sms/email/dingtalk
		private JSONObject data;
		private Long timestamp;

		public String getType() { return type; }
		public void setType(String type) { this.type = type; }
		public JSONObject getData() { return data; }
		public void setData(JSONObject data) { this.data = data; }
		public Long getTimestamp() { return timestamp; }
		public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
	}

	public static class DeviceCommandMessage {
		private String deviceKey;
		private JSONObject command;
		private Long timestamp;

		public String getDeviceKey() { return deviceKey; }
		public void setDeviceKey(String deviceKey) { this.deviceKey = deviceKey; }
		public JSONObject getCommand() { return command; }
		public void setCommand(JSONObject command) { this.command = command; }
		public Long getTimestamp() { return timestamp; }
		public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
	}

	public static class QueueStatus {
		private int deviceDataSize;
		private int alarmSize;
		private int ruleSize;
		private int notificationSize;
		private int commandSize;

		public int getDeviceDataSize() { return deviceDataSize; }
		public void setDeviceDataSize(int deviceDataSize) { this.deviceDataSize = deviceDataSize; }
		public int getAlarmSize() { return alarmSize; }
		public void setAlarmSize(int alarmSize) { this.alarmSize = alarmSize; }
		public int getRuleSize() { return ruleSize; }
		public void setRuleSize(int ruleSize) { this.ruleSize = ruleSize; }
		public int getNotificationSize() { return notificationSize; }
		public void setNotificationSize(int notificationSize) { this.notificationSize = notificationSize; }
		public int getCommandSize() { return commandSize; }
		public void setCommandSize(int commandSize) { this.commandSize = commandSize; }
	}

	// ==================== 消息处理器接口 ====================

	public interface DeviceDataMessageHandler {
		void handle(DeviceDataMessage message);
	}

	public interface AlarmMessageHandler {
		void handle(AlarmMessage message);
	}

	public interface RuleTriggerMessageHandler {
		void handle(RuleTriggerMessage message);
	}

	public interface NotificationMessageHandler {
		void handle(NotificationMessage message);
	}

	public interface DeviceCommandMessageHandler {
		void handle(DeviceCommandMessage message);
	}
}
