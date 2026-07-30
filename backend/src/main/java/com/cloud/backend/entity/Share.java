package com.cloud.backend.entity;

import com.cloud.backend.enums.ShareStatus;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 分享记录实体 —— 对应数据库 t_share 表。
 *
 * 设计思路：
 * shareToken 是分享链接的唯一标识（类似百度网盘的 "s/xxxx"），由 UUID 生成。
 * accessPassword 可选密码保护（为空则公开访问）。
 * maxDownload / downloadCount 控制下载次数限制。
 * expireTime 控制过期时间，配合定时任务更新 status 为 EXPIRED。
 */
@Data
public class Share {

    private Long id;
    private Long userId;
    private Long fileId;
    private String shareToken;
    private String accessPassword;
    private ShareStatus status;
    private LocalDateTime expireTime;
    private Integer maxDownload;
    private Integer downloadCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}