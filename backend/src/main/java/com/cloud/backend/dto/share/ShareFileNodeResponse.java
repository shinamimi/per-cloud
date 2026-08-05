package com.cloud.backend.dto.share;

import lombok.Data;

/**
 * 分享文件树节点 —— GET /api/shares/access/{token}/files。
 * id 为快照节点 id（t_share_file），后续预览/下载/转存均用该 id（防原文件 id 越权枚举）。
 *
 * 修改指引：
 * - 【习惯】修改 id              → Long id；快照节点 id（t_share_file），后续预览/下载/转存均以它为参数；
 *                         前端勿改用原文件 id，否则越权校验不通过
 * - 【习惯】修改 parentId        → Long parentId；快照父节点 id，前端按此拼装树形结构
 * - 【习惯】修改 name / isDir    → String name / Boolean isDir；节点名与是否目录
 * - 【习惯】修改 size            → Long size；文件大小，单位：字节，前端展示需换算
 * - 【习惯】修改 mimeType / extension → String mimeType / String extension；预览判断与下载命名用
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
