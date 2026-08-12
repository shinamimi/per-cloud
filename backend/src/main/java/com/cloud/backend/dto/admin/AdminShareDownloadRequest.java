package com.cloud.backend.dto.admin;

import lombok.Data;

/**
 * 管理端分享下载开关更新请求 —— PUT /api/admin/shares/{id}/download。
 *
 * 修改指引：
 * - 【统一】修改 allowDownload 字段名/类型 → boolean，true=允许下载 false=禁止下载；对应接口请求参数；改后需同步
 *                                    AdminShareService 开关逻辑与前端开关组件
 * - 【统一】修改默认行为          → 当前无默认值（必传）；改后需同步 service 默认值逻辑与前端未传参行为说明
 */
@Data
public class AdminShareDownloadRequest {

    /** true=允许下载 false=禁止下载 */
    private boolean allowDownload;
}
