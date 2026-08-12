package com.cloud.backend.dto.admin;

import com.cloud.backend.enums.Role;
import lombok.Data;

/**
 * 创建管理员请求 DTO —— 后台新建管理员账号的入参。
 *
 * 修改指引：
 * - 【统一】修改 username/password/email/nickname → 对应创建管理员接口请求参数；改后需同步 AdminService 创建逻辑与前端表单
 * - 【统一】修改 role             → 自定义枚举 Role（enums/Role.java：USER=0/OPERATOR=10/ADMIN=20/SUPER_ADMIN=100）；
 *                           服务层校验仅允许 OPERATOR/ADMIN（不可 SUPER_ADMIN）；改后需同步校验与前端角色选择
 * - 【统一】新增入参              → 新增字段并同步创建逻辑与前端，否则该参数不生效；改后需同步创建逻辑与前端
 */
@Data
public class CreateAdminRequest {
    /** 登录用户名 */
    private String username;
    /** 初始密码（明文，入库前加密） */
    private String password;
    /** 邮箱 */
    private String email;
    /** 昵称 */
    private String nickname;
    /** 目标角色（OPERATOR / ADMIN，不可为 SUPER_ADMIN，由服务层校验） */
    private Role role;
}