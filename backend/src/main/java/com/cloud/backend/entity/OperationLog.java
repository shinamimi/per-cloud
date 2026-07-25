package com.cloud.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OperationLog {

    private Long id;

    private Long userId;

    private String operation;

    private String targetType;

    private Long targetId;

    private String detail;

    private String ip;

    private String userAgent;

    private LocalDateTime createdAt;
}