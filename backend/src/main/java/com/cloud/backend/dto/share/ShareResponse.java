package com.cloud.backend.dto.share;

import com.cloud.backend.enums.ShareStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 我的分享列表项 —— GET /api/shares。
 * 根节点名/大小由服务层填充（单文件=文件名，目录=根目录名）。
 *
 * 修改指引：
 * - 【习惯】修改 id              → Long id；分享记录 id，取消/删除/修改有效期接口的路径参数
 *                         （DELETE /api/shares/{id}、DELETE /api/shares/{id}/record、PUT /api/shares/{id}）
 * - 【习惯】修改 fileId          → Long fileId；被分享文件/文件夹 id
 * - 【习惯】修改 isDir / name    → Boolean isDir / String name；根节点是否目录与名称（服务层填充）
 * - 【习惯】修改 status          → ShareStatus status；自定义枚举（enums/ShareStatus.java）：
 *                         NORMAL=0 / EXPIRED=1 / CANCELED=2 / EXHAUSTED=3，前端据此展示分享状态
 * - 【习惯】修改 shareToken      → String shareToken；分享 token，前端据此拼装访客链接（10 位短码）
 * - 【习惯】修改 requirePassword → Boolean requirePassword；是否设置提取码
 * - 【习惯】修改 expireTime      → LocalDateTime expireTime；到期时间，PERMANENT 永久分享为 null
 * - 【习惯】修改 allowDownload / maxDownload / downloadCount → 下载策略与计数（maxDownload=0 不限），达限置 EXHAUSTED
 * - 【习惯】修改 allowSave       → Boolean allowSave；是否允许转存
 * - 【习惯】修改 createdAt       → LocalDateTime createdAt；创建时间
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
