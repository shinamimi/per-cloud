package com.cloud.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求体 —— 用户名 + 密码 + 可选验证码。
 * captchaCode 验证码用于防暴力破解（MVP 阶段可选，不传则跳过验证码校验）。
 *
 * 修改指引：
 * - 【统一】修改 username/password 字段名 → String；对应登录表单字段，改动需同步 AuthService 登录逻辑与前端登录表单；改后需同步 AuthService 登录逻辑与前端登录表单
 * - 【统一】修改必填校验          → @NotBlank 注解；当前 username/password 必填，放宽/加严会影响前端登录校验提示与接口契约；改后需同步前端登录校验提示与接口契约
 * - 【统一】修改验证码字段        → captchaId/captchaCode；当前可选（不传则跳过校验），改动需同步 AuthService 验证码校验逻辑与前端登录表单；改后需同步 AuthService 验证码校验逻辑与前端登录表单
 * - 【习惯】新增登录参数          → 新增字段并同步 AuthService 与前端，否则该参数不生效
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