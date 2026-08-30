package com.cloud.backend.dto.file;

import lombok.Data;

import java.util.List;

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
