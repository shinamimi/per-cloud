package com.cloud.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RecycleBin {

    private Long id;
    private Long userId;
    private Long fileId;
    private String originalName;
    private String objectName;
    private String fileHash;
    private Integer type;
    private Long teamId;
    private Integer deletedBy;
    private Long parentId;
    private Long size;
    private String mimeType;
    private LocalDateTime deletedTime;
    private LocalDateTime expireTime;
}
