package com.cloud.backend.dto;

import com.cloud.backend.enums.CaptchaType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendCodeRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotNull(message = "验证码类型不能为空")
    private CaptchaType captchaType;
}