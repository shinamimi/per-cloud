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
    private Long totalQuota;
    private Long adminBonusQuota;
    private Long rewardQuota;
    private Long usedSpace;
    private Boolean isVip;
    private UserStatus status;
    private LocalDateTime createdAt;

    public AdminUserResponse(Long id, String username, String email, String nickname, String avatar,
                             Role role, Long quota, Long totalQuota, Long adminBonusQuota,
                             Long rewardQuota, Long usedSpace, Boolean isVip,
                             UserStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.nickname = nickname;
        this.avatar = avatar;
        this.role = role;
        this.quota = quota;
        this.totalQuota = totalQuota;
        this.adminBonusQuota = adminBonusQuota;
        this.rewardQuota = rewardQuota;
        this.usedSpace = usedSpace;
        this.isVip = isVip;
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
    public Long getTotalQuota() { return totalQuota; }
    public Long getAdminBonusQuota() { return adminBonusQuota; }
    public Long getRewardQuota() { return rewardQuota; }
    public Long getUsedSpace() { return usedSpace; }
    public Boolean getIsVip() { return isVip; }
    public UserStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
