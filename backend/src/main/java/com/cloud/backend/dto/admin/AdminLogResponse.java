package com.cloud.backend.dto.admin;

import com.cloud.backend.enums.OperationTypeEnum;
import com.cloud.backend.enums.TargetTypeEnum;

import java.time.LocalDateTime;

public class AdminLogResponse {

    private Long id;
    private Long userId;
    private OperationTypeEnum operation;
    private TargetTypeEnum targetType;
    private Long targetId;
    private String detail;
    private String ip;
    private LocalDateTime createdAt;

    public AdminLogResponse(Long id, Long userId, OperationTypeEnum operation, TargetTypeEnum targetType,
                            Long targetId, String detail, String ip, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.operation = operation;
        this.targetType = targetType;
        this.targetId = targetId;
        this.detail = detail;
        this.ip = ip;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public OperationTypeEnum getOperation() { return operation; }
    public TargetTypeEnum getTargetTypeEnum() { return targetType; }
    public Long getTargetId() { return targetId; }
    public String getDetail() { return detail; }
    public String getIp() { return ip; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
