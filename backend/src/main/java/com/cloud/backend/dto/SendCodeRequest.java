package com.cloud.backend.dto;

import com.cloud.backend.enums.CaptchaTypeEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发送验证码请求体 —— 邮箱 + 验证码用途类型。
 * captchaType 区分注册/找回密码，便于 CaptchaService 生成不同的 Redis Key。
 */
@Data
public class SendCodeRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotNull(message = "验证码类型不能为空")
    private CaptchaTypeEnum captchaType;
}