package com.cloud.backend.entity;

import com.cloud.backend.enums.FileStatus;
import com.cloud.backend.enums.FileType;
import lombok.Data;
import java.time.LocalDateTime;

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
