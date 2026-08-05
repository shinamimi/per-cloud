package com.cloud.backend.dto.file;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 移动请求（仅改数据库 parentId，MinIO 对象不动）。
 *
 * 修改指引：
 * - 【习惯】修改 targetParentId  → Long targetParentId；目标目录 id，请求体字段名对应 POST /api/files/{id}/move 入参；
 *                         目标目录必须属于当前用户，移动仅改 parentId，不影响存储对象
 * - 【习惯】修改校验注解 @NotNull → 目标目录缺失直接 400，改动影响接口契约
 */
@Data
public class FileMoveRequest {

    @NotNull(message = "目标目录不能为空")
    private Long targetParentId;
}
