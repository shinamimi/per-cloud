package com.cloud.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求体。
 *
 * 密码校验规则：8-20 位，必须同时包含字母和数字。
 * 前端应做同样的校验提示，后端作为安全兜底。
 *
 * 修改指引：
 * - 【习惯】修改 username 校验    → @Size(min=3, max=32)；改动用户名长度限制会影响注册接口契约与前端校验提示，需同步前端
 * - 【习惯】修改 password 校验    → @Size(min=8, max=20) + @Pattern（必须含字母和数字）；改动影响注册/登录接口契约与前端密码规则提示，
 *                           存量用户密码若不合规将无法登录
 * - 【习惯】修改 email 校验       → @NotBlank + @Email；改动影响注册接口契约与前端邮箱格式校验
 * - 【习惯】修改 nickname         → 可选昵称字段；改动影响用户展示，需同步前端
 * - 【习惯】修改验证码字段        → code 必填；改动需同步验证码校验逻辑与前端注册表单
 * - 【习惯】新增注册参数          → 新增字段并同步 AuthService 注册逻辑与前端，否则该参数不生效
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 32, message = "用户名长度3-32位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 20, message = "密码长度8-20位")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码必须包含字母和数字")
    private String password;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    private String nickname;

    @NotBlank(message = "验证码不能为空")
    private String code;
}