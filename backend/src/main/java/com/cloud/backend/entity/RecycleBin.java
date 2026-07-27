package com.cloud.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 回收站实体 —— 对应数据库 t_recycle_bin 表。
 *
 * 设计思路：
 * 用户删除文件时不会立即从 MinIO 删除，而是将记录移入 recycle_bin 表，
 * 保留原文件信息和过期时间。在 expireTime 之前可以恢复。
 * 定时任务清理过期记录时同时删除 MinIO 中的原始对象。
 */
@Data
public class RecycleBin {

    private Long id;
    private Long userId;
    private Long fileId;
    private String originalName;
    private String objectName;
    private Long parentId;
    private Long size;
    private String mimeType;
    private LocalDateTime deletedTime;
    private LocalDateTime expireTime;
}