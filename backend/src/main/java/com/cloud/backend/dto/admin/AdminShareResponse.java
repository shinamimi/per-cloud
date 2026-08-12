package com.cloud.backend.dto.admin;

import com.cloud.backend.entity.Share;
import com.cloud.backend.enums.ShareStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端分享响应 —— GET /api/admin/shares。
 *
 * 修改指引：
 * - 【统一】修改响应字段名/类型    → 字段为前端管理端分享列表取值依据；改后需同步 AdminShareService 的 from() 与前端
 * - 【统一】修改 status           → 自定义枚举 ShareStatus（enums/ShareStatus.java：NORMAL=0 生效/EXPIRED=1 过期/CANCELED=2 取消/
 *                           EXHAUSTED=3 达下载上限），存储 TINYINT；改后需同步枚举定义与前端状态展示
 * - 【统一】修改 allowDownload/allowSave → from() 中 DB 为 null/1 时按 true 处理；改后需同步 from() 空值处理与前端分享行为展示
 * - 【统一】修改 maxDownload/downloadCount → Integer 下载上限/已下载次数，前端用于剩余次数展示；改后需同步分享逻辑与前端剩余次数展示
 * - 【习惯】修改 ownerName/fileName → 由服务层填充；改动需同步填充逻辑，否则返回 null
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
