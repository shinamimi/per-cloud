package com.cloud.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 重置密码请求体 —— 需要验证码（找回密码流程的最后一步）。
 *
 * 修改指引：
 * - 【统一】修改 email 校验       → @NotBlank + @Email；改动影响找回密码接口契约与前端邮箱格式校验；改后需同步找回密码接口契约与前端邮箱格式校验
 * - 【统一】修改 code 字段        → 验证码必填；改动需同步验证码校验逻辑与前端找回密码表单；改后需同步验证码校验逻辑与前端找回密码表单
 * - 【统一】修改 newPassword 校验 → @Size(min=8, max=20) + @Pattern（必须含字母和数字）；改动影响接口契约与前端密码规则提示，
 *                           若放宽需评估与注册密码规则的一致性；改后需同步注册/找回密码接口契约与前端密码规则提示
 * - 【习惯】新增重置参数          → 新增字段并同步 AuthService 重置逻辑与前端，否则该参数不生效
 */
@Data
public class ResetPasswordRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "验证码不能为空")
    private String code;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 20, message = "密码长度8-20位")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码必须包含字母和数字")
    private String newPassword;
}