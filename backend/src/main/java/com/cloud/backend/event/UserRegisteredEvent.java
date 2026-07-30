package com.cloud.backend.event;

public class UserRegisteredEvent {

    private final Long userId;
    private final String username;
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
