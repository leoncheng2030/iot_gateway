/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 */
package vip.xiaonuo.iot.core.protocol.annotation;

import java.lang.annotation.*;

/**
 * 协议注解
 * 用于标记协议服务类，支持自动注册
 *
 * @author xiaonuo
 * @date 2026/01/10
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Protocol {
    
    /**
     * 协议类型（唯一标识）
     */
    String type();
    
    /**
     * 协议名称
     */
    String name();
    
    /**
     * 协议描述
     */
    String description() default "";
}
