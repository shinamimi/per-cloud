package com.cloud.backend.entity;

import com.cloud.backend.enums.FileStatus;
import lombok.Data;
import java.time.LocalDateTime;

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

    private FileStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}