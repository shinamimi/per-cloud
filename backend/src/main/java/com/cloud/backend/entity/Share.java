package com.cloud.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Share {

    private Long id;

    private Long userId;

    private Long fileId;

    private String shareToken;

    private String accessPassword;

    private Integer status;

    private LocalDateTime expireTime;

    private Integer maxDownload;

    private Integer downloadCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}