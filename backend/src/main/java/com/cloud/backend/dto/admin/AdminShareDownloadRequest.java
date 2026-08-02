package com.cloud.backend.dto.admin;

import lombok.Data;

/**
 * 管理端分享下载开关更新请求 —— PUT /api/admin/shares/{id}/download。
 */
@Data
public class AdminShareDownloadRequest {

    /** true=允许下载 false=禁止下载 */
    private boolean allowDownload;
}
