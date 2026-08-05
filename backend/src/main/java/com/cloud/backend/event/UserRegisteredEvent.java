package com.cloud.backend.event;

/**
 * 用户注册事件 —— 用户注册成功后由注册流程发布，携带注册结果快照。
 *
 * 设计思路：
 * 字段不可变（final），事件只承载已发生事实，避免监听器侧修改语义。
 *
 * 修改指引：
 * - 【习惯】新增事件字段            → 添加 final 字段 + 构造器参数 + getter；所有发布点（注册流程）需同步传入，监听器可读取新字段
 * - 【习惯】修改事件携带的数据      → 调整构造器参数；发布方与监听方需同步修改，避免漏传或取不到值
 * - 【习惯】拆分/新增领域事件       → 在 event 包新增事件类，发布处调用 applicationEventPublisher.publishEvent(...)，监听器按需订阅
 */
public class UserRegisteredEvent {

    /** 新注册用户 ID */
    private final Long userId;
    /** 注册用户名 */
    private final String username;
    /** 注册邮箱 */
    private final String email;

    public UserRegisteredEvent(Long userId, String username, String email) {
        this.userId = userId;
        this.username = username;
        this.email = email;
    }

    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
}
