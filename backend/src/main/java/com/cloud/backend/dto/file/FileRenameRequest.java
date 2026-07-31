package com.cloud.backend.dto.file;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 重命名请求。
 */
@Data
public class FileRenameRequest {

    @NotBlank(message = "新文件名不能为空")
    private String name;
}
