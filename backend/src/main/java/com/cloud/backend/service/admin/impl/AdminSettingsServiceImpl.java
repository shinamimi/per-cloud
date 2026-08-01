package com.cloud.backend.service.admin.impl;

import com.cloud.backend.config.FileProperties;
import com.cloud.backend.config.JwtProperties;
import com.cloud.backend.dto.admin.AdminUserResponse;
import com.cloud.backend.dto.admin.QuotaBatchRequest;
import com.cloud.backend.dto.admin.QuotaBatchResponse;
import com.cloud.backend.entity.Setting;
import com.cloud.backend.entity.User;
import com.cloud.backend.enums.ErrorCode;
import com.cloud.backend.exception.BusinessException;
import com.cloud.backend.mapper.SettingMapper;
import com.cloud.backend.mapper.UserMapper;
import com.cloud.backend.service.admin.AdminSettingsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 系统设置服务实现 —— 所有系统级配置项集中管理（t_setting 表，key-value）。
 * 读取：t_setting 有值优先，否则回落配置文件默认值；写入：null = 删除配置行恢复默认。
 *
 * key 命名（docs/system-config-center.md §四）：
 * upload.* / storage.* / session.* / cache.* / system.* / file.* / share.* / mail.* / log.*
 */
@Service
public class AdminSettingsServiceImpl implements AdminSettingsService {

    /* ==================== 上传限制 ==================== */
    private static final String KEY_MAX_SIZE_USER = "upload.max-size-user";
    private static final String KEY_MAX_SIZE_VIP = "upload.max-size-vip";
    private static final String KEY_MAX_CONCURRENT_USER = "upload.max-concurrent-user";
    private static final String KEY_MAX_CONCURRENT_VIP = "upload.max-concurrent-vip";

    /* ==================== 存储限制 ==================== */
    private static final String KEY_DEFAULT_QUOTA_USER = "storage.default-quota-user";
    private static final String KEY_DEFAULT_QUOTA_VIP = "storage.default-quota-vip";

    /* ==================== 会话安全 ==================== */
    private static final String KEY_ACCESS_TOKEN_TTL = "session.access-token-ttl";
    private static final String KEY_CAPTCHA_TTL = "session.captcha-ttl";
    private static final String KEY_LOGIN_LOCK_THRESHOLD = "session.login-lock-threshold";
    private static final String KEY_LOGIN_LOCK_DURATION = "session.login-lock-duration";
    private static final String KEY_RESET_PASSWORD_TTL = "session.reset-password-ttl";

    /* ==================== 缓存策略 ==================== */
    private static final String KEY_CACHE_CAPTCHA = "cache.captcha";
    private static final String KEY_CACHE_LOGIN_ATTEMPT = "cache.login-attempt";
    private static final String KEY_CACHE_BLACKLIST = "cache.blacklist-token";
    private static final String KEY_CACHE_FILE_PREVIEW = "cache.file-preview";
    private static final String KEY_CACHE_DOWNLOAD_LINK = "cache.download-link";

    /* ==================== 系统功能 ==================== */
    private static final String KEY_SYSTEM_ALLOW_REGISTER = "system.allow-register";
    private static final String KEY_SYSTEM_ALLOW_GUEST_SHARE = "system.allow-guest-share";
    private static final String KEY_SYSTEM_ENABLE_MAIL_VERIFY = "system.enable-mail-verify";
    private static final String KEY_SYSTEM_ENABLE_CAPTCHA = "system.enable-captcha";
    private static final String KEY_SYSTEM_ENABLE_OPERATION_LOG = "system.enable-operation-log";

    /* ==================== 文件管理 ==================== */
    private static final String KEY_RECYCLE_BIN_DAYS = "file.recycle-bin-days";
    private static final String KEY_SHARE_DEFAULT_VALID_DAYS = "share.default-valid-days";
    private static final String KEY_SHARE_MAX_VALID_DAYS = "share.max-valid-days";
    private static final String KEY_SHARE_MAX_COUNT_PER_FILE = "share.max-count-per-file";
    private static final String KEY_SHARE_DEFAULT_REQUIRE_PASSWORD = "share.default-require-password";

    /* ==================== 邮件服务 ==================== */
    private static final String KEY_MAIL_ENABLED = "mail.enabled";
    private static final String KEY_MAIL_HOST = "mail.host";
    private static final String KEY_MAIL_PORT = "mail.port";
    private static final String KEY_MAIL_ENCRYPTION = "mail.encryption";
    private static final String KEY_MAIL_USERNAME = "mail.username";
    private static final String KEY_MAIL_PASSWORD = "mail.password";
    private static final String KEY_MAIL_FROM_NAME = "mail.from-name";
    private static final String KEY_MAIL_FROM = "mail.from";
    private static final String KEY_MAIL_FREQUENCY_LIMIT = "mail.frequency-limit";

    /* ==================== 日志 ==================== */
    private static final String KEY_LOG_OPERATION_DAYS = "log.operation-days";
    private static final String KEY_LOG_LOGIN_DAYS = "log.login-days";

    /* ==================== 团队默认值 ==================== */
    private static final String KEY_TEAM_MAX_PER_USER = "team.max-per-user";
    private static final String KEY_TEAM_DEFAULT_QUOTA = "team.default-quota";
    private static final String KEY_TEAM_RECYCLE_BIN_DAYS = "team.recycle-bin-days";
    private static final String KEY_TEAM_MAX_MEMBERS = "team.max-members";

    /** SMTP 密码脱敏占位符（GET 返回时替代真实值；PUT 提交该值表示不修改密码） */
    private static final String PASSWORD_MASK = "********";

    private final SettingMapper settingMapper;
    private final FileProperties fileProperties;
    private final JwtProperties jwtProperties;
    private final UserMapper userMapper;

    /** yml 配额默认值（quota.default-user/vip）作为 t_setting 缺失时的兜底 */
    @Value("${quota.default-user:5368709120}")
    private long ymlDefaultQuotaUser;

    @Value("${quota.default-vip:107374182400}")
    private long ymlDefaultQuotaVip;

    /** yml spring.mail.from 兜底（发件人邮箱地址） */
    @Value("${spring.mail.from:}")
    private String ymlMailFrom;

    public AdminSettingsServiceImpl(SettingMapper settingMapper, FileProperties fileProperties,
                                    JwtProperties jwtProperties, UserMapper userMapper) {
        this.settingMapper = settingMapper;
        this.fileProperties = fileProperties;
        this.jwtProperties = jwtProperties;
        this.userMapper = userMapper;
    }

    /* ==================== 上传限制 ==================== */

    @Override
    public long getMaxSizeUser() {
        return readLong(KEY_MAX_SIZE_USER, fileProperties.getMaxSizeUser());
    }

    @Override
    public long getMaxSizeVip() {
        return readLong(KEY_MAX_SIZE_VIP, fileProperties.getMaxSizeVip());
    }

    @Override
    public int getMaxConcurrentUser() {
        return readInt(KEY_MAX_CONCURRENT_USER, fileProperties.getMaxConcurrentUser());
    }

    @Override
    public int getMaxConcurrentVip() {
        return readInt(KEY_MAX_CONCURRENT_VIP, fileProperties.getMaxConcurrentVip());
    }

    @Override
    public void updateUploadLimits(Long maxSizeUser, Long maxSizeVip, Integer maxConcurrentUser, Integer maxConcurrentVip) {
        upsertOrReset(KEY_MAX_SIZE_USER, maxSizeUser, "普通用户单文件大小上限（字节）");
        upsertOrReset(KEY_MAX_SIZE_VIP, maxSizeVip, "VIP 用户单文件大小上限（字节）");
        upsertOrReset(KEY_MAX_CONCURRENT_USER, maxConcurrentUser, "普通用户上传并发任务数上限");
        upsertOrReset(KEY_MAX_CONCURRENT_VIP, maxConcurrentVip, "VIP 用户上传并发任务数上限");
    }

    /* ==================== 存储限制 ==================== */

    @Override
    public long getDefaultQuotaUser() {
        return readLong(KEY_DEFAULT_QUOTA_USER, ymlDefaultQuotaUser);
    }

    @Override
    public long getDefaultQuotaVip() {
        return readLong(KEY_DEFAULT_QUOTA_VIP, ymlDefaultQuotaVip);
    }

    @Override
    public void updateStorage(Long defaultQuotaUser, Long defaultQuotaVip) {
        upsertOrReset(KEY_DEFAULT_QUOTA_USER, defaultQuotaUser, "普通用户新注册默认配额（字节）");
        upsertOrReset(KEY_DEFAULT_QUOTA_VIP, defaultQuotaVip, "VIP 用户新注册默认配额（字节）");
    }

    /* ==================== 会话安全 ==================== */

    @Override
    public long getAccessTokenTtlMs() {
        long minutes = readLong(KEY_ACCESS_TOKEN_TTL, jwtProperties.getExpiration() / 60000);
        return minutes * 60 * 1000;
    }

    @Override
    public long getCaptchaTtlSeconds() {
        return readLong(KEY_CAPTCHA_TTL, 300);
    }

    @Override
    public int getLoginLockThreshold() {
        return readInt(KEY_LOGIN_LOCK_THRESHOLD, 5);
    }

    @Override
    public long getLoginLockDurationMinutes() {
        return readLong(KEY_LOGIN_LOCK_DURATION, 30);
    }

    @Override
    public long getResetPasswordTtlMinutes() {
        return readLong(KEY_RESET_PASSWORD_TTL, 30);
    }

    @Override
    public void updateSession(Long accessTokenTtlMinutes, Long captchaTtlSeconds, Integer loginLockThreshold,
                              Long loginLockDurationMinutes, Long resetPasswordTtlMinutes) {
        upsertOrReset(KEY_ACCESS_TOKEN_TTL, accessTokenTtlMinutes, "Access Token 有效期（分钟）");
        upsertOrReset(KEY_CAPTCHA_TTL, captchaTtlSeconds, "验证码有效期（秒）");
        upsertOrReset(KEY_LOGIN_LOCK_THRESHOLD, loginLockThreshold, "登录失败锁定次数");
        upsertOrReset(KEY_LOGIN_LOCK_DURATION, loginLockDurationMinutes, "登录锁定时间（分钟）");
        upsertOrReset(KEY_RESET_PASSWORD_TTL, resetPasswordTtlMinutes, "密码重置链接有效期（分钟）");
    }

    /* ==================== 缓存策略 ==================== */

    @Override
    public long getCacheCaptchaTtlSeconds() {
        return readLong(KEY_CACHE_CAPTCHA, 300);
    }

    @Override
    public long getLoginAttemptTtlSeconds() {
        return readLong(KEY_CACHE_LOGIN_ATTEMPT, 30 * 60);
    }

    @Override
    public long getBlacklistTokenTtlSeconds() {
        return readLong(KEY_CACHE_BLACKLIST, getAccessTokenTtlMs() / 1000);
    }

    @Override
    public long getFilePreviewTtlSeconds() {
        return readLong(KEY_CACHE_FILE_PREVIEW, 0);
    }

    @Override
    public int getDownloadLinkTtlMinutes() {
        return readInt(KEY_CACHE_DOWNLOAD_LINK, 10);
    }

    @Override
    public void updateCache(Long captcha, Long loginAttempt, Long blacklist, Long filePreview, Long downloadLinkMinutes) {
        upsertOrReset(KEY_CACHE_CAPTCHA, captcha, "验证码缓存 TTL（秒，预留）");
        upsertOrReset(KEY_CACHE_LOGIN_ATTEMPT, loginAttempt, "登录失败计数 TTL（秒）");
        upsertOrReset(KEY_CACHE_BLACKLIST, blacklist, "黑名单 Token TTL（秒）");
        upsertOrReset(KEY_CACHE_FILE_PREVIEW, filePreview, "文件预览缓存 TTL（秒，预留）");
        upsertOrReset(KEY_CACHE_DOWNLOAD_LINK, downloadLinkMinutes, "下载链接 TTL（分钟）");
    }

    /* ==================== 系统功能 ==================== */

    @Override
    public boolean isAllowRegister() {
        return readBoolean(KEY_SYSTEM_ALLOW_REGISTER, true);
    }

    @Override
    public boolean isAllowGuestShare() {
        return readBoolean(KEY_SYSTEM_ALLOW_GUEST_SHARE, false);
    }

    @Override
    public boolean isMailVerifyEnabled() {
        return readBoolean(KEY_SYSTEM_ENABLE_MAIL_VERIFY, true);
    }

    @Override
    public boolean isCaptchaEnabled() {
        return readBoolean(KEY_SYSTEM_ENABLE_CAPTCHA, false);
    }

    @Override
    public boolean isOperationLogEnabled() {
        return readBoolean(KEY_SYSTEM_ENABLE_OPERATION_LOG, true);
    }

    @Override
    public void updateSystem(Boolean allowRegister, Boolean allowGuestShare, Boolean enableMailVerify,
                             Boolean enableCaptcha, Boolean enableOperationLog) {
        upsertOrReset(KEY_SYSTEM_ALLOW_REGISTER, allowRegister, "是否开放注册");
        upsertOrReset(KEY_SYSTEM_ALLOW_GUEST_SHARE, allowGuestShare, "是否允许游客分享");
        upsertOrReset(KEY_SYSTEM_ENABLE_MAIL_VERIFY, enableMailVerify, "是否开启邮件验证");
        upsertOrReset(KEY_SYSTEM_ENABLE_CAPTCHA, enableCaptcha, "是否开启登录验证码");
        upsertOrReset(KEY_SYSTEM_ENABLE_OPERATION_LOG, enableOperationLog, "是否开启操作日志");
    }

    /* ==================== 文件管理 ==================== */

    @Override
    public int getRecycleBinDays() {
        return readInt(KEY_RECYCLE_BIN_DAYS, fileProperties.getRecycleDays());
    }

    @Override
    public int getShareDefaultValidDays() {
        return readInt(KEY_SHARE_DEFAULT_VALID_DAYS, 7);
    }

    @Override
    public int getShareMaxValidDays() {
        return readInt(KEY_SHARE_MAX_VALID_DAYS, 30);
    }

    @Override
    public int getShareMaxCountPerFile() {
        return readInt(KEY_SHARE_MAX_COUNT_PER_FILE, 0);
    }

    @Override
    public boolean isShareDefaultRequirePassword() {
        return readBoolean(KEY_SHARE_DEFAULT_REQUIRE_PASSWORD, false);
    }

    @Override
    public void updateFile(Integer recycleBinDays, Integer shareDefaultValidDays, Integer shareMaxValidDays,
                           Integer shareMaxCountPerFile, Boolean shareDefaultRequirePassword) {
        upsertOrReset(KEY_RECYCLE_BIN_DAYS, recycleBinDays, "回收站保留天数");
        upsertOrReset(KEY_SHARE_DEFAULT_VALID_DAYS, shareDefaultValidDays, "分享默认有效期（天）");
        upsertOrReset(KEY_SHARE_MAX_VALID_DAYS, shareMaxValidDays, "分享最长有效期（天）");
        upsertOrReset(KEY_SHARE_MAX_COUNT_PER_FILE, shareMaxCountPerFile, "同一文件最大分享次数");
        upsertOrReset(KEY_SHARE_DEFAULT_REQUIRE_PASSWORD, shareDefaultRequirePassword, "分享默认是否要求提取码");
    }

    /* ==================== 邮件服务 ==================== */

    @Override
    public boolean isMailEnabled() {
        return readBoolean(KEY_MAIL_ENABLED, true);
    }

    @Override
    public String getMailHost() {
        return readString(KEY_MAIL_HOST, null);
    }

    @Override
    public int getMailPort() {
        return readInt(KEY_MAIL_PORT, 587);
    }

    @Override
    public String getMailEncryption() {
        return readString(KEY_MAIL_ENCRYPTION, "STARTTLS");
    }

    @Override
    public String getMailUsername() {
        return readString(KEY_MAIL_USERNAME, null);
    }

    @Override
    public String getMailPassword() {
        return readString(KEY_MAIL_PASSWORD, null);
    }

    @Override
    public String getMailFromName() {
        return readString(KEY_MAIL_FROM_NAME, null);
    }

    @Override
    public String getMailFrom() {
        String from = readString(KEY_MAIL_FROM, null);
        if (from != null && !from.isBlank()) {
            return from;
        }
        return ymlMailFrom;
    }

    @Override
    public long getMailFrequencyLimitSeconds() {
        return readLong(KEY_MAIL_FREQUENCY_LIMIT, 60);
    }

    @Override
    public void updateMail(Boolean enabled, String host, Integer port, String username, String password,
                           String encryption, String from, String fromName, Long frequencyLimit) {
        upsertOrReset(KEY_MAIL_ENABLED, enabled, "SMTP 开关");
        upsertOrReset(KEY_MAIL_HOST, host, "SMTP 服务器地址");
        upsertOrReset(KEY_MAIL_PORT, port, "SMTP 端口");
        upsertOrReset(KEY_MAIL_ENCRYPTION, encryption, "SMTP 加密方式");
        upsertOrReset(KEY_MAIL_USERNAME, username, "SMTP 登录名");
        // 密码为空或脱敏占位符 = 不修改密码（保留原值）
        if (password != null && !password.isBlank() && !PASSWORD_MASK.equals(password)) {
            upsertOrReset(KEY_MAIL_PASSWORD, password, "SMTP 密码");
        }
        upsertOrReset(KEY_MAIL_FROM, from, "发件人邮箱");
        upsertOrReset(KEY_MAIL_FROM_NAME, fromName, "发件人显示名");
        upsertOrReset(KEY_MAIL_FREQUENCY_LIMIT, frequencyLimit, "邮件频率限制（秒）");
    }

    /* ==================== 日志 ==================== */

    @Override
    public int getOperationLogDays() {
        return readInt(KEY_LOG_OPERATION_DAYS, 30);
    }

    @Override
    public int getLoginLogDays() {
        return readInt(KEY_LOG_LOGIN_DAYS, 30);
    }

    @Override
    public void updateLog(Integer operationDays, Integer loginDays) {
        upsertOrReset(KEY_LOG_OPERATION_DAYS, operationDays, "操作日志保存天数");
        upsertOrReset(KEY_LOG_LOGIN_DAYS, loginDays, "登录日志保存天数");
    }

    /* ==================== 团队默认值 ==================== */

    @Override
    public int getTeamMaxPerUser() {
        return readInt(KEY_TEAM_MAX_PER_USER, 10);
    }

    @Override
    public long getTeamDefaultQuota() {
        return readLong(KEY_TEAM_DEFAULT_QUOTA, 10737418240L);
    }

    @Override
    public int getTeamRecycleBinDays() {
        return readInt(KEY_TEAM_RECYCLE_BIN_DAYS, 30);
    }

    @Override
    public int getTeamMaxMembers() {
        return readInt(KEY_TEAM_MAX_MEMBERS, 50);
    }

    @Override
    public void updateTeam(Integer maxPerUser, Long defaultQuota, Integer recycleBinDays, Integer maxMembers) {
        upsertOrReset(KEY_TEAM_MAX_PER_USER, maxPerUser, "每人团队数上限");
        upsertOrReset(KEY_TEAM_DEFAULT_QUOTA, defaultQuota, "新团队默认配额（字节）");
        upsertOrReset(KEY_TEAM_RECYCLE_BIN_DAYS, recycleBinDays, "团队回收站保留天数");
        upsertOrReset(KEY_TEAM_MAX_MEMBERS, maxMembers, "团队最大成员数");
    }

    /* ==================== 老用户配额批量调整 ==================== */

    @Override
    public QuotaBatchResponse quotaBatch(QuotaBatchRequest request) {
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "开始日期不能晚于结束日期");
        }
        if (request.getTargetQuotaUser() < 0 || request.getTargetQuotaVip() < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "目标配额不能为负数");
        }

        List<User> users = userMapper.findByQuotaFilter(request.getStartDate(), request.getEndDate(),
                request.getRole(), request.getStatus());

        QuotaBatchResponse response = new QuotaBatchResponse();
        response.setCount(users.size());

        if (request.isPreview()) {
            List<AdminUserResponse> preview = new ArrayList<>(users.size());
            for (User user : users) {
                // 预览中的"总配额"按目标配额 + 管理加成 + 奖励配额展示（内联计算，避免依赖 UserService 形成循环依赖）
                long target = Boolean.TRUE.equals(user.getIsVip())
                        ? request.getTargetQuotaVip() : request.getTargetQuotaUser();
                long adminBonus = user.getAdminBonusQuota() != null ? user.getAdminBonusQuota() : 0;
                long reward = user.getRewardQuota() != null ? user.getRewardQuota() : 0;
                preview.add(new AdminUserResponse(user.getId(), user.getUsername(), user.getEmail(),
                        user.getNickname(), user.getAvatar(), user.getRole(), user.getQuota(),
                        target + adminBonus + reward, user.getAdminBonusQuota(), user.getRewardQuota(),
                        user.getUsedSpace(), user.getIsVip(), user.getStatus(), user.getCreatedAt()));
            }
            response.setUsers(preview);
            return response;
        }

        // 执行：按 VIP 拆分为两组批量更新（只改 quota 基础字段，不触碰 adminBonusQuota/rewardQuota，幂等）
        List<Long> userIds = new ArrayList<>();
        List<Long> vipIds = new ArrayList<>();
        for (User user : users) {
            if (Boolean.TRUE.equals(user.getIsVip())) {
                vipIds.add(user.getId());
            } else {
                userIds.add(user.getId());
            }
        }
        if (!userIds.isEmpty()) {
            userMapper.batchUpdateQuota(userIds, request.getTargetQuotaUser());
        }
        if (!vipIds.isEmpty()) {
            userMapper.batchUpdateQuota(vipIds, request.getTargetQuotaVip());
        }
        return response;
    }

    /* ==================== 通用读写 ==================== */

    private void upsertOrReset(String key, Object value, String description) {
        if (value == null) {
            settingMapper.deleteByKey(key);
            return;
        }
        Setting setting = new Setting();
        setting.setSettingKey(key);
        setting.setSettingValue(String.valueOf(value));
        setting.setDescription(description);
        settingMapper.upsert(setting);
    }

    private long readLong(String key, long defaultValue) {
        Setting setting = settingMapper.findByKey(key);
        if (setting == null || setting.getSettingValue() == null || setting.getSettingValue().isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(setting.getSettingValue());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private int readInt(String key, int defaultValue) {
        Setting setting = settingMapper.findByKey(key);
        if (setting == null || setting.getSettingValue() == null || setting.getSettingValue().isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(setting.getSettingValue());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private boolean readBoolean(String key, boolean defaultValue) {
        Setting setting = settingMapper.findByKey(key);
        if (setting == null || setting.getSettingValue() == null || setting.getSettingValue().isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(setting.getSettingValue());
    }

    private String readString(String key, String defaultValue) {
        Setting setting = settingMapper.findByKey(key);
        if (setting == null || setting.getSettingValue() == null || setting.getSettingValue().isBlank()) {
            return defaultValue;
        }
        return setting.getSettingValue();
    }
}
