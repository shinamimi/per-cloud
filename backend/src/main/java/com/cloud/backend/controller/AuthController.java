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

/**
 * 认证控制器 —— 登录、注册、验证码发送、找回/重置密码、登出等未登录可访问的接口入口。
 *
 * 设计思路：
 * 1. 登录/注册记录操作日志（由 AuthService 内部写入），并返回签发好的 JWT
 * 2. 登出采用黑名单机制：将当前 Token 加入黑名单直至其自然过期
 * 3. 客户端 IP 统一由 IpUtil 从请求中提取，用于登录日志审计
 *
 * 修改指引：
 * - 【习惯】登录             → POST /api/auth/login，调 authService.login(request, ip)；公开接口（SecurityConfig /api/auth/** permitAll），
 *                       改动影响登录鉴权与 JWT 签发，失败计数/账号锁定策略在 LoginAttemptService
 * - 【习惯】发送验证码        → POST /api/auth/send-code，调 authService.sendCode(request)；公开接口，带发送冷却限制，改动影响验证码通道
 * - 【习惯】注册             → POST /api/auth/register，调 authService.register(request, ip)；公开接口，受开放注册开关与邮箱验证开关约束，成功直接签发 JWT
 * - 【习惯】忘记密码          → POST /api/auth/forgot-password，调 authService.sendForgotPasswordCode(email)；公开接口，邮箱不存在时返回用户不存在
 * - 【习惯】重置密码          → POST /api/auth/reset-password，调 authService.resetPassword(request)；公开接口，需先校验邮箱验证码
 * - 【习惯】登出             → POST /api/auth/logout，调 jwtBlacklistService.blacklistToken(token, 剩余有效期)；
 *                       从 Authorization 头解析 Bearer Token 加入黑名单直至自然过期；路径同为 permitAll（无 Token 也返回成功）
 * - 【习惯】新增/修改接口      → 在 @RequestMapping("/api/auth") 下新增方法；路径已在 SecurityConfig 白名单内（新增子路径无需改动），
 *                       若改为需登录的接口须移出白名单并同步前端 API 层
 * - 【习惯】请求参数校验       → login/send-code/register/forgot-password/reset-password 均用 @Valid @RequestBody 校验对应 Request DTO，
 *                       字段规则改动需同步 dto 与前端表单
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
