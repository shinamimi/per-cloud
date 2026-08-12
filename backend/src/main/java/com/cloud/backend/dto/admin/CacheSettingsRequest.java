package com.cloud.backend.dto.admin;

import lombok.Data;

/**
 * 缓存策略配置更新请求（null 字段恢复配置默认值）。
 *
 * 修改指引：
 * - 【统一】修改单位             → captcha/loginAttempt/blacklist/filePreview 单位为秒，downloadLinkMinutes 单位为分钟（presigned URL 有效期）；
 *                          单位混用易错；改后需同步配置读写逻辑与前端表单的单位标注
 * - 【统一】修改 null 语义         → null 字段恢复配置默认值；改后需同步 service 的空值判断
 * - 【习惯】修改缓存 TTL 字段     → 对应 t_setting/配置类的缓存策略，改动影响验证码、登录失败计数、黑名单 Token、预览、下载链接的过期时间
 * - 【统一】新增缓存项            → 新增字段并同步缓存读写逻辑与前端，否则该配置不生效；改后需同步缓存读写逻辑与前端
 */
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
