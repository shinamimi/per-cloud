package com.cloud.backend.dto.file;

import lombok.Data;

import java.util.List;

/**
 * 断点续传进度响应：已上传分片序号列表。
 */
@Data
public class UploadProgressResponse {

    private String uploadId;
    private String fileName;
    private Long fileSize;
    private String mimeType;
    private List<Integer> uploadedChunks;
    private Long parentId;
    private Long teamId;
}
