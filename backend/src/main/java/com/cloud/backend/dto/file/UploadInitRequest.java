package com.cloud.backend.dto.file;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class UploadInitRequest {

    @NotBlank(message = "文件名不能为空")
    private String fileName;

    @NotNull(message = "文件大小不能为空")
    @Positive(message = "文件大小必须大于 0")
    private Long fileSize;

    @NotBlank(message = "文件哈希不能为空")
    private String fileHash;

    @NotNull(message = "父目录不能为空")
    private Long parentId;

    /** 团队空间预留（本期仅个人空间） */
    private Long teamId;
}
