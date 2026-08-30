package com.cloud.backend.entity;

import com.cloud.backend.enums.Role;
import com.cloud.backend.enums.UserStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {

    private Long id;
    private String username;
    private String password;
    private String email;
    private String nickname;
    private String avatar;
    private Role role;
    private Long quota;
    private Long usedSpace;
    private UserStatus status;
    private Boolean isVip;
    private Long adminBonusQuota;
    private Long rewardQuota;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}