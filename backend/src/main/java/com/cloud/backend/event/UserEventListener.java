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
 *
 * 修改指引：
 * - 【习惯】新增监听方法            → 新增 @EventListener 方法（参数为对应事件类型）；方法内抛异常会影响主流程，建议自行兜底
 * - 【习惯】订阅新事件类型          → 方法参数改为新事件类型并加 @EventListener；同类型事件需加 SpEL 条件区分时用 @EventListener 的 condition 属性
 * - 【习惯】修改监听执行方式        → 默认同步同线程执行；如需异步改用 @Async 并开启 @EnableAsync，注意事务边界与结果可见性
 * - 【习惯】调整日志内容            → handleUserRegistered 中的 log.info；改动只影响注册成功日志输出
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
