package com.cloud.backend.service.admin;

import com.cloud.backend.dto.admin.QuotaBatchRequest;
import com.cloud.backend.dto.admin.QuotaBatchResponse;
import com.cloud.backend.dto.admin.AdminUserResponse;

/**
 * 系统设置服务 —— 集中管理所有系统级配置项（t_setting 表）。
 * t_setting 表有记录时优先，否则使用配置文件默认值。
 *
 * 分组：
 * - 上传限制（upload.*）/ 存储限制（storage.*）/ 会话安全（session.*）
 * - 缓存策略（cache.*）/ 系统功能（system.*）/ 文件管理（file.*、share.*）
 * - 邮件服务（mail.*）/ 日志（log.*）
 *
 * 修改指引：
 * - 【习惯】想改"读取优先级（t_setting 有值优先，否则回落配置文件/yml 默认值）" → 各 getter 对应
 *   AdminSettingsServiceImpl.readLong/readInt/readBoolean/readString 与 upsertOrReset()（value 为 null
 *   删除配置行恢复默认）；改动影响全部系统级配置的生效来源
 * - 【习惯】想改"某个 getter 的 key/默认值/单位换算" → 各 getter 与其对应 KEY_* 常量（如 getMaxSizeUser 默认值取
 *   fileProperties.getMaxSizeUser()、getAccessTokenTtlMs 分钟→毫秒换算、getMailFrom 回落 yml spring.mail.from）；
 *   改动影响读取生效值，须保持 key 命名一致
 * - 【习惯】想改"某个 update* 的入参（null 恢复默认）" → updateUploadLimits()/updateStorage()/updateSession()/updateCache()/
 *   updateSystem()/updateFile()/updateMail()/updateLog()/updateTeam() 均调用 upsertOrReset()；
 *   改动影响对应配置组的重置语义与写库
 * - 【习惯】想改"SMTP 密码脱敏占位" → updateMail() 中 PASSWORD_MASK（"********"）判断，password 为空或占位符不更新密码；
 *   改动影响管理端回显与密码更新语义
 * - 【习惯】想改"老用户配额批量调整" → quotaBatch()（isPreview 内联计算总配额展示，执行只改基础 quota 字段、
 *   不触碰 adminBonusQuota/rewardQuota、幂等）；改动影响批量配额生效范围与三来源配额模型
 * - 【习惯】本接口 getter 被 AuthService/UploadService/TeamService/CaptchaService/LoginAttemptService/EmailService/
 *   OperationLogService 等广泛消费，改动语义须评估全部调用方
 * - 【习惯】新增方法 → 需同步实现类 AdminSettingsServiceImpl 与 AdminSettingsController
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

    /** 验证码缓存 TTL（秒），默认 300（本期预留，未消费） */
    long getCacheCaptchaTtlSeconds();

    /** 登录失败计数 TTL（秒），默认 1800 */
    long getLoginAttemptTtlSeconds();

    /** 黑名单 Token TTL（秒），默认取 Access Token 有效期兜底 */
    long getBlacklistTokenTtlSeconds();

    /** 文件预览缓存 TTL（秒），默认 0（本期预留，未消费） */
    long getFilePreviewTtlSeconds();

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

    /** 分享默认有效期（天），默认 7 */
    int getShareDefaultValidDays();

    /** 分享最长有效期（天），默认 30 */
    int getShareMaxValidDays();

    /** 同一文件最大分享次数，默认 0（不限） */
    int getShareMaxCountPerFile();

    /** 分享默认是否要求提取码，默认 false */
    boolean isShareDefaultRequirePassword();

    /** 分享默认下载策略：ALLOW=允许下载（默认）/ DENY=禁止下载 */
    String getShareDefaultDownloadPolicy();

    /** 更新文件管理配置（null 字段恢复配置默认值） */
    void updateFile(Integer recycleBinDays, Integer shareDefaultValidDays, Integer shareMaxValidDays,
                    Integer shareMaxCountPerFile, Boolean shareDefaultRequirePassword, String shareDefaultDownloadPolicy);

    /* ==================== 邮件服务（ADMIN） ==================== */

    /** SMTP 是否启用，默认 true */
    boolean isMailEnabled();

    /** SMTP 主机（null = 未在 t_setting 配置，用 yml） */
    String getMailHost();

    int getMailPort();

    /** SMTP 加密方式：STARTTLS / SSL / NONE（默认 STARTTLS） */
    String getMailEncryption();

    String getMailUsername();

    String getMailPassword();

    /** 发件人显示名 */
    String getMailFromName();

    /** 发件人邮箱地址（null = 未在 t_setting 配置，用 yml spring.mail.from） */
    String getMailFrom();

    /** 邮件频率限制（秒），默认 60 */
    long getMailFrequencyLimitSeconds();

    /** 更新邮件服务配置（null 字段恢复配置默认值；password 为空或脱敏占位符时不更新密码） */
    void updateMail(Boolean enabled, String host, Integer port, String username, String password,
                    String encryption, String from, String fromName, Long frequencyLimit);

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

    /* ==================== 团队默认值 ==================== */

    /** 每人团队数上限，默认 10 */
    int getTeamMaxPerUser();

    /** 新团队默认配额（字节），默认 10GB */
    long getTeamDefaultQuota();

    /** 团队回收站保留天数，默认 30 */
    int getTeamRecycleBinDays();

    /** 团队最大成员数，默认 50 */
    int getTeamMaxMembers();

    /** 更新团队配置（null 字段恢复配置默认值） */
    void updateTeam(Integer maxPerUser, Long defaultQuota, Integer recycleBinDays, Integer maxMembers);
}
