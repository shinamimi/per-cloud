package com.cloud.backend.dto.admin;

import lombok.Data;

/**
 * 会话安全配置更新请求（null 字段恢复配置默认值）。
 *
 * 修改指引：
 * - 【习惯】修改单位             → accessTokenTtlMinutes/loginLockDurationMinutes/resetPasswordTtlMinutes 单位为分钟，
 *                           captchaTtlSeconds 单位为秒；单位混用易错，改动需同步配置读写逻辑与前端表单的单位标注
 * - 【习惯】修改 loginLockThreshold → Integer 登录失败锁定次数；改动影响登录锁定触发条件，需同步登录失败计数逻辑
 * - 【习惯】修改 null 语义         → null 字段恢复配置默认值；改动需同步 service 的空值判断，否则会影响未传字段
 * - 【习惯】修改会话安全项        → 影响 Access Token 过期、验证码有效期、登录锁定、密码重置链接有效期的行为
 */
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
