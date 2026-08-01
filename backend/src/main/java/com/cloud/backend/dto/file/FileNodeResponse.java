package com.cloud.backend.dto.file;

import com.cloud.backend.entity.File;
import com.cloud.backend.enums.FileType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件列表节点响应。
 * userId/uploaderName 为上传者信息：个人文件列表用不到，团队文件列表由服务层填充。
 */
@Data
public class FileNodeResponse {

    private Long id;
    private Long parentId;
    private String name;
    private Long size;
    private String mimeType;
    private String extension;
    private Boolean isDirectory;
    private FileType type;
    private Integer category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long userId;
    private String uploaderName;

    public static FileNodeResponse from(File file) {
        FileNodeResponse response = new FileNodeResponse();
        response.setId(file.getId());
        response.setParentId(file.getParentId());
        response.setName(file.getName());
        response.setSize(file.getSize());
        response.setMimeType(file.getMimeType());
        response.setExtension(file.getExtension());
        response.setIsDirectory(file.isDir());
        response.setType(file.getType());
        response.setCategory(file.getCategory());
        response.setCreatedAt(file.getCreatedAt());
        response.setUpdatedAt(file.getUpdatedAt());
        response.setUserId(file.getUserId());
        return response;
    }
}
