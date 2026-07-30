package com.cloud.backend.controller;

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

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ip = IpUtil.getClientIp(httpRequest);
        return Result.success(authService.login(request, ip));
    }

    @PostMapping("/send-code")
    public Result<Void> sendCode(@Valid @RequestBody SendCodeRequest request) {
        authService.sendCode(request);
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
