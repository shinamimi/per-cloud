package com.cloud.backend.service.system.impl;

import com.cloud.backend.constant.FileConstants;
import com.cloud.backend.dto.LoginRequest;
import com.cloud.backend.dto.LoginResponse;
import com.cloud.backend.dto.RegisterRequest;
import com.cloud.backend.dto.ResetPasswordRequest;
import com.cloud.backend.dto.SendCodeRequest;
import com.cloud.backend.entity.OperationLog;
import com.cloud.backend.entity.User;
import com.cloud.backend.enums.CaptchaTypeEnum;
import com.cloud.backend.enums.ErrorCodeEnum;
import com.cloud.backend.enums.OperationTypeEnum;
import com.cloud.backend.enums.RoleEnum;
import com.cloud.backend.enums.TargetTypeEnum;
import com.cloud.backend.enums.UserStatusEnum;
import com.cloud.backend.exception.BusinessException;
import com.cloud.backend.security.LoginUser;
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

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           UserService userService,
                           JwtTokenUtil jwtTokenUtil,
                           CaptchaService captchaService,
                           EmailService emailService,
                           LoginAttemptService loginAttemptService,
                           OperationLogService operationLogService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtTokenUtil = jwtTokenUtil;
        this.captchaService = captchaService;
        this.emailService = emailService;
        this.loginAttemptService = loginAttemptService;
        this.operationLogService = operationLogService;
    }

    @Override
    public LoginResponse login(LoginRequest request, String ip) {
        if (loginAttemptService.isLocked(request.getUsername())) {
            throw new BusinessException(ErrorCodeEnum.LOGIN_LOCKED);
        }
        if (request.getCaptchaCode() != null && !request.getCaptchaCode().isEmpty()) {
            if (!captchaService.verify(request.getCaptchaId(), request.getCaptchaCode())) {
                throw new BusinessException(ErrorCodeEnum.CAPTCHA_INVALID);
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
            log.setOperation(OperationTypeEnum.LOGIN);
            log.setTargetType(TargetTypeEnum.USER);
            log.setTargetId(loginUser.getUserId());
            log.setIp(ip);
            operationLogService.log(log);

            return new LoginResponse(token, loginUser.getUserId(), loginUser.getUsername(), loginUser.getRole().getValue());
        } catch (DisabledException e) {
            loginAttemptService.loginFailed(request.getUsername());
            throw new BusinessException(ErrorCodeEnum.ACCOUNT_DISABLED);
        } catch (BadCredentialsException e) {
            loginAttemptService.loginFailed(request.getUsername());
            throw new BusinessException(ErrorCodeEnum.WRONG_CREDENTIALS);
        }
    }

    @Override
    public LoginResponse register(RegisterRequest request, String ip) {
        if (userService.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCodeEnum.USER_ALREADY_EXISTS);
        }
        if (userService.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCodeEnum.EMAIL_ALREADY_EXISTS);
        }
        if (!captchaService.verify(request.getEmail(), CaptchaTypeEnum.REGISTER, request.getCode())) {
            throw new BusinessException(ErrorCodeEnum.CAPTCHA_INVALID);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setEmail(request.getEmail());
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setRole(RoleEnum.USER);
        user.setStatus(UserStatusEnum.NORMAL);
        user.setQuota(FileConstants.DEFAULT_QUOTA);
        user.setUsedSpace(0L);

        userService.register(user);

        LoginUser loginUser = new LoginUser(user);
        String token = jwtTokenUtil.generateToken(loginUser);

        OperationLog log = new OperationLog();
        log.setUserId(user.getId());
        log.setOperation(OperationTypeEnum.REGISTER);
        log.setTargetType(TargetTypeEnum.USER);
        log.setTargetId(user.getId());
        log.setIp(ip);
        operationLogService.log(log);

        return new LoginResponse(token, user.getId(), user.getUsername(), user.getRole().getValue());
    }

    @Override
    public void sendCode(SendCodeRequest request) {
        if (captchaService.isOnCooldown(request.getEmail())) {
            throw new BusinessException(ErrorCodeEnum.CAPTCHA_COOLDOWN);
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

    @Override
    public void sendForgotPasswordCode(String email) {
        if (!userService.existsByEmail(email)) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        if (captchaService.isOnCooldown(email)) {
            throw new BusinessException(ErrorCodeEnum.CAPTCHA_COOLDOWN);
        }
        String code = captchaService.generateAndStore(email, CaptchaTypeEnum.RESET_PASSWORD);
        emailService.sendCaptchaMail(email, code, "重置密码验证");
        captchaService.setCooldown(email);
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        if (!captchaService.verify(request.getEmail(), CaptchaTypeEnum.RESET_PASSWORD, request.getCode())) {
            throw new BusinessException(ErrorCodeEnum.CAPTCHA_INVALID);
        }
        User user = userService.findByEmail(request.getEmail());
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        userService.updatePassword(user.getId(), request.getNewPassword());
    }
}
