package com.cloud.backend.dto.admin;

import com.cloud.backend.enums.Role;
import com.cloud.backend.enums.UserStatus;

import java.time.LocalDateTime;

public class AdminUserResponse {

    private Long id;
    private String username;
    private String email;
    private String nickname;
    private String avatar;
    private Role role;
    private Long quota;
    private Long usedSpace;
    private UserStatus status;
    private LocalDateTime createdAt;

    public AdminUserResponse(Long id, String username, String email, String nickname, String avatar,
                             Role role, Long quota, Long usedSpace, UserStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.nickname = nickname;
        this.avatar = avatar;
        this.role = role;
        this.quota = quota;
        this.usedSpace = usedSpace;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getNickname() { return nickname; }
    public String getAvatar() { return avatar; }
    public Role getRole() { return role; }
    public Long getQuota() { return quota; }
    public Long getUsedSpace() { return usedSpace; }
    public UserStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
