package com.cloud.backend.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 用户事件监听器 —— 订阅用户领域事件，处理注册后的旁路逻辑（当前仅记录日志）。
 *
 * 设计思路：
 * 1. 通过 Spring 事件机制解耦：注册主流程不直接依赖日志/通知逻辑
 * 2. 事件发布与监听同线程执行，失败不影响注册主流程的结果返回
 */
@Component
public class UserEventListener {

    private static final Logger log = LoggerFactory.getLogger(UserEventListener.class);

    /**
     * 处理用户注册事件，记录注册成功日志（含 ID、用户名、邮箱）。
     */
    @EventListener
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("User registered: id={}, username={}, email={}",
                event.getUserId(), event.getUsername(), event.getEmail());
    }
}
