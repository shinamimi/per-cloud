package com.cloud.backend.dto.file;

import lombok.Data;

@Data
public class UploadInitResponse {

    private String uploadId;
    private long chunkSize;
    private int totalChunks;
}
