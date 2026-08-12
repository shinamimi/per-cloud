package com.cloud.backend.dto.file;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建目录请求。
 *
 * 修改指引：
 * - 【统一】修改 parentId        → Long parentId；父目录 id，请求体字段名对应 POST /api/files/directory 入参；根目录传 0；改名需同步前端 API 层与 Service 组装
 * - 【统一】修改 name            → String name；目录名，非空校验，重名与非法字符校验在服务层；改名需同步前端 API 层与 Service 组装
 * - 【统一】修改 teamId          → Long teamId；团队空间预留（本期仅个人空间），默认 null 表示个人空间；改名需同步前端 API 层与 Service 组装
 * - 【统一】修改校验注解 @NotNull/@NotBlank → 必填字段缺失/为空直接 400，改动影响前端表单必填契约；改后需同步前端表单必填契约
 */
@Data
public class DirectoryCreateRequest {

    @NotNull(message = "父目录不能为空")
    private Long parentId;

    @NotBlank(message = "目录名不能为空")
    private String name;

    /** 团队空间预留（本期仅个人空间） */
    private Long teamId;
}
