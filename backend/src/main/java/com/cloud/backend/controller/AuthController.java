package com.cloud.backend.controller;

import com.cloud.backend.dto.*;
import com.cloud.backend.service.system.AuthService;
import com.cloud.backend.service.system.JwtBlacklistService;
import com.cloud.backend.utils.IpUtil;
import com.cloud.backend.utils.JwtTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器 —— 登录、注册、验证码发送、找回/重置密码、登出等未登录可访问的接口入口。
 *
 * 设计思路：
 * 1. 登录/注册记录操作日志（由 AuthService 内部写入），并返回签发好的 JWT
 * 2. 登出采用黑名单机制：将当前 Token 加入黑名单直至其自然过期
 * 3. 客户端 IP 统一由 IpUtil 从请求中提取，用于登录日志审计
 */
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
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ip = IpUtil.getClientIp(httpRequest);
        return Result.success(authService.login(request, ip));
    }

    /**
     * 发送验证码（注册 / 登录 / 重置密码场景，按请求中的 captchaType 区分），带发送冷却限制。
     */
    @PostMapping("/send-code")
    public Result<Void> sendCode(@Valid @RequestBody SendCodeRequest request) {
        authService.sendCode(request);
        return Result.success();
    }

    /**
     * 注册新用户（受开放注册开关与邮箱验证开关约束），成功直接返回 JWT 完成自动登录。
     */
    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        String ip = IpUtil.getClientIp(httpRequest);
        return Result.success(authService.register(request, ip));
    }

    /**
     * 忘记密码：向已注册邮箱发送重置验证码（邮箱不存在时返回用户不存在）。
     */
    @PostMapping("/forgot-password")
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
