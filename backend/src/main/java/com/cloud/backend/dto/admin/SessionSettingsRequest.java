package com.cloud.backend.dto.admin;

import lombok.Data;

@Data
public class SessionSettingsRequest {

    /** Access Token 有效期（分钟） */
    private Long accessTokenTtlMinutes;

    /** 验证码有效期（秒） */
    private Long captchaTtlSeconds;

    /** 登录失败锁定次数 */
    private Integer loginLockThreshold;

    /** 登录锁定时间（分钟） */
    private Long loginLockDurationMinutes;

    /** 密码重置链接有效期（分钟，预留） */
    private Long resetPasswordTtlMinutes;
}
