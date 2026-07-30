package com.cloud.backend.entity;

import com.cloud.backend.enums.FileStatusEnum;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文件/目录实体 —— 对应数据库 t_file 表。
 *
 * 设计思路：
 * 文件系统采用"目录树"模型（类似 Linux 文件系统）：
 * - 每个文件/目录有 parentId 指向父目录，根目录 parentId=0
 * - isDirectory=1 表示目录，=0 表示文件
 * - objectName 是 MinIO 中的对象路径（仅文件有此值）
 * - fileHash 用于秒传（相同文件哈希直接引用已有 object，不上传）
 * - path 是文件实际路径（方便前端展示树形结构）
 */
@Data
public class File {

    private Long id;
    private Long userId;
    private Long parentId;
    private String name;
    private String path;
    private Long size;
    private String mimeType;
    private String extension;
    private String fileHash;
    private Integer isDirectory;
    private String objectName;
    private FileStatusEnum status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}