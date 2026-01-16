package vip.xiaonuo.iot.core.mq;

import cn.hutool.json.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.RollCycles;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import java.io.File;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Chronicle Queue 消息队列（带降级机制）
 * 
 * 特点：
 * - 高性能：Chronicle Queue 100万+ msg/s
 * - 持久化：支持重启恢复
 * - 降级：JVM参数缺失时自动回退到内存队列
 * 
 * JVM参数要求（Java 11+）：
 * --add-exports=java.base/jdk.internal.ref=ALL-UNNAMED
 * --add-exports=java.base/sun.nio.ch=ALL-UNNAMED
 * --add-exports=jdk.unsupported/sun.misc=ALL-UNNAMED
 * --add-opens=java.base/java.lang=ALL-UNNAMED
 * --add-opens=java.base/java.lang.reflect=ALL-UNNAMED
 * --add-opens=java.base/java.io=ALL-UNNAMED
 * --add-opens=java.base/java.util=ALL-UNNAMED
 * 
 * @author wqs
 * @date 2026/01/15
 */
@Slf4j
@Component
public class ChronicleMessageQueue {
    
    @Value("${iot.mq.data-dir:data/queue}")
    private String queueDataDir;
    
    @Value("${iot.mq.consumer-threads:4}")
    private int consumerThreads;
    
    @Value("${iot.mq.queue-capacity:100000}")
    private int queueCapacity;
    
    @Resource
    private ApplicationEventPublisher eventPublisher;
    
    /** 是否使用Chronicle Queue（true）或降级模式（false） */
    private volatile boolean useChronicle = false;
    
    /** Chronicle Queue - 设备消息队列 */
    private ChronicleQueue deviceChronicleQueue;
    private ChronicleQueue ruleChronicleQueue;
    private ChronicleQueue alertChronicleQueue;
    
    /** 降级模式 - 内存队列 */
    private BlockingQueue<DeviceMessage> deviceMemoryQueue;
    private BlockingQueue<RuleMessage> ruleMemoryQueue;
    private BlockingQueue<AlertMessage> alertMemoryQueue;
    
    /** 消费者线程池 */
    private ExecutorService consumerExecutor;
    
    /** 运行状态 */
    private volatile boolean running = false;
    
    /** 统计计数器 */
    private final AtomicLong deviceMessageCount = new AtomicLong(0);
    private final AtomicLong ruleMessageCount = new AtomicLong(0);
    private final AtomicLong alertMessageCount = new AtomicLong(0);
    
    /** 消息处理器 */
    private Consumer<DeviceMessage> deviceMessageHandler;
    private Consumer<RuleMessage> ruleMessageHandler;
    private Consumer<AlertMessage> alertMessageHandler;
    
    @PostConstruct
    public void init() {
        log.info("初始化消息队列服务...");
        
        // 确保目录存在
        new File(queueDataDir).mkdirs();
        
        // 尝试初始化 Chronicle Queue
        try {
            deviceChronicleQueue = SingleChronicleQueueBuilder
                .binary(new File(queueDataDir, "device"))
                .rollCycle(RollCycles.DAILY)
                .build();
            
            ruleChronicleQueue = SingleChronicleQueueBuilder
                .binary(new File(queueDataDir, "rule"))
                .rollCycle(RollCycles.DAILY)
                .build();
            
            alertChronicleQueue = SingleChronicleQueueBuilder
                .binary(new File(queueDataDir, "alert"))
                .rollCycle(RollCycles.DAILY)
                .build();
            
            useChronicle = true;
            log.info("Chronicle Queue 初始化成功，使用高性能持久化队列");
            
        } catch (Throwable e) {
            log.warn("Chronicle Queue 初始化失败，降级到内存队列模式: {}", e.getMessage());
            log.warn("如需启用Chronicle Queue，请添加JVM参数：--add-opens=java.base/java.lang.reflect=ALL-UNNAMED 等");
            
            // 清理可能部分初始化的资源
            closeChronicleQueues();
            
            // 初始化内存队列
            deviceMemoryQueue = new LinkedBlockingQueue<>(queueCapacity);
            ruleMemoryQueue = new LinkedBlockingQueue<>(queueCapacity);
            alertMemoryQueue = new LinkedBlockingQueue<>(queueCapacity);
            
            useChronicle = false;
        }
        
        // 启动消费者线程
        consumerExecutor = Executors.newFixedThreadPool(consumerThreads, r -> {
            Thread thread = new Thread(r, "mq-consumer");
            thread.setDaemon(true);
            return thread;
        });
        
        running = true;
        
        // 启动消费者
        if (useChronicle) {
            consumerExecutor.submit(() -> consumeChronicleMessages(deviceChronicleQueue, "device", this::handleDeviceRaw));
            consumerExecutor.submit(() -> consumeChronicleMessages(ruleChronicleQueue, "rule", this::handleRuleRaw));
            consumerExecutor.submit(() -> consumeChronicleMessages(alertChronicleQueue, "alert", this::handleAlertRaw));
        } else {
            consumerExecutor.submit(this::consumeDeviceMemoryMessages);
            consumerExecutor.submit(this::consumeRuleMemoryMessages);
            consumerExecutor.submit(this::consumeAlertMemoryMessages);
        }
        
        log.info("消息队列初始化完成: mode={}, consumerThreads={}", 
            useChronicle ? "Chronicle" : "Memory", consumerThreads);
    }
    
    /**
     * 发送设备消息
     */
    public boolean sendDeviceMessage(DeviceMessage message) {
        try {
            if (useChronicle) {
                ExcerptAppender appender = deviceChronicleQueue.acquireAppender();
                appender.writeDocument(w -> {
                    w.write("type").text("DEVICE");
                    w.write("messageId").text(message.getMessageId());
                    w.write("deviceId").text(message.getDeviceId());
                    w.write("data").text(message.getData() != null ? message.getData().toString() : "{}");
                    w.write("timestamp").int64(message.getTimestamp());
                });
            } else {
                if (!deviceMemoryQueue.offer(message, 100, TimeUnit.MILLISECONDS)) {
                    log.warn("设备消息队列已满，丢弃消息: deviceId={}", message.getDeviceId());
                    return false;
                }
            }
            deviceMessageCount.incrementAndGet();
            return true;
        } catch (Exception e) {
            log.error("发送设备消息失败", e);
            return false;
        }
    }
    
    /**
     * 发送规则触发消息
     */
    public boolean sendRuleMessage(RuleMessage message) {
        try {
            if (useChronicle) {
                ExcerptAppender appender = ruleChronicleQueue.acquireAppender();
                appender.writeDocument(w -> {
                    w.write("type").text("RULE");
                    w.write("ruleId").text(message.getRuleId());
                    w.write("deviceId").text(message.getDeviceId());
                    w.write("triggerData").text(message.getTriggerData() != null ? message.getTriggerData().toString() : "{}");
                    w.write("timestamp").int64(message.getTimestamp());
                });
            } else {
                if (!ruleMemoryQueue.offer(message, 100, TimeUnit.MILLISECONDS)) {
                    log.warn("规则消息队列已满，丢弃消息: ruleId={}", message.getRuleId());
                    return false;
                }
            }
            ruleMessageCount.incrementAndGet();
            return true;
        } catch (Exception e) {
            log.error("发送规则消息失败", e);
            return false;
        }
    }
    
    /**
     * 发送告警消息
     */
    public boolean sendAlertMessage(AlertMessage message) {
        try {
            if (useChronicle) {
                ExcerptAppender appender = alertChronicleQueue.acquireAppender();
                appender.writeDocument(w -> {
                    w.write("type").text("ALERT");
                    w.write("alertId").text(message.getAlertId());
                    w.write("deviceId").text(message.getDeviceId());
                    w.write("level").text(message.getLevel());
                    w.write("content").text(message.getContent());
                    w.write("timestamp").int64(message.getTimestamp());
                });
            } else {
                if (!alertMemoryQueue.offer(message, 100, TimeUnit.MILLISECONDS)) {
                    log.warn("告警消息队列已满，丢弃消息: alertId={}", message.getAlertId());
                    return false;
                }
            }
            alertMessageCount.incrementAndGet();
            return true;
        } catch (Exception e) {
            log.error("发送告警消息失败", e);
            return false;
        }
    }
    
    /**
     * 消费 Chronicle 消息
     */
    private void consumeChronicleMessages(ChronicleQueue queue, String queueName, Consumer<String> handler) {
        ExcerptTailer tailer = queue.createTailer();
        log.info("启动 Chronicle 消息消费者: queue={}", queueName);
        
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                boolean hasMessage = tailer.readDocument(r -> {
                    StringBuilder sb = new StringBuilder();
                    String type = r.read("type").text();
                    sb.append("type=").append(type).append(";");
                    
                    if ("DEVICE".equals(type)) {
                        sb.append("messageId=").append(r.read("messageId").text()).append(";");
                        sb.append("deviceId=").append(r.read("deviceId").text()).append(";");
                        sb.append("data=").append(r.read("data").text()).append(";");
                        sb.append("timestamp=").append(r.read("timestamp").int64()).append(";");
                    } else if ("RULE".equals(type)) {
                        sb.append("ruleId=").append(r.read("ruleId").text()).append(";");
                        sb.append("deviceId=").append(r.read("deviceId").text()).append(";");
                        sb.append("triggerData=").append(r.read("triggerData").text()).append(";");
                        sb.append("timestamp=").append(r.read("timestamp").int64()).append(";");
                    } else if ("ALERT".equals(type)) {
                        sb.append("alertId=").append(r.read("alertId").text()).append(";");
                        sb.append("deviceId=").append(r.read("deviceId").text()).append(";");
                        sb.append("level=").append(r.read("level").text()).append(";");
                        sb.append("content=").append(r.read("content").text()).append(";");
                        sb.append("timestamp=").append(r.read("timestamp").int64()).append(";");
                    }
                    
                    handler.accept(sb.toString());
                });
                
                if (!hasMessage) {
                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("消费 Chronicle 消息异常: queue={}", queueName, e);
            }
        }
        
        log.info("Chronicle 消息消费者已停止: queue={}", queueName);
    }
    
    /**
     * 消费内存设备消息
     */
    private void consumeDeviceMemoryMessages() {
        log.info("启动内存设备消息消费者");
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                DeviceMessage msg = deviceMemoryQueue.poll(100, TimeUnit.MILLISECONDS);
                if (msg != null && deviceMessageHandler != null) {
                    deviceMessageHandler.accept(msg);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("消费设备消息异常", e);
            }
        }
        log.info("内存设备消息消费者已停止");
    }
    
    /**
     * 消费内存规则消息
     */
    private void consumeRuleMemoryMessages() {
        log.info("启动内存规则消息消费者");
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                RuleMessage msg = ruleMemoryQueue.poll(100, TimeUnit.MILLISECONDS);
                if (msg != null && ruleMessageHandler != null) {
                    ruleMessageHandler.accept(msg);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("消费规则消息异常", e);
            }
        }
        log.info("内存规则消息消费者已停止");
    }
    
    /**
     * 消费内存告警消息
     */
    private void consumeAlertMemoryMessages() {
        log.info("启动内存告警消息消费者");
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                AlertMessage msg = alertMemoryQueue.poll(100, TimeUnit.MILLISECONDS);
                if (msg != null && alertMessageHandler != null) {
                    alertMessageHandler.accept(msg);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("消费告警消息异常", e);
            }
        }
        log.info("内存告警消息消费者已停止");
    }
    
    /**
     * 处理原始设备消息
     */
    private void handleDeviceRaw(String raw) {
        if (deviceMessageHandler != null) {
            DeviceMessage msg = parseDeviceMessage(raw);
            if (msg != null) {
                deviceMessageHandler.accept(msg);
            }
        }
    }
    
    /**
     * 处理原始规则消息
     */
    private void handleRuleRaw(String raw) {
        if (ruleMessageHandler != null) {
            RuleMessage msg = parseRuleMessage(raw);
            if (msg != null) {
                ruleMessageHandler.accept(msg);
            }
        }
    }
    
    /**
     * 处理原始告警消息
     */
    private void handleAlertRaw(String raw) {
        if (alertMessageHandler != null) {
            AlertMessage msg = parseAlertMessage(raw);
            if (msg != null) {
                alertMessageHandler.accept(msg);
            }
        }
    }
    
    /**
     * 解析设备消息
     */
    private DeviceMessage parseDeviceMessage(String raw) {
        try {
            String[] parts = raw.split(";");
            DeviceMessage msg = new DeviceMessage();
            for (String part : parts) {
                String[] kv = part.split("=", 2);
                if (kv.length == 2) {
                    switch (kv[0]) {
                        case "messageId" -> msg.setMessageId(kv[1]);
                        case "deviceId" -> msg.setDeviceId(kv[1]);
                        case "data" -> msg.setData(new JSONObject(kv[1]));
                        case "timestamp" -> msg.setTimestamp(Long.parseLong(kv[1]));
                    }
                }
            }
            return msg;
        } catch (Exception e) {
            log.warn("解析设备消息失败: {}", raw, e);
            return null;
        }
    }
    
    /**
     * 解析规则消息
     */
    private RuleMessage parseRuleMessage(String raw) {
        try {
            String[] parts = raw.split(";");
            RuleMessage msg = new RuleMessage();
            for (String part : parts) {
                String[] kv = part.split("=", 2);
                if (kv.length == 2) {
                    switch (kv[0]) {
                        case "ruleId" -> msg.setRuleId(kv[1]);
                        case "deviceId" -> msg.setDeviceId(kv[1]);
                        case "triggerData" -> msg.setTriggerData(new JSONObject(kv[1]));
                        case "timestamp" -> msg.setTimestamp(Long.parseLong(kv[1]));
                    }
                }
            }
            return msg;
        } catch (Exception e) {
            log.warn("解析规则消息失败: {}", raw, e);
            return null;
        }
    }
    
    /**
     * 解析告警消息
     */
    private AlertMessage parseAlertMessage(String raw) {
        try {
            String[] parts = raw.split(";");
            AlertMessage msg = new AlertMessage();
            for (String part : parts) {
                String[] kv = part.split("=", 2);
                if (kv.length == 2) {
                    switch (kv[0]) {
                        case "alertId" -> msg.setAlertId(kv[1]);
                        case "deviceId" -> msg.setDeviceId(kv[1]);
                        case "level" -> msg.setLevel(kv[1]);
                        case "content" -> msg.setContent(kv[1]);
                        case "timestamp" -> msg.setTimestamp(Long.parseLong(kv[1]));
                    }
                }
            }
            return msg;
        } catch (Exception e) {
            log.warn("解析告警消息失败: {}", raw, e);
            return null;
        }
    }
    
    public void setDeviceMessageHandler(Consumer<DeviceMessage> handler) {
        this.deviceMessageHandler = handler;
    }
    
    public void setRuleMessageHandler(Consumer<RuleMessage> handler) {
        this.ruleMessageHandler = handler;
    }
    
    public void setAlertMessageHandler(Consumer<AlertMessage> handler) {
        this.alertMessageHandler = handler;
    }
    
    /**
     * 是否使用 Chronicle Queue
     */
    public boolean isUseChronicle() {
        return useChronicle;
    }
    
    /**
     * 获取队列统计信息
     */
    public QueueStats getStats() {
        return new QueueStats(
            deviceMessageCount.get(),
            ruleMessageCount.get(),
            alertMessageCount.get(),
            useChronicle ? "Chronicle" : "Memory"
        );
    }
    
    private void closeChronicleQueues() {
        try {
            if (deviceChronicleQueue != null) {
                deviceChronicleQueue.close();
                deviceChronicleQueue = null;
            }
            if (ruleChronicleQueue != null) {
                ruleChronicleQueue.close();
                ruleChronicleQueue = null;
            }
            if (alertChronicleQueue != null) {
                alertChronicleQueue.close();
                alertChronicleQueue = null;
            }
        } catch (Exception ignored) {}
    }
    
    @PreDestroy
    public void shutdown() {
        log.info("关闭消息队列服务...");
        
        running = false;
        
        if (consumerExecutor != null) {
            consumerExecutor.shutdown();
            try {
                if (!consumerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    consumerExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                consumerExecutor.shutdownNow();
            }
        }
        
        closeChronicleQueues();
        
        if (deviceMemoryQueue != null) deviceMemoryQueue.clear();
        if (ruleMemoryQueue != null) ruleMemoryQueue.clear();
        if (alertMemoryQueue != null) alertMemoryQueue.clear();
        
        log.info("消息队列服务已关闭");
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DeviceMessage {
        private String messageId;
        private String deviceId;
        private JSONObject data;
        private long timestamp;
        
        public static DeviceMessage of(String deviceId, JSONObject data) {
            DeviceMessage msg = new DeviceMessage();
            msg.setMessageId(java.util.UUID.randomUUID().toString());
            msg.setDeviceId(deviceId);
            msg.setData(data);
            msg.setTimestamp(System.currentTimeMillis());
            return msg;
        }
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RuleMessage {
        private String ruleId;
        private String deviceId;
        private JSONObject triggerData;
        private long timestamp;
        
        public static RuleMessage of(String ruleId, String deviceId, JSONObject triggerData) {
            RuleMessage msg = new RuleMessage();
            msg.setRuleId(ruleId);
            msg.setDeviceId(deviceId);
            msg.setTriggerData(triggerData);
            msg.setTimestamp(System.currentTimeMillis());
            return msg;
        }
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AlertMessage {
        private String alertId;
        private String deviceId;
        private String level;
        private String content;
        private long timestamp;
        
        public static AlertMessage of(String deviceId, String level, String content) {
            AlertMessage msg = new AlertMessage();
            msg.setAlertId(java.util.UUID.randomUUID().toString());
            msg.setDeviceId(deviceId);
            msg.setLevel(level);
            msg.setContent(content);
            msg.setTimestamp(System.currentTimeMillis());
            return msg;
        }
    }
    
    @Data
    @AllArgsConstructor
    public static class QueueStats {
        private long deviceMessageCount;
        private long ruleMessageCount;
        private long alertMessageCount;
        private String mode;
    }
}
