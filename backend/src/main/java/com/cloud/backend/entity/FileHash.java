package com.cloud.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 秒传索引实体 —— 对应数据库 t_file_hash 表。
 *
 * 设计思路：
 * 全局 SHA256 索引表，hash 唯一。命中即秒传：新文件引用同一物理对象，ref_count +1。
 * 物理删除（回收站 30 天清理）时 ref_count -1，归零才真正删除 MinIO 对象。
 */
@Data
public class FileHash {

    private Long id;
    private String fileHash;
    private String objectName;
    private Long size;
    private String mimeType;
    private Integer refCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
