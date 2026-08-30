package com.cloud.backend.service.system.impl;

import com.cloud.backend.dto.LoginRequest;
import com.cloud.backend.dto.LoginResponse;
import com.cloud.backend.dto.RegisterRequest;
import com.cloud.backend.dto.ResetPasswordRequest;
import com.cloud.backend.dto.SendCodeRequest;
import com.cloud.backend.entity.OperationLog;
import com.cloud.backend.entity.User;
import com.cloud.backend.enums.CaptchaType;
import com.cloud.backend.enums.ErrorCode;
import com.cloud.backend.enums.OperationType;
import com.cloud.backend.enums.Role;
import com.cloud.backend.enums.TargetType;
import com.cloud.backend.enums.UserStatus;
import com.cloud.backend.exception.BusinessException;
import com.cloud.backend.security.LoginUser;
import com.cloud.backend.service.admin.AdminSettingsService;
import com.cloud.backend.service.system.AuthService;
import com.cloud.backend.service.system.CaptchaService;
import com.cloud.backend.service.system.EmailService;
import com.cloud.backend.service.system.LoginAttemptService;
import com.cloud.backend.service.system.OperationLogService;
import com.cloud.backend.service.user.UserService;
import com.cloud.backend.utils.JwtTokenUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtTokenUtil jwtTokenUtil;
    private final CaptchaService captchaService;
    private final EmailService emailService;
    private final LoginAttemptService loginAttemptService;
    private final OperationLogService operationLogService;
    private final AdminSettingsService settingsService;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           UserService userService,
                           JwtTokenUtil jwtTokenUtil,
                           CaptchaService captchaService,
                           EmailService emailService,
                           LoginAttemptService loginAttemptService,
                           OperationLogService operationLogService,
                           AdminSettingsService settingsService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtTokenUtil = jwtTokenUtil;
        this.captchaService = captchaService;
        this.emailService = emailService;
        this.loginAttemptService = loginAttemptService;
        this.operationLogService = operationLogService;
        this.settingsService = settingsService;
    }

    /**
     * 登录：校验账号密码，签发 JWT 并记录登录日志。
     * 前置条件：账号存在且未禁用；验证码开关开启且前端传了验证码时必须校验通过。
     * 副作用：失败累计登录尝试次数，达阈值锁定账号；锁定期内直接拒绝。
     */
    @Override
    public LoginResponse login(LoginRequest request, String ip) {
        User user = userService.findByAccount(request.getUsername());
        if (user != null && user.getStatus() == UserStatus.LOCKED) {
            if (loginAttemptService.isLocked(request.getUsername())) {
                throw new BusinessException(ErrorCode.LOGIN_LOCKED);
            }
            // 已过锁定窗口：先解除锁定标记，允许本次正常尝试
            user.setStatus(UserStatus.NORMAL);
            userService.update(user);
        }
        // 登录验证码开关（system.enable-captcha）：关闭时忽略前端传的验证码
        if (settingsService.isCaptchaEnabled()
                && request.getCaptchaCode() != null && !request.getCaptchaCode().isEmpty()) {
            if (!captchaService.verify(request.getCaptchaId(), request.getCaptchaCode())) {
                throw new BusinessException(ErrorCode.CAPTCHA_INVALID);
            }
        }
        try {
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword());
            Authentication authentication = authenticationManager.authenticate(authToken);
            LoginUser loginUser = (LoginUser) authentication.getPrincipal();

            loginAttemptService.loginSucceeded(request.getUsername());

            String token = jwtTokenUtil.generateToken(loginUser);

            OperationLog log = new OperationLog();
            log.setUserId(loginUser.getUserId());
            log.setOperation(OperationType.LOGIN);
            log.setTargetType(TargetType.USER);
            log.setTargetId(loginUser.getUserId());
            log.setIp(ip);
            operationLogService.log(log);

            return new LoginResponse(token, loginUser.getUserId(), loginUser.getUsername(), loginUser.getRole().getValue());
        } catch (DisabledException e) {
            loginAttemptService.loginFailed(request.getUsername());
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        } catch (BadCredentialsException e) {
            loginAttemptService.loginFailed(request.getUsername());
            // 失败次数达到阈值后把账号落库为 LOCKED，配合顶部逻辑实现锁定期自动解锁
            if (loginAttemptService.isLocked(request.getUsername())) {
                User lockedUser = userService.findByAccount(request.getUsername());
                if (lockedUser != null) {
                    lockedUser.setStatus(UserStatus.LOCKED);
                    userService.update(lockedUser);
                }
            }
            throw new BusinessException(ErrorCode.WRONG_CREDENTIALS);
        }
    }

    /**
     * 注册并自动登录：校验开关与唯一性，创建用户（默认配额），签发 JWT 并记录注册日志。
     * 前置条件：开放注册开关开启；用户名/邮箱未占用；邮件验证开启时须通过邮箱验证码。
     */
    @Override
    public LoginResponse register(RegisterRequest request, String ip) {
        // 开放注册开关（system.allow-register，ADMIN 配置）
        if (!settingsService.isAllowRegister()) {
            throw new BusinessException(ErrorCode.REGISTER_DISABLED);
        }
        if (userService.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }
        if (userService.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        // 邮件验证开关（system.enable-mail-verify）：关闭时注册不校验邮箱验证码
        if (settingsService.isMailVerifyEnabled()
                && !captchaService.verify(request.getEmail(), CaptchaType.REGISTER, request.getCode())) {
            throw new BusinessException(ErrorCode.CAPTCHA_INVALID);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setEmail(request.getEmail());
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setRole(Role.USER);
        user.setStatus(UserStatus.NORMAL);
        user.setIsVip(false);
        user.setAdminBonusQuota(0L);
        user.setRewardQuota(0L);
        // 新用户默认配额（storage.default-quota-user，ADMIN 配置，只影响新注册用户）
        user.setQuota(settingsService.getDefaultQuotaUser());
        user.setUsedSpace(0L);

        userService.register(user);

        LoginUser loginUser = new LoginUser(user);
        String token = jwtTokenUtil.generateToken(loginUser);

        OperationLog log = new OperationLog();
        log.setUserId(user.getId());
        log.setOperation(OperationType.REGISTER);
        log.setTargetType(TargetType.USER);
        log.setTargetId(user.getId());
        log.setIp(ip);
        operationLogService.log(log);

        return new LoginResponse(token, user.getId(), user.getUsername(), user.getRole().getValue());
    }

    /**
     * 发送注册/登录/重置密码场景的验证码邮件。
     * 前置条件：不在冷却期内（同一邮箱发信有冷却限制）。
     * 副作用：生成验证码并写入存储，随后设置冷却时间。
     */
    @Override
    public void sendCode(SendCodeRequest request) {
        if (captchaService.isOnCooldown(request.getEmail())) {
            throw new BusinessException(ErrorCode.CAPTCHA_COOLDOWN);
        }
        String code = captchaService.generateAndStore(request.getEmail(), request.getCaptchaType());
        String purpose = switch (request.getCaptchaType()) {
            case REGISTER -> "注册验证";
            case RESET_PASSWORD -> "重置密码验证";
            case LOGIN -> "登录验证";
        };
        emailService.sendCaptchaMail(request.getEmail(), code, purpose);
        captchaService.setCooldown(request.getEmail());
    }

    /**
     * 发送忘记密码验证码邮件。
     * 前置条件：邮箱已注册（防止向未注册邮箱发信）；不在冷却期内。
     */
    @Override
    public void sendForgotPasswordCode(String email) {
        if (!userService.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (captchaService.isOnCooldown(email)) {
            throw new BusinessException(ErrorCode.CAPTCHA_COOLDOWN);
        }
        String code = captchaService.generateAndStore(email, CaptchaType.RESET_PASSWORD);
        emailService.sendCaptchaMail(email, code, "重置密码验证");
        captchaService.setCooldown(email);
    }

    /**
     * 校验邮箱验证码并重置密码。
     * 前置条件：邮件验证开关开启时验证码必须通过；邮箱必须已注册。
     * 副作用：直接更新用户密码（BCrypt 加密入库）。
     */
    @Override
    public void resetPassword(ResetPasswordRequest request) {
        if (settingsService.isMailVerifyEnabled()
                && !captchaService.verify(request.getEmail(), CaptchaType.RESET_PASSWORD, request.getCode())) {
            throw new BusinessException(ErrorCode.CAPTCHA_INVALID);
        }
        User user = userService.findByEmail(request.getEmail());
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        userService.updatePassword(user.getId(), request.getNewPassword());
    }
}
