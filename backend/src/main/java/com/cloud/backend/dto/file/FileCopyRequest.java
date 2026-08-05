package com.cloud.backend.dto.file;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 复制请求（同用户复制，跨用户本期不做）。
 *
 * 修改指引：
 * - 【习惯】修改 targetParentId  → Long targetParentId；目标目录 id，请求体字段名对应 POST /api/files/{id}/copy 入参，
 *                         目标目录必须属于当前用户，否则按文件不存在处理
 * - 【习惯】修改校验注解 @NotNull → 目标目录缺失直接 400，改动影响接口契约
 * - 【习惯】放开跨用户复制       → 需要新增被复制者/目标者字段并改造服务层校验，前端需同步传参
 */
@Data
public class FileCopyRequest {

    @NotNull(message = "目标目录不能为空")
    private Long targetParentId;
}
