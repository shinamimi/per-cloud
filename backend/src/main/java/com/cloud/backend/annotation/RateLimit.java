package com.cloud.backend.annotation;

import java.lang.annotation.*;

/**
 * API 速率限制注解
 * 基于 Redis 滑动窗口实现
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /** 限流 key 前缀 */
    String key() default "";

    /** 时间窗口内最大请求数 */
    int limit() default 100;

    /** 时间窗口（秒） */
    int window() default 60;

    /** 限流维度：IP / USER / GLOBAL */
    Dimension dimension() default Dimension.USER;

    enum Dimension {
        IP,     // 按 IP 限流
        USER,   // 按用户 ID 限流
        GLOBAL  // 全局限流
    }
}
