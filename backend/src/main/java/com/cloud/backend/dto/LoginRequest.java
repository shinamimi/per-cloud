package com.cloud.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求体 —— 用户名 + 密码。
 * 使用 @NotBlank 避免前端传入空字符串导致空指针。
 */
@Data
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}