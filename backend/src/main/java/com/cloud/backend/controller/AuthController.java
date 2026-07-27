package com.cloud.backend.controller;

import com.cloud.backend.dto.*;
import com.cloud.backend.entity.User;
import com.cloud.backend.enums.CaptchaType;
import com.cloud.backend.enums.Role;
import com.cloud.backend.enums.UserStatus;
import com.cloud.backend.security.LoginUser;
import com.cloud.backend.service.*;
import com.cloud.backend.utils.JwtTokenUtil;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器 —— 处理登录、注册、验证码、找回密码、登出等公开接口。
 *
 * 设计思路：
 * 登录流程：校验参数 → 检查登录锁定 → AuthenticationManager.authenticate() 鉴权
 *   → 成功则生成 Token → 返回 LoginResponse
 * 注册流程：校验唯一性 → 校验验证码 → BCrypt 加密密码 → 入库 → 自动登录（返回 Token）
 * 登出流程：将当前 Token 加入 Redis 黑名单，后续请求自动拦截
 *
 * 为什么手动调用 authenticationManager 而不是用 Security Filter 自动处理？
 * 因为需要在登录成功/失败后做额外操作（记录失败次数、返回自定义字段等）。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtTokenUtil jwtTokenUtil;
    private final JwtBlacklistService jwtBlacklistService;
    private final PasswordEncoder passwordEncoder;
    private final CaptchaService captchaService;
    private final EmailService emailService;
    private final LoginAttemptService loginAttemptService;

    public AuthController(AuthenticationManager authenticationManager,
                          UserService userService,
                          JwtTokenUtil jwtTokenUtil,
                          JwtBlacklistService jwtBlacklistService,
                          PasswordEncoder passwordEncoder,
                          CaptchaService captchaService,
                          EmailService emailService,
                          LoginAttemptService loginAttemptService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtTokenUtil = jwtTokenUtil;
        this.jwtBlacklistService = jwtBlacklistService;
        this.passwordEncoder = passwordEncoder;
        this.captchaService = captchaService;
        this.emailService = emailService;
        this.loginAttemptService = loginAttemptService;
    }

    /**
     * 登录 —— 支持用户名和邮箱登录（UserDetailsServiceImpl.findByAccount 支持两者）
     * SecurityConfig 已配置 /api/auth/** 无需 Token，因此登录接口对未登录用户可访问
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        if (loginAttemptService.isLocked(request.getUsername())) {
            return Result.fail("账号已锁定，请15分钟后再试");
        }
        try {
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword());
            Authentication authentication = authenticationManager.authenticate(authToken);
            LoginUser loginUser = (LoginUser) authentication.getPrincipal();

            loginAttemptService.loginSucceeded(request.getUsername());

            String token = jwtTokenUtil.generateToken(loginUser);
            LoginResponse response = new LoginResponse(
                    token,
                    loginUser.getUserId(),
                    loginUser.getUsername(),
                    loginUser.getRole().getValue()
            );
            return Result.success(response);
        } catch (LockedException e) {
            return Result.fail("账号已被禁用");
        } catch (DisabledException e) {
            loginAttemptService.loginFailed(request.getUsername());
            return Result.fail("账号已被禁用");
        } catch (BadCredentialsException e) {
            loginAttemptService.loginFailed(request.getUsername());
            return Result.fail("用户名或密码错误");
        }
    }

    /**
     * 发送验证码 —— 带 60 秒冷却限制
     * 先检查冷却，再生成验证码，发送邮件后才设置冷却（防止生成失败后也需要等冷却）
     */
    @PostMapping("/send-code")
    public Result<Void> sendCode(@Valid @RequestBody SendCodeRequest request) {
        if (captchaService.isOnCooldown(request.getEmail())) {
            return Result.fail("请60秒后再发送");
        }

        String code = captchaService.generateAndStore(request.getEmail(), request.getCaptchaType());
        String purpose = switch (request.getCaptchaType()) {
            case REGISTER -> "注册验证";
            case RESET_PASSWORD -> "重置密码验证";
        };
        emailService.sendCaptchaMail(request.getEmail(), code, purpose);
        captchaService.setCooldown(request.getEmail());
        return Result.success();
    }

    /**
     * 注册 —— 需要邮箱 + 验证码
     * 注册成功后直接返回 Token（免登录）
     */
    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        if (userService.existsByUsername(request.getUsername())) {
            return Result.fail("用户名已存在");
        }
        if (userService.existsByEmail(request.getEmail())) {
            return Result.fail("邮箱已被注册");
        }
        if (!captchaService.verify(request.getEmail(), CaptchaType.REGISTER, request.getCode())) {
            return Result.fail("验证码错误或已过期");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setRole(Role.USER);
        user.setStatus(UserStatus.NORMAL);

        userService.register(user);

        LoginUser loginUser = new LoginUser(user);
        String token = jwtTokenUtil.generateToken(loginUser);
        LoginResponse response = new LoginResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getRole().getValue()
        );
        return Result.success(response);
    }

    /**
     * 忘记密码 —— 发送重置密码的验证码到邮箱
     * 与 send-code 的区别：这里需要先确认邮箱已注册
     */
    @PostMapping("/forgot-password")
    public Result<Void> forgotPassword(@Valid @RequestBody SendCodeRequest request) {
        User user = userService.findByEmail(request.getEmail());
        if (user == null) {
            return Result.fail("该邮箱未注册");
        }
        if (captchaService.isOnCooldown(request.getEmail())) {
            return Result.fail("请60秒后再发送");
        }

        String code = captchaService.generateAndStore(request.getEmail(), request.getCaptchaType());
        emailService.sendCaptchaMail(request.getEmail(), code, "重置密码验证");
        captchaService.setCooldown(request.getEmail());
        return Result.success();
    }

    /**
     * 重置密码 —— 验证码 + 新密码
     */
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        User user = userService.findByEmail(request.getEmail());
        if (user == null) {
            return Result.fail("用户不存在");
        }
        if (!captchaService.verify(request.getEmail(), CaptchaType.RESET_PASSWORD, request.getCode())) {
            return Result.fail("验证码错误或已过期");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userService.update(user);
        return Result.success();
    }

    /**
     * 登出 —— 将 Token 加入黑名单
     * 实现原理见 JwtBlacklistService：缓存当前 Token 到 Redis，TTL=Token 剩余有效期
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