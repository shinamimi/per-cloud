package com.cloud.backend.service.admin;

import com.cloud.backend.dto.admin.QuotaBatchRequest;
import com.cloud.backend.dto.admin.QuotaBatchResponse;
import com.cloud.backend.dto.admin.AdminUserResponse;

/**
 * 系统设置服务 —— 集中管理所有系统级配置项（t_setting 表）。
 * t_setting 表有记录时优先，否则使用配置文件默认值。
 *
 * 分组（docs/system-config-center.md）：
 * - 上传限制（upload.*）/ 存储限制（storage.*）/ 会话安全（session.*）
 * - 缓存策略（cache.*）/ 系统功能（system.*）/ 文件管理（file.*、share.*）
 * - 邮件服务（mail.*）/ 日志（log.*）
 */
public interface AdminSettingsService {

    /* ==================== 上传限制 ==================== */

    long getMaxSizeUser();

    long getMaxSizeVip();

    int getMaxConcurrentUser();

    int getMaxConcurrentVip();

    /** 更新上传限制（null 字段恢复配置默认值） */
    void updateUploadLimits(Long maxSizeUser, Long maxSizeVip, Integer maxConcurrentUser, Integer maxConcurrentVip);

    /* ==================== 存储限制 ==================== */

    /** 新注册普通用户默认配额（字节） */
    long getDefaultQuotaUser();

    /** 新注册 VIP 用户默认配额（字节） */
    long getDefaultQuotaVip();

    /** 更新新用户默认配额（null 字段恢复配置默认值） */
    void updateStorage(Long defaultQuotaUser, Long defaultQuotaVip);

    /* ==================== 会话安全 ==================== */

    /** Access Token 有效期（毫秒），默认 24 小时 */
    long getAccessTokenTtlMs();

    /** 验证码有效期（秒），默认 300 */
    long getCaptchaTtlSeconds();

    /** 登录失败锁定次数，默认 5 */
    int getLoginLockThreshold();

    /** 登录锁定时间（分钟），默认 30 */
    long getLoginLockDurationMinutes();

    /** 密码重置链接有效期（分钟），预留 */
    long getResetPasswordTtlMinutes();

    /** 更新会话安全配置（null 字段恢复配置默认值） */
    void updateSession(Long accessTokenTtlMinutes, Long captchaTtlSeconds, Integer loginLockThreshold,
                       Long loginLockDurationMinutes, Long resetPasswordTtlMinutes);

    /* ==================== 缓存策略 ==================== */

    /** 登录失败计数 TTL（秒），默认 1800 */
    long getLoginAttemptTtlSeconds();

    /** 黑名单 Token TTL（秒），默认取 Access Token 有效期兜底 */
    long getBlacklistTokenTtlSeconds();

    /** 下载链接 TTL（分钟，presigned URL 有效期），默认 10 */
    int getDownloadLinkTtlMinutes();

    /** 更新缓存策略配置（null 字段恢复配置默认值） */
    void updateCache(Long captcha, Long loginAttempt, Long blacklist, Long filePreview, Long downloadLinkMinutes);

    /* ==================== 系统功能（ADMIN） ==================== */

    /** 是否开放注册，默认 true */
    boolean isAllowRegister();

    /** 是否允许游客分享，默认 false（本期预留） */
    boolean isAllowGuestShare();

    /** 是否开启邮件验证（注册/重置密码验证码），默认 true */
    boolean isMailVerifyEnabled();

    /** 是否开启登录验证码，默认 false */
    boolean isCaptchaEnabled();

    /** 是否开启操作日志，默认 true */
    boolean isOperationLogEnabled();

    /** 更新系统功能开关（null 字段恢复配置默认值） */
    void updateSystem(Boolean allowRegister, Boolean allowGuestShare, Boolean enableMailVerify,
                      Boolean enableCaptcha, Boolean enableOperationLog);

    /* ==================== 文件管理 ==================== */

    /** 回收站保留天数，默认 30 */
    int getRecycleBinDays();

    /** 更新文件管理配置（null 字段恢复配置默认值） */
    void updateFile(Integer recycleBinDays, Integer shareDefaultValidDays, Integer shareMaxValidDays,
                    Integer shareMaxCountPerFile, Boolean shareDefaultRequirePassword);

    /* ==================== 邮件服务（ADMIN） ==================== */

    /** SMTP 是否启用，默认 true */
    boolean isMailEnabled();

    /** SMTP 主机（null = 未在 t_setting 配置，用 yml） */
    String getMailHost();

    int getMailPort();

    String getMailUsername();

    String getMailPassword();

    /** 发件人显示名 */
    String getMailFromName();

    /** 邮件频率限制（秒），默认 60 */
    long getMailFrequencyLimitSeconds();

    /** 更新邮件服务配置（null 字段恢复配置默认值；password 为空或脱敏占位符时不更新密码） */
    void updateMail(Boolean enabled, String host, Integer port, String username, String password,
                    String fromName, Long frequencyLimit);

    /* ==================== 日志 ==================== */

    /** 操作日志保存天数，默认 30 */
    int getOperationLogDays();

    /** 登录日志保存天数，默认 30 */
    int getLoginLogDays();

    /** 更新日志配置（null 字段恢复配置默认值） */
    void updateLog(Integer operationDays, Integer loginDays);

    /* ==================== 老用户配额批量调整 ==================== */

    /** 按日期范围 + 角色/状态过滤查询用户（quota-batch 预览与执行共用） */
    QuotaBatchResponse quotaBatch(QuotaBatchRequest request);
}
