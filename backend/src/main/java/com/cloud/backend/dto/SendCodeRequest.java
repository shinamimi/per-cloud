package com.cloud.backend.dto;

import com.cloud.backend.enums.CaptchaType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发送验证码请求体 —— 邮箱 + 验证码用途类型。
 * captchaType 区分注册/找回密码，便于 CaptchaService 生成不同的 Redis Key。
 *
 * 修改指引：
 * - 【习惯】修改 email 校验       → @NotBlank + @Email；改动影响发送验证码接口契约与前端邮箱格式校验
 * - 【习惯】修改 captchaType 字段 → 自定义枚举 CaptchaType（enums/CaptchaType.java）：REGISTER/RESET_PASSWORD/LOGIN；
 *                           作为请求参数（JSON）传入并拼入 Redis Key（captcha:xxx:email），改动需同步前端取值与 Redis Key 约定
 * - 【习惯】修改必填校验          → @NotNull 注解；当前 captchaType 必填，放宽会导致无法区分验证码场景
 * - 【习惯】新增验证码场景        → 在 CaptchaType 枚举末尾追加并同步 CaptchaService 场景分发逻辑，否则新场景发不出或校验不到
 */
@Data
public class SendCodeRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotNull(message = "验证码类型不能为空")
    private CaptchaType captchaType;
}