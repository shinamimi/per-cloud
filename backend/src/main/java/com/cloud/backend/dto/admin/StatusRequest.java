package com.cloud.backend.dto.admin;

import com.cloud.backend.enums.UserStatus;
import lombok.Data;

@Data
public class StatusRequest {
    /** 目标状态（NORMAL=正常 / DISABLED=禁用 / LOCKED=锁定 / INACTIVE=未激活） */
    private UserStatus status;
}