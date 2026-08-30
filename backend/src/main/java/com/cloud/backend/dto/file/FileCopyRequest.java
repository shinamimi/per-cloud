package com.cloud.backend.dto.file;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FileCopyRequest {

    @NotNull(message = "目标目录不能为空")
    private Long targetParentId;
}
