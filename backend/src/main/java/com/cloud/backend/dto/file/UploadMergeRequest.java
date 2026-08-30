package com.cloud.backend.dto.file;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UploadMergeRequest {

    @NotBlank(message = "uploadId 不能为空")
    private String uploadId;
}
