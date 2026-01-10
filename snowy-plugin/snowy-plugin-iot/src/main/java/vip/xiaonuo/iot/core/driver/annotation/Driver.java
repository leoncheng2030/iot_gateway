/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 */
package vip.xiaonuo.iot.core.driver.annotation;

import java.lang.annotation.*;

/**
 * 驱动注解
 * 用于标记设备驱动类，支持自动注册
 *
 * @author xiaonuo
 * @date 2026/01/10
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Driver {
    
    /**
     * 驱动类型（唯一标识）
     */
    String type();
    
    /**
     * 驱动名称
     */
    String name();
    
    /**
     * 驱动描述
     */
    String description() default "";
}
