package com.cloud.backend.dto.file;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 秒传请求（全站 SHA256 命中则引用现有对象，不上传）。
 */
@Data
public class UploadSecRequest {

    @NotBlank(message = "文件哈希不能为空")
    private String fileHash;

    @NotBlank(message = "文件名不能为空")
    private String fileName;

    @NotNull(message = "文件大小不能为空")
    @Positive(message = "文件大小必须大于 0")
    private Long fileSize;

    @NotNull(message = "父目录不能为空")
    private Long parentId;

    /** 团队空间预留（本期仅个人空间） */
    private Long teamId;
}
