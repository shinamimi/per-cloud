package com.cloud.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

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
