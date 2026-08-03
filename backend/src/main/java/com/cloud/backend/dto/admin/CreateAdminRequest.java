package com.cloud.backend.dto.admin;

import com.cloud.backend.enums.Role;
import lombok.Data;

/**
 * 创建管理员请求 DTO —— 后台新建管理员账号的入参。
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