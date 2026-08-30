package com.cloud.backend.dto.file;

import lombok.Data;

@Data
public class FilePreviewResponse {

    private String type;
    private String url;
    private String thumbnailUrl;
    private String content;
    private String name;
    private Long size;
}
