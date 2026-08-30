package com.cloud.backend.dto.share;

import lombok.Data;

@Data
public class ShareFileNodeResponse {

    private Long id;
    private Long parentId;
    private String name;
    private Boolean isDir;
    private Long size;
    private String mimeType;
    private String extension;
}
