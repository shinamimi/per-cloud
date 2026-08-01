package com.cloud.backend.controller.admin;

import com.cloud.backend.dto.Result;
import com.cloud.backend.dto.admin.*;
import com.cloud.backend.service.admin.AdminSettingsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统配置中心（管理端，docs/system-config-center.md）。
 * 权限：/api/admin/settings/** 默认 OPERATOR+；/system 与 /mail 两个敏感分组仅 ADMIN+（SecurityConfig 细化）。
 * 所有分组独立保存；null 字段表示恢复配置文件默认值（删除 t_setting 配置行）。
 */
@RestController
@RequestMapping("/api/admin/settings")
public class AdminSettingsController {

    /** SMTP 密码脱敏占位符（与 Service 层保持一致） */
    private static final String PASSWORD_MASK = "********";

    private final AdminSettingsService adminSettingsService;

    public AdminSettingsController(AdminSettingsService adminSettingsService) {
        this.adminSettingsService = adminSettingsService;
    }

    /** 返回全部分组配置（SMTP 密码脱敏） */
    @GetMapping
    public Result<Map<String, Object>> getSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("upload", Map.of(
                "maxSizeUser", adminSettingsService.getMaxSizeUser(),
                "maxSizeVip", adminSettingsService.getMaxSizeVip(),
                "maxConcurrentUser", adminSettingsService.getMaxConcurrentUser(),
                "maxConcurrentVip", adminSettingsService.getMaxConcurrentVip()));
        settings.put("storage", Map.of(
                "defaultQuotaUser", adminSettingsService.getDefaultQuotaUser(),
                "defaultQuotaVip", adminSettingsService.getDefaultQuotaVip()));
        settings.put("session", Map.of(
                "accessTokenTtlMinutes", adminSettingsService.getAccessTokenTtlMs() / 60000,
                "captchaTtlSeconds", adminSettingsService.getCaptchaTtlSeconds(),
                "loginLockThreshold", adminSettingsService.getLoginLockThreshold(),
                "loginLockDurationMinutes", adminSettingsService.getLoginLockDurationMinutes(),
                "resetPasswordTtlMinutes", adminSettingsService.getResetPasswordTtlMinutes()));
        settings.put("cache", Map.of(
                "captcha", adminSettingsService.getCaptchaTtlSeconds(),
                "loginAttempt", adminSettingsService.getLoginAttemptTtlSeconds(),
                "blacklist", adminSettingsService.getBlacklistTokenTtlSeconds(),
                "filePreview", 0L,
                "downloadLinkMinutes", adminSettingsService.getDownloadLinkTtlMinutes()));
        settings.put("system", Map.of(
                "allowRegister", adminSettingsService.isAllowRegister(),
                "allowGuestShare", adminSettingsService.isAllowGuestShare(),
                "enableMailVerify", adminSettingsService.isMailVerifyEnabled(),
                "enableCaptcha", adminSettingsService.isCaptchaEnabled(),
                "enableOperationLog", adminSettingsService.isOperationLogEnabled()));
        settings.put("file", Map.of(
                "recycleBinDays", adminSettingsService.getRecycleBinDays()));
        settings.put("mail", mailGroup());
        settings.put("log", Map.of(
                "operationDays", adminSettingsService.getOperationLogDays(),
                "loginDays", adminSettingsService.getLoginLogDays()));
        return Result.success(settings);
    }

    /** mail 分组使用 HashMap：SMTP 未配置时 host/username 等为 null，Map.of 不允许 null 值 */
    private Map<String, Object> mailGroup() {
        Map<String, Object> mail = new HashMap<>();
        mail.put("enabled", adminSettingsService.isMailEnabled());
        mail.put("host", adminSettingsService.getMailHost());
        mail.put("port", adminSettingsService.getMailPort());
        mail.put("username", adminSettingsService.getMailUsername());
        mail.put("password", adminSettingsService.getMailPassword() == null ? null : PASSWORD_MASK);
        mail.put("fromName", adminSettingsService.getMailFromName());
        mail.put("frequencyLimit", adminSettingsService.getMailFrequencyLimitSeconds());
        return mail;
    }

    @PutMapping("/upload")
    public Result<Void> updateUploadLimits(@RequestBody AdminUploadLimitsRequest request) {
        adminSettingsService.updateUploadLimits(
                request.getMaxSizeUser(),
                request.getMaxSizeVip(),
                request.getMaxConcurrentUser(),
                request.getMaxConcurrentVip());
        return Result.success();
    }

    @PutMapping("/storage")
    public Result<Void> updateStorage(@RequestBody StorageSettingsRequest request) {
        adminSettingsService.updateStorage(request.getDefaultQuotaUser(), request.getDefaultQuotaVip());
        return Result.success();
    }

    @PutMapping("/session")
    public Result<Void> updateSession(@RequestBody SessionSettingsRequest request) {
        adminSettingsService.updateSession(
                request.getAccessTokenTtlMinutes(),
                request.getCaptchaTtlSeconds(),
                request.getLoginLockThreshold(),
                request.getLoginLockDurationMinutes(),
                request.getResetPasswordTtlMinutes());
        return Result.success();
    }

    @PutMapping("/cache")
    public Result<Void> updateCache(@RequestBody CacheSettingsRequest request) {
        adminSettingsService.updateCache(
                request.getCaptcha(),
                request.getLoginAttempt(),
                request.getBlacklist(),
                request.getFilePreview(),
                request.getDownloadLinkMinutes());
        return Result.success();
    }

    /** 系统功能开关（仅 ADMIN+，SecurityConfig 限制） */
    @PutMapping("/system")
    public Result<Void> updateSystem(@RequestBody SystemSettingsRequest request) {
        adminSettingsService.updateSystem(
                request.getAllowRegister(),
                request.getAllowGuestShare(),
                request.getEnableMailVerify(),
                request.getEnableCaptcha(),
                request.getEnableOperationLog());
        return Result.success();
    }

    @PutMapping("/file")
    public Result<Void> updateFile(@RequestBody FileSettingsRequest request) {
        adminSettingsService.updateFile(
                request.getRecycleBinDays(),
                request.getShareDefaultValidDays(),
                request.getShareMaxValidDays(),
                request.getShareMaxCountPerFile(),
                request.getShareDefaultRequirePassword());
        return Result.success();
    }

    /** 邮件服务（仅 ADMIN+，SecurityConfig 限制） */
    @PutMapping("/mail")
    public Result<Void> updateMail(@RequestBody MailSettingsRequest request) {
        adminSettingsService.updateMail(
                request.getEnabled(),
                request.getHost(),
                request.getPort(),
                request.getUsername(),
                request.getPassword(),
                request.getFromName(),
                request.getFrequencyLimit());
        return Result.success();
    }

    @PutMapping("/log")
    public Result<Void> updateLog(@RequestBody LogSettingsRequest request) {
        adminSettingsService.updateLog(request.getOperationDays(), request.getLoginDays());
        return Result.success();
    }

    /** 老用户配额批量调整（preview=true 仅返回受影响用户明细，不执行修改） */
    @PostMapping("/users/quota-batch")
    public Result<QuotaBatchResponse> quotaBatch(@Valid @RequestBody QuotaBatchRequest request) {
        return Result.success(adminSettingsService.quotaBatch(request));
    }
}
