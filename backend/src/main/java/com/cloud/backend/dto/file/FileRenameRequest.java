package com.cloud.backend.dto.file;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 重命名请求。
 *
 * 修改指引：
 * - 【习惯】修改 name            → String name；新文件名，请求体字段名对应 PUT /api/files/{id}/rename 入参；
 *                         重名与非法字符校验在服务层
 * - 【习惯】修改校验注解 @NotBlank → 空名直接 400，改动影响前端表单必填契约
 */
@Data
public class FileRenameRequest {

    @NotBlank(message = "新文件名不能为空")
    private String name;
}
