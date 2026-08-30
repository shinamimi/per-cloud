package com.cloud.backend.service.system;

import com.cloud.backend.dto.LoginRequest;
import com.cloud.backend.dto.LoginResponse;
import com.cloud.backend.dto.RegisterRequest;
import com.cloud.backend.dto.ResetPasswordRequest;
import com.cloud.backend.dto.SendCodeRequest;

public interface AuthService {

    /**
     * 账号密码登录。
     * 前置条件：账号存在且未禁用；验证码开关开启时须校验通过。
     * 失败时累计失败次数，达阈值锁定账号；成功返回 JWT。
     */
    LoginResponse login(LoginRequest request, String ip);

    /**
     * 注册并自动登录。
     * 前置条件：开放注册开关开启、用户名/邮箱未占用；邮件验证开启时须校验邮箱验证码。
     * 新用户配额取管理端配置的默认值；成功后直接返回 JWT。
     */
    LoginResponse register(RegisterRequest request, String ip);

    /**
     * 发送注册/登录/重置场景的验证码邮件，同一邮箱有发送冷却时间。
     */
    void sendCode(SendCodeRequest request);

    /**
     * 发送忘记密码验证码邮件。
     * 前置条件：邮箱已注册（未注册返回用户不存在）；受发送冷却限制。
     */
    void sendForgotPasswordCode(String email);

    /**
     * 校验邮箱验证码并重置密码。
     * 前置条件：邮件验证开关开启时验证码必须通过；邮箱必须已注册。
     */
    void resetPassword(ResetPasswordRequest request);
}
