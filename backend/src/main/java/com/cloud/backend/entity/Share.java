package com.cloud.backend.entity;

import com.cloud.backend.enums.ShareStatus;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 分享记录实体 —— 对应数据库 t_share 表。
 *
 * 设计思路：
 * shareToken 是分享链接的唯一标识（10 位去混淆短码 + 生成时查重）。
 * accessPassword 可选密码保护（为空则公开访问）。
 * maxDownload / downloadCount 控制下载次数限制（0=不限；全局共享累计，达限状态变 EXHAUSTED）。
 * expireTime 控制过期时间（NULL=永久分享）。
 * allowDownload / allowSave 下载策略与转存开关。
 * isDir = 1 表示目录分享（创建时锁定快照 t_share_file）。
 */
@Data
public class Share {

    private Long id;
    private Long userId;
    private Long fileId;
    /** 1=目录分享（快照锁定） 0=单文件分享 */
    private Integer isDir;
    private String shareToken;
    private String accessPassword;
    private ShareStatus status;
    private LocalDateTime expireTime;
    private Integer maxDownload;
    private Integer downloadCount;
    /** 1=允许下载 0=禁止下载（只能在线预览） */
    private Integer allowDownload;
    /** 1=允许转存 0=禁止转存 */
    private Integer allowSave;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}