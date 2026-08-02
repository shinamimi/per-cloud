package com.cloud.backend.dto.admin;

import com.cloud.backend.entity.File;
import com.cloud.backend.enums.FileStatus;
import com.cloud.backend.enums.FileType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端全局文件响应 —— GET /api/admin/files（列表）、GET /api/admin/files/{id}（详情）。
 * userName/teamName 由服务层批量填充（所属用户/所属团队显示名）。
 */
@Data
public class AdminFileResponse {

    private Long id;
    private Long userId;
    private String userName;
    private Long teamId;
    private String teamName;
    private Long parentId;
    private String name;
    private String path;
    private Long size;
    private String mimeType;
    private String extension;
    private Boolean isDirectory;
    private FileType type;
    private Integer category;
    private FileStatus status;
    /** 内容 hash（禁用来源判断用） */
    private String fileHash;
    /** 禁用来源：GLOBAL=全站禁（红色展示）/ USER=仅该用户禁用；非禁用为 null（docs/admin-file-management.md 5.1） */
    private String disabledScope;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AdminFileResponse from(File file) {
        AdminFileResponse response = new AdminFileResponse();
        response.setId(file.getId());
        response.setUserId(file.getUserId());
        response.setTeamId(file.getTeamId());
        response.setParentId(file.getParentId());
        response.setName(file.getName());
        response.setPath(file.getPath());
        response.setSize(file.getSize());
        response.setMimeType(file.getMimeType());
        response.setExtension(file.getExtension());
        response.setIsDirectory(file.isDir());
        response.setType(file.getType());
        response.setCategory(file.getCategory());
        response.setStatus(file.getStatus());
        response.setFileHash(file.getFileHash());
        response.setCreatedAt(file.getCreatedAt());
        response.setUpdatedAt(file.getUpdatedAt());
        return response;
    }
}
