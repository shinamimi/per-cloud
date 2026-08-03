package com.cloud.backend.event;

/**
 * 用户注册事件 —— 用户注册成功后由注册流程发布，携带注册结果快照。
 *
 * 设计思路：
 * 字段不可变（final），事件只承载已发生事实，避免监听器侧修改语义。
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
