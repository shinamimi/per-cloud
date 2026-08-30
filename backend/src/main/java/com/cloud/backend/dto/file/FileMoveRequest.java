package com.cloud.backend.dto.file;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FileMoveRequest {

    @NotNull(message = "目标目录不能为空")
    private Long targetParentId;
}
