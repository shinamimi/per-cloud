package com.cloud.backend.entity;

import lombok.Data;

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
