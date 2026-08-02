package com.cloud.backend.dto.admin;

import com.cloud.backend.enums.DisableScope;
import com.cloud.backend.enums.FileStatus;
import lombok.Data;

/**
 * 管理端文件状态变更请求 —— PUT /api/admin/files/{id}/status。
 * NORMAL=启用，DISABLED=禁用；scope 仅禁用时生效（docs/admin-file-management.md 5.1）：
 * GLOBAL=全站禁（按内容 hash），USER=仅用户（默认）。
 */
@Data
public class FileStatusRequest {

    private FileStatus status;
    private DisableScope scope = DisableScope.USER;
}
