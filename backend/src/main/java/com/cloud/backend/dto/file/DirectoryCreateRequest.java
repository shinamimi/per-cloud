package com.cloud.backend.dto.file;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建目录请求。
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
