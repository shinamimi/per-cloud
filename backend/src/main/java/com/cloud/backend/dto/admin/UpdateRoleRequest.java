package com.cloud.backend.dto.admin;

import com.cloud.backend.enums.Role;
import lombok.Data;

/**
 * 管理员角色调整请求 DTO —— 修改单个管理员账号的角色。
 *
 * 修改指引：
 * - 【习惯】修改 role             → 自定义枚举 Role（enums/Role.java：USER=0/OPERATOR=10/ADMIN=20/SUPER_ADMIN=100）；
 *                           仅允许 USER/OPERATOR/ADMIN（不可 SUPER_ADMIN，由服务层校验），改动需同步校验与前端角色选择
 * - 【习惯】修改字段名/类型        → 对应用户角色调整接口请求参数，改动需同步 service 与前端
 */
@Data
public class UpdateRoleRequest {
    /** 目标角色（USER / OPERATOR / ADMIN，不可为 SUPER_ADMIN，由服务层校验） */
    private Role role;
}