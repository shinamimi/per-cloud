package com.cloud.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录响应体 —— 返回 Token 及用户基本信息。
 * 前端需要 token（存入 localStorage/header）、userId、username、role（用于路由鉴权）。
 *
 * 修改指引：
 * - 【习惯】修改字段名/类型       → 响应字段为前端登录成功后的取值依据（token 存 localStorage 并随请求头回传、role 用于路由鉴权），
 *                           改动需同步 AuthService 组装逻辑与前端
 * - 【习惯】修改 role 取值        → Integer，取 Role 枚举的 value（USER=0/OPERATOR=10/ADMIN=20/SUPER_ADMIN=100，AuthService 中由
 *                           getRole().getValue() 填充）；改动影响前端角色判断与接口权限，需同步 Role 枚举定义
 * - 【习惯】新增响应字段          → 新增字段并同步 AuthService 组装逻辑与前端，否则字段恒为 null
 */
@Data
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private Long userId;
    private String username;
    private Integer role;
}