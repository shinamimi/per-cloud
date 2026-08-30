package com.cloud.backend.dto.file;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BatchDownloadRequest {

    @NotEmpty(message = "请选择要下载的文件")
    private List<Long> fileIds;
}
