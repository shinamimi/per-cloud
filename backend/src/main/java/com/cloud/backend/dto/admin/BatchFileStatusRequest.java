package com.cloud.backend.dto.admin;

import com.cloud.backend.enums.DisableScope;
import com.cloud.backend.enums.FileStatus;
import lombok.Data;

import java.util.List;

/**
 * 管理端批量状态变更请求 —— POST /api/admin/files/batch-status。
 * scope 仅禁用时生效（GLOBAL=全站禁 / USER=仅用户，默认 USER）。
 */
@Data
public class BatchFileStatusRequest {

    private List<Long> ids;
    private FileStatus status;
    private DisableScope scope = DisableScope.USER;
}
