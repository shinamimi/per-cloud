package com.cloud.backend.entity;

import com.cloud.backend.enums.FileStatus;
import com.cloud.backend.enums.FileType;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文件/目录实体 —— 对应数据库 t_file 表。
 *
 * 设计思路：
 * 文件系统采用"目录树"模型（类似 Linux 文件系统）：
 * - 每个文件/目录有 parentId 指向父目录，根目录 parentId=0
 * - type 区分 FILE / DIRECTORY（统一表模型，is_directory 为兼容旧字段保留）
 * - category 为文件分类（0-图片 1-文档 2-视频 3-音频 4-压缩包 5-其他），搜索按类型过滤
 * - teamId 归属：0 表示个人空间，>0 表示团队空间
 * - objectName 是 MinIO 中的对象路径（仅文件有此值）
 * - fileHash 用于秒传（相同文件哈希直接引用已有 object，不上传）
 */
@Data
public class File {

    private Long id;
    private Long userId;
    private Long teamId;
    private Long parentId;
    private String name;
    private String path;
    private Long size;
    private String mimeType;
    private String extension;
    private String fileHash;
    private Integer isDirectory;
    private FileType type;
    private Integer category;
    private String objectName;
    private FileStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 兼容判断：是否为目录（type == DIRECTORY 或旧数据 is_directory == 1） */
    public boolean isDir() {
        return type == FileType.DIRECTORY || (type == null && isDirectory != null && isDirectory == 1);
    }
}
