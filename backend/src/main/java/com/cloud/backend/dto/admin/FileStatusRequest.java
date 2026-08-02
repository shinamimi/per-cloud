package com.cloud.backend.dto.admin;

import com.cloud.backend.enums.FileStatus;
import lombok.Data;

/**
 * 管理端文件状态变更请求 —— PUT /api/admin/files/{id}/status。
 * NORMAL=启用，DISABLED=禁用。
 */
@Data
public class FileStatusRequest {

    private FileStatus status;
}
