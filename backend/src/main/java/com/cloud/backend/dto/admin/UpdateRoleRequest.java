package com.cloud.backend.dto.admin;

import com.cloud.backend.enums.Role;
import lombok.Data;

/**
 * 管理员角色调整请求 DTO —— 修改单个管理员账号的角色。
 */
@Data
public class UpdateRoleRequest {
    /** 目标角色（USER / OPERATOR / ADMIN，不可为 SUPER_ADMIN，由服务层校验） */
    private Role role;
}