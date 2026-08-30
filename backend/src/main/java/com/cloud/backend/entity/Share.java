package com.cloud.backend.entity;

import com.cloud.backend.enums.ShareStatus;
import lombok.Data;
import java.time.LocalDateTime;

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