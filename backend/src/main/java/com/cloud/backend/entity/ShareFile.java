package com.cloud.backend.entity;

import lombok.Data;

/**
 * 分享快照实体 —— 对应数据库 t_share_file 表。
 *
 * 目录分享时锁定创建时刻的文件树（docs/share-module.md §4.2）：
 * 分享后原文件的新增/改名/删除均不影响访客看到的内容；
 * 树结构由 parentId 指向本表 id 自包含（0=根节点）。
 * 下载/预览/转存时通过 fileId 回查原文件状态（删除/禁用则拒绝）。
 */
@Data
public class ShareFile {

    private Long id;
    private Long shareId;
    /** 原文件 id */
    private Long fileId;
    /** 快照树父节点 id（0=根） */
    private Long parentId;
    private String name;
    /** 1=目录 0=文件 */
    private Integer isDir;
    private Long size;
    private String mimeType;
    private String extension;
    private String fileHash;
}
