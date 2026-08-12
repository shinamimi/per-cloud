package com.cloud.backend.dto.admin;

import com.cloud.backend.entity.File;
import com.cloud.backend.enums.FileStatus;
import com.cloud.backend.enums.FileType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端全局文件响应 —— GET /api/admin/files（列表）、GET /api/admin/files/{id}（详情）。
 * userName/teamName 由服务层批量填充（所属用户/所属团队显示名）。
 *
 * 修改指引：
 * - 【统一】修改响应字段名/类型    → 字段为前端管理端文件列表/详情取值依据；改后需同步 AdminFileService 的 from() 与前端类型定义
 * - 【统一】修改 type/status       → 自定义枚举 FileType（FILE=0/DIRECTORY=1，enums/FileType.java）、FileStatus
 *                           （DELETED=0/NORMAL=1/DISABLED=2，enums/FileStatus.java），前端按枚举值展示；改后需同步枚举定义与前端展示
 * - 【统一】修改 size 单位         → 当前为 Long 字节，前端需换算展示；改后需同步前端单位换算与管理端容量列展示
 * - 【统一】修改 disabledScope     → GLOBAL=全站禁（红）/ USER=仅用户，非禁用为 null；改后需同步禁用执行逻辑与前端来源展示
 * - 【习惯】修改 userName/teamName → 由服务层批量填充；改动需同步填充逻辑，否则返回 null
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
    /** 禁用来源：GLOBAL=全站禁（红色展示）/ USER=仅该用户禁用；非禁用为 null */
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
