package com.cloud.backend.controller;

import com.cloud.backend.annotation.RateLimit;
import com.cloud.backend.dto.*;
import com.cloud.backend.service.system.AuthService;
import com.cloud.backend.service.system.JwtBlacklistService;
import com.cloud.backend.utils.IpUtil;
import com.cloud.backend.utils.JwtTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtBlacklistService jwtBlacklistService;
    private final JwtTokenUtil jwtTokenUtil;

    public AuthController(AuthService authService,
                          JwtBlacklistService jwtBlacklistService,
                          JwtTokenUtil jwtTokenUtil) {
        this.authService = authService;
        this.jwtBlacklistService = jwtBlacklistService;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    /**
     * 账号密码登录，成功返回 JWT。失败按登录尝试策略累计错误次数，超限锁定账号。
     */
    @PostMapping("/login")
    @RateLimit(key = "login", limit = 10, window = 60, dimension = RateLimit.Dimension.IP)
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ip = IpUtil.getClientIp(httpRequest);
        return Result.success(authService.login(request, ip));
    }

    /**
     * 发送验证码（注册 / 登录 / 重置密码场景，按请求中的 captchaType 区分），带发送冷却限制。
     */
    @PostMapping("/send-code")
    @RateLimit(key = "send-code", limit = 5, window = 60, dimension = RateLimit.Dimension.IP)
    public Result<Void> sendCode(@Valid @RequestBody SendCodeRequest request) {
        authService.sendCode(request);
        return Result.success();
    }

    /**
     * 注册新用户（受开放注册开关与邮箱验证开关约束），成功直接返回 JWT 完成自动登录。
     */
    @PostMapping("/register")
    @RateLimit(key = "register", limit = 5, window = 60, dimension = RateLimit.Dimension.IP)
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        String ip = IpUtil.getClientIp(httpRequest);
        return Result.success(authService.register(request, ip));
    }

    /**
     * 忘记密码：向已注册邮箱发送重置验证码（邮箱不存在时返回用户不存在）。
     */
    @PostMapping("/forgot-password")
    @RateLimit(key = "forgot-password", limit = 3, window = 60, dimension = RateLimit.Dimension.IP)
    public Result<Void> forgotPassword(@Valid @RequestBody SendCodeRequest request) {
        authService.sendForgotPasswordCode(request.getEmail());
        return Result.success();
    }

    /**
     * 校验邮箱验证码并重置密码。
     */
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return Result.success();
    }

    /**
     * 登出：将 Authorization 头中的 Bearer Token 加入黑名单，使其立即失效。
     * 黑名单有效期与 Token 剩余有效期一致，Token 自然过期后可被清理。
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            jwtBlacklistService.blacklistToken(token, jwtTokenUtil.getExpirationMs());
        }
        return Result.success();
    }
}
