package com.cloud.backend.dto.admin;

import com.cloud.backend.enums.FileStatus;
import lombok.Data;

import java.util.List;

/**
 * 管理端批量状态变更请求 —— POST /api/admin/files/batch-status。
 */
@Data
public class BatchFileStatusRequest {

    private List<Long> ids;
    private FileStatus status;
}
