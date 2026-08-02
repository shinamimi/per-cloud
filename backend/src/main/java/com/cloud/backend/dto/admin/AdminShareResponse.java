package com.cloud.backend.dto.admin;

import com.cloud.backend.entity.Share;
import com.cloud.backend.enums.ShareStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端分享响应 —— GET /api/admin/shares。
 */
@Data
public class AdminShareResponse {

    private Long id;
    private Long userId;
    private String ownerName;
    private Long fileId;
    private String fileName;
    private Boolean isDir;
    private String shareToken;
    private ShareStatus status;
    private LocalDateTime expireTime;
    private Integer maxDownload;
    private Integer downloadCount;
    private Boolean allowDownload;
    private Boolean allowSave;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AdminShareResponse from(Share share) {
        AdminShareResponse response = new AdminShareResponse();
        response.setId(share.getId());
        response.setUserId(share.getUserId());
        response.setFileId(share.getFileId());
        response.setIsDir(share.getIsDir() != null && share.getIsDir() == 1);
        response.setShareToken(share.getShareToken());
        response.setStatus(share.getStatus());
        response.setExpireTime(share.getExpireTime());
        response.setMaxDownload(share.getMaxDownload());
        response.setDownloadCount(share.getDownloadCount());
        response.setAllowDownload(share.getAllowDownload() == null || share.getAllowDownload() == 1);
        response.setAllowSave(share.getAllowSave() == null || share.getAllowSave() == 1);
        response.setCreatedAt(share.getCreatedAt());
        response.setUpdatedAt(share.getUpdatedAt());
        return response;
    }
}
