package com.cloud.backend.dto.admin;

import com.cloud.backend.enums.ShareStatusEnum;

import java.time.LocalDateTime;

public class AdminShareResponse {

    private Long id;
    private Long userId;
    private Long fileId;
    private String shareToken;
    private ShareStatusEnum status;
    private LocalDateTime expireTime;
    private Integer maxDownload;
    private Integer downloadCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AdminShareResponse(Long id, Long userId, Long fileId, String shareToken, ShareStatusEnum status,
                              LocalDateTime expireTime, Integer maxDownload, Integer downloadCount,
                              LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.fileId = fileId;
        this.shareToken = shareToken;
        this.status = status;
        this.expireTime = expireTime;
        this.maxDownload = maxDownload;
        this.downloadCount = downloadCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getFileId() { return fileId; }
    public String getShareToken() { return shareToken; }
    public ShareStatusEnum getStatus() { return status; }
    public LocalDateTime getExpireTime() { return expireTime; }
    public Integer getMaxDownload() { return maxDownload; }
    public Integer getDownloadCount() { return downloadCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
