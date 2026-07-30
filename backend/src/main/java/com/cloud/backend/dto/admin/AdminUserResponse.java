package com.cloud.backend.dto.admin;

import com.cloud.backend.enums.RoleEnum;
import com.cloud.backend.enums.UserStatusEnum;

import java.time.LocalDateTime;

public class AdminUserResponse {

    private Long id;
    private String username;
    private String email;
    private String nickname;
    private String avatar;
    private RoleEnum role;
    private Long quota;
    private Long usedSpace;
    private UserStatusEnum status;
    private LocalDateTime createdAt;

    public AdminUserResponse(Long id, String username, String email, String nickname, String avatar,
                             RoleEnum role, Long quota, Long usedSpace, UserStatusEnum status, LocalDateTime createdAt) {
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
    public RoleEnum getRoleEnum() { return role; }
    public Long getQuota() { return quota; }
    public Long getUsedSpace() { return usedSpace; }
    public UserStatusEnum getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
