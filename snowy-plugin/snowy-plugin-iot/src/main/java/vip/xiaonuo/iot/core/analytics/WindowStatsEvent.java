package vip.xiaonuo.iot.core.analytics;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 窗口统计事件
 * 当窗口聚合器计算完成后发布此事件
 * 
 * @author wqs
 * @date 2026/01/15
 */
@Getter
public class WindowStatsEvent extends ApplicationEvent {
    
    /** 设备ID */
    private final String deviceId;
    
    /** 属性键 */
    private final String propertyKey;
    
    /** 窗口统计结果 */
    private final WindowAggregator.WindowStats stats;
    
    /** 事件时间 */
    private final long eventTime;
    
    public WindowStatsEvent(Object source, String deviceId, String propertyKey, 
                           WindowAggregator.WindowStats stats) {
        super(source);
        this.deviceId = deviceId;
        this.propertyKey = propertyKey;
        this.stats = stats;
        this.eventTime = System.currentTimeMillis();
    }
}
