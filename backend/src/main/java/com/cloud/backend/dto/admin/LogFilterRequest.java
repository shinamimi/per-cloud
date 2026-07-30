package com.cloud.backend.dto.admin;

import com.cloud.backend.enums.OperationType;
import com.cloud.backend.enums.TargetType;
import java.time.LocalDateTime;

public class LogFilterRequest {

    private Long userId;
    private OperationType operation;
    private TargetType targetType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public OperationType getOperation() { return operation; }
    public void setOperation(OperationType operation) { this.operation = operation; }

    public TargetType getTargetType() { return targetType; }
    public void setTargetType(TargetType targetType) { this.targetType = targetType; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}
