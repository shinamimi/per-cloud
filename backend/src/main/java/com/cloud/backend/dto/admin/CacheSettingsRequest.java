package com.cloud.backend.dto.admin;

import lombok.Data;

@Data
public class CacheSettingsRequest {

    /** 验证码缓存 TTL（秒，预留） */
    private Long captcha;

    /** 登录失败计数 TTL（秒） */
    private Long loginAttempt;

    /** 黑名单 Token TTL（秒） */
    private Long blacklist;

    /** 文件预览缓存 TTL（秒，预留） */
    private Long filePreview;

    /** 下载链接 TTL（分钟，presigned URL 有效期） */
    private Long downloadLinkMinutes;
}
