package com.cloud.backend.dto.file;

import lombok.Data;

@Data
public class AudioPlayResponse {

    private Long fileId;

    private String name;

    private String url;
}
