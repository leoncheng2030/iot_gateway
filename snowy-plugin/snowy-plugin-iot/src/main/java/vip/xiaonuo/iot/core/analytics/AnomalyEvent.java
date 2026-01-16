package vip.xiaonuo.iot.core.analytics;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 异常检测事件
 * 当检测到异常数据时发布此事件
 * 
 * @author wqs
 * @date 2026/01/15
 */
@Getter
public class AnomalyEvent extends ApplicationEvent {
    
    /** 异常检测结果 */
    private final AnomalyDetector.AnomalyResult result;
    
    public AnomalyEvent(Object source, AnomalyDetector.AnomalyResult result) {
        super(source);
        this.result = result;
    }
    
    /**
     * 获取设备ID
     */
    public String getDeviceId() {
        return result.getDeviceId();
    }
    
    /**
     * 获取属性键
     */
    public String getPropertyKey() {
        return result.getPropertyKey();
    }
    
    /**
     * 获取当前值
     */
    public double getValue() {
        return result.getValue();
    }
    
    /**
     * 获取异常原因
     */
    public String getReason() {
        return result.getReason();
    }
}
