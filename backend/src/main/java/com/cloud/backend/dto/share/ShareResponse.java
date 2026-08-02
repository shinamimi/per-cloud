package com.cloud.backend.dto.share;

import com.cloud.backend.enums.ShareStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 我的分享列表项 —— GET /api/shares。
 * 根节点名/大小由服务层填充（单文件=文件名，目录=根目录名）。
 */
@Data
public class ShareResponse {

    private Long id;
    private Long fileId;
    private Boolean isDir;
    private String name;
    private ShareStatus status;
    private String shareToken;
    private Boolean requirePassword;
    private LocalDateTime expireTime;
    private Boolean allowDownload;
    private Integer maxDownload;
    private Integer downloadCount;
    private Boolean allowSave;
    private LocalDateTime createdAt;

    public static ShareResponse from(com.cloud.backend.entity.Share share) {
        ShareResponse response = new ShareResponse();
        response.setId(share.getId());
        response.setFileId(share.getFileId());
        response.setIsDir(share.getIsDir() != null && share.getIsDir() == 1);
        response.setStatus(share.getStatus());
        response.setShareToken(share.getShareToken());
        response.setRequirePassword(share.getAccessPassword() != null && !share.getAccessPassword().isEmpty());
        response.setExpireTime(share.getExpireTime());
        response.setAllowDownload(share.getAllowDownload() == null || share.getAllowDownload() == 1);
        response.setMaxDownload(share.getMaxDownload());
        response.setDownloadCount(share.getDownloadCount());
        response.setAllowSave(share.getAllowSave() == null || share.getAllowSave() == 1);
        response.setCreatedAt(share.getCreatedAt());
        return response;
    }
}
