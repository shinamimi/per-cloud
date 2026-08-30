package com.cloud.backend.event;

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
