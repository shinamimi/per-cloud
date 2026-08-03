package com.cloud.backend.dto.share;

import lombok.Data;

/**
 * 创建分享请求 —— POST /api/shares。
 * validType: PERMANENT=永久 / DAYS=按天数（validDays 生效）
 */
@Data
public class ShareCreateRequest {

    /** 被分享文件/文件夹 id（仅个人空间） */
    private Long fileId;

    /** PERMANENT=永久 / DAYS=按天数 */
    private String validType;

    /** validType=DAYS 时有效期天数（上限 share.max-valid-days） */
    private Integer validDays;

    /** 是否设置提取码（requirePassword=true 时 accessPassword 必填） */
    private Boolean requirePassword;

    private String accessPassword;

    /** 下载策略：true=允许下载（maxDownload 可选）/ false=禁止下载（只能预览） */
    private Boolean allowDownload;

    /** 下载次数限制，0=不限（仅 allowDownload=true 时有效） */
    private Integer maxDownload;

    /** 允许转存 */
    private Boolean allowSave;
}
