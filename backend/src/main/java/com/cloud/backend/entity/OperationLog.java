package com.cloud.backend.entity;

import com.cloud.backend.enums.OperationType;
import com.cloud.backend.enums.TargetType;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OperationLog {

    private Long id;

    private Long userId;

    private OperationType operation;

    private TargetType targetType;

    private Long targetId;

    private String detail;

    private String ip;

    private String userAgent;

    private LocalDateTime createdAt;
}