package com.cloud.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求体 —— 用户名 + 密码 + 可选验证码。
 * captchaCode 验证码用于防暴力破解（MVP 阶段可选，不传则跳过验证码校验）。
 */
@Data
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    private String captchaId;

    private String captchaCode;
}