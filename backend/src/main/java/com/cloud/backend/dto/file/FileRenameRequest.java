package com.cloud.backend.dto.file;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FileRenameRequest {

    @NotBlank(message = "新文件名不能为空")
    private String name;
}
