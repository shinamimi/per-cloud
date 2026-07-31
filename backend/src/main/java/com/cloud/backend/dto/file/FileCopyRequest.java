package com.cloud.backend.dto.file;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 复制请求（同用户复制，跨用户本期不做）。
 */
@Data
public class FileCopyRequest {

    @NotNull(message = "目标目录不能为空")
    private Long targetParentId;
}
