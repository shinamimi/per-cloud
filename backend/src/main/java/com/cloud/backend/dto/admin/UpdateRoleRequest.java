package com.cloud.backend.dto.admin;

import com.cloud.backend.enums.Role;
import lombok.Data;

@Data
public class UpdateRoleRequest {
    /** 目标角色（USER / OPERATOR / ADMIN，不可为 SUPER_ADMIN，由服务层校验） */
    private Role role;
}