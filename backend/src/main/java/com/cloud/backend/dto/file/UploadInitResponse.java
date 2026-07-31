package com.cloud.backend.dto.file;

import lombok.Data;

/**
 * 初始化分片上传响应。
 */
@Data
public class UploadInitResponse {

    private String uploadId;
    private long chunkSize;
    private int totalChunks;
}
