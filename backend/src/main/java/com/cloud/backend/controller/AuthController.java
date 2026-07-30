package com.cloud.backend.controller;

import com.cloud.backend.dto.*;
import com.cloud.backend.enums.ErrorCode;
import com.cloud.backend.service.system.AuthService;
import com.cloud.backend.service.system.CaptchaService;
import com.cloud.backend.service.system.JwtBlacklistService;
import com.cloud.backend.service.system.EmailService;
import com.cloud.backend.utils.IpUtil;
import com.cloud.backend.utils.JwtTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;
    private final EmailService emailService;
    private final JwtBlacklistService jwtBlacklistService;
    private final JwtTokenUtil jwtTokenUtil;

    public AuthController(AuthService authService,
                          CaptchaService captchaService,
                          EmailService emailService,
                          JwtBlacklistService jwtBlacklistService,
                          JwtTokenUtil jwtTokenUtil) {
        this.authService = authService;
        this.captchaService = captchaService;
        this.emailService = emailService;
        this.jwtBlacklistService = jwtBlacklistService;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ip = IpUtil.getClientIp(httpRequest);
        return Result.success(authService.login(request, ip));
    }

    @PostMapping("/send-code")
    public Result<Void> sendCode(@Valid @RequestBody SendCodeRequest request) {
        if (captchaService.isOnCooldown(request.getEmail())) {
            return Result.fail(ErrorCode.CAPTCHA_COOLDOWN);
        }
        String code = captchaService.generateAndStore(request.getEmail(), request.getCaptchaType());
        String purpose = switch (request.getCaptchaType()) {
            case REGISTER -> "注册验证";
            case RESET_PASSWORD -> "重置密码验证";
            case LOGIN -> "登录验证";
        };
        emailService.sendCaptchaMail(request.getEmail(), code, purpose);
        captchaService.setCooldown(request.getEmail());
        return Result.success();
    }

    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        String ip = IpUtil.getClientIp(httpRequest);
        return Result.success(authService.register(request, ip));
    }

    @PostMapping("/forgot-password")
    public Result<Void> forgotPassword(@Valid @RequestBody SendCodeRequest request) {
        authService.sendForgotPasswordCode(request.getEmail());
        return Result.success();
    }

    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return Result.success();
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            jwtBlacklistService.blacklistToken(token, jwtTokenUtil.getExpirationMs());
        }
        return Result.success();
    }
}
