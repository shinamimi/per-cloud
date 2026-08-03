package com.cloud.backend.controller.admin;

import com.cloud.backend.dto.Page;
import com.cloud.backend.dto.PageRequest;
import com.cloud.backend.dto.Result;
import com.cloud.backend.dto.admin.*;
import com.cloud.backend.enums.OperationType;
import com.cloud.backend.service.admin.AdminSettingsService;
import com.cloud.backend.service.system.OperationLogService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统配置中心（管理端）。
 * 权限：/api/admin/settings/** 默认 OPERATOR+；/system 与 /mail 两个敏感分组仅 ADMIN+（SecurityConfig 细化）。
 * 所有分组独立保存；null 字段表示恢复配置文件默认值（删除 t_setting 配置行）。
 */
@RestController
@RequestMapping("/api/admin/settings")
public class AdminSettingsController {

    /** SMTP 密码脱敏占位符（与 Service 层保持一致） */
    private static final String PASSWORD_MASK = "********";

    private final AdminSettingsService adminSettingsService;
    private final OperationLogService operationLogService;

    public AdminSettingsController(AdminSettingsService adminSettingsService, OperationLogService operationLogService) {
        this.adminSettingsService = adminSettingsService;
        this.operationLogService = operationLogService;
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
                "captcha", adminSettingsService.getCacheCaptchaTtlSeconds(),
                "loginAttempt", adminSettingsService.getLoginAttemptTtlSeconds(),
                "blacklist", adminSettingsService.getBlacklistTokenTtlSeconds(),
                "filePreview", adminSettingsService.getFilePreviewTtlSeconds(),
                "downloadLinkMinutes", adminSettingsService.getDownloadLinkTtlMinutes()));
        settings.put("system", Map.of(
                "allowRegister", adminSettingsService.isAllowRegister(),
                "allowGuestShare", adminSettingsService.isAllowGuestShare(),
                "enableMailVerify", adminSettingsService.isMailVerifyEnabled(),
                "enableCaptcha", adminSettingsService.isCaptchaEnabled(),
                "enableOperationLog", adminSettingsService.isOperationLogEnabled()));
        settings.put("file", Map.of(
                "recycleBinDays", adminSettingsService.getRecycleBinDays(),
                "shareDefaultValidDays", adminSettingsService.getShareDefaultValidDays(),
                "shareMaxValidDays", adminSettingsService.getShareMaxValidDays(),
                "shareMaxCountPerFile", adminSettingsService.getShareMaxCountPerFile(),
                "shareDefaultRequirePassword", adminSettingsService.isShareDefaultRequirePassword(),
                "shareDefaultDownloadPolicy", adminSettingsService.getShareDefaultDownloadPolicy()));
        settings.put("mail", mailGroup());
        settings.put("log", Map.of(
                "operationDays", adminSettingsService.getOperationLogDays(),
                "loginDays", adminSettingsService.getLoginLogDays()));
        settings.put("team", Map.of(
                "maxPerUser", adminSettingsService.getTeamMaxPerUser(),
                "defaultQuota", adminSettingsService.getTeamDefaultQuota(),
                "recycleBinDays", adminSettingsService.getTeamRecycleBinDays(),
                "maxMembers", adminSettingsService.getTeamMaxMembers()));
        return Result.success(settings);
    }

    /** mail 分组使用 HashMap：SMTP 未配置时 host/username 等为 null，Map.of 不允许 null 值 */
    private Map<String, Object> mailGroup() {
        Map<String, Object> mail = new HashMap<>();
        mail.put("enabled", adminSettingsService.isMailEnabled());
        mail.put("host", adminSettingsService.getMailHost());
        mail.put("port", adminSettingsService.getMailPort());
        mail.put("encryption", adminSettingsService.getMailEncryption());
        mail.put("username", adminSettingsService.getMailUsername());
        mail.put("password", adminSettingsService.getMailPassword() == null ? null : PASSWORD_MASK);
        mail.put("from", adminSettingsService.getMailFrom());
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
                request.getShareDefaultRequirePassword(),
                request.getShareDefaultDownloadPolicy());
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
                request.getEncryption(),
                request.getFrom(),
                request.getFromName(),
                request.getFrequencyLimit());
        return Result.success();
    }

    @PutMapping("/log")
    public Result<Void> updateLog(@RequestBody LogSettingsRequest request) {
        adminSettingsService.updateLog(request.getOperationDays(), request.getLoginDays());
        return Result.success();
    }

    @PutMapping("/team")
    public Result<Void> updateTeam(@RequestBody TeamSettingsRequest request) {
        adminSettingsService.updateTeam(
                request.getMaxPerUser(),
                request.getDefaultQuota(),
                request.getRecycleBinDays(),
                request.getMaxMembers());
        return Result.success();
    }

    /** 老用户配额批量调整（preview=true 仅返回受影响用户明细，不执行修改） */
    @PostMapping("/users/quota-batch")
    public Result<QuotaBatchResponse> quotaBatch(@Valid @RequestBody QuotaBatchRequest request) {
        return Result.success(adminSettingsService.quotaBatch(request));
    }

    /**
     * 日志分页查询（审计）。
     * operation：可选，如 LOGIN 表示登录日志；不传返回全部操作日志。
     * 记录按创建时间倒序，join 用户表带出用户名。
     */
    @GetMapping("/logs")
    public Result<Page<LogItem>> queryLogs(@RequestParam(required = false) OperationType operation,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        LogFilterRequest filter = new LogFilterRequest();
        filter.setOperation(operation);
        return Result.success(operationLogService.listByFilterPaged(filter, new PageRequest(page, size)));
    }
}
