package com.cloud.backend.dto.file;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 移动请求（仅改数据库 parentId，MinIO 对象不动）。
 */
@Data
public class FileMoveRequest {

    @NotNull(message = "目标目录不能为空")
    private Long targetParentId;
}
