package com.cloud.backend.dto.share;

import lombok.Data;

/**
 * 分享文件树节点 —— GET /api/shares/access/{token}/files。
 * id 为快照节点 id（t_share_file），后续预览/下载/转存均用该 id（防原文件 id 越权枚举）。
 */
@Data
public class ShareFileNodeResponse {

    private Long id;
    private Long parentId;
    private String name;
    private Boolean isDir;
    private Long size;
    private String mimeType;
    private String extension;
}
