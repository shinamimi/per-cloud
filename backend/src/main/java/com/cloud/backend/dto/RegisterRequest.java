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