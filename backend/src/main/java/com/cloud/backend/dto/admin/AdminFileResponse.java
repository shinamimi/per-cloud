package com.cloud.backend.dto.admin;

import com.cloud.backend.enums.FileStatusEnum;

import java.time.LocalDateTime;

public class AdminFileResponse {

    private Long id;
    private Long userId;
    private Long parentId;
    private String name;
    private String path;
    private Long size;
    private String mimeType;
    private String extension;
    private Integer isDirectory;
    private FileStatusEnum status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AdminFileResponse(Long id, Long userId, Long parentId, String name, String path, Long size,
                             String mimeType, String extension, Integer isDirectory, FileStatusEnum status,
                             LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.parentId = parentId;
        this.name = name;
        this.path = path;
        this.size = size;
        this.mimeType = mimeType;
        this.extension = extension;
        this.isDirectory = isDirectory;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getParentId() { return parentId; }
    public String getName() { return name; }
    public String getPath() { return path; }
    public Long getSize() { return size; }
    public String getMimeType() { return mimeType; }
    public String getExtension() { return extension; }
    public Integer getIsDirectory() { return isDirectory; }
    public FileStatusEnum getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
