package com.cloud.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录响应体 —— 返回 Token 及用户基本信息。
 * 前端需要 token（存入 localStorage/header）、userId、username、role（用于路由鉴权）。
 */
@Data
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private Long userId;
    private String username;
    private Integer role;
}