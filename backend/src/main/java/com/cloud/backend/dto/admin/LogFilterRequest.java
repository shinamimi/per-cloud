package com.cloud.backend.dto.admin;

import com.cloud.backend.enums.OperationTypeEnum;
import com.cloud.backend.enums.TargetTypeEnum;
import java.time.LocalDateTime;

public class LogFilterRequest {

    private Long userId;
    private OperationTypeEnum operation;
    private TargetTypeEnum targetType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public OperationTypeEnum getOperation() { return operation; }
    public void setOperation(OperationTypeEnum operation) { this.operation = operation; }

    public TargetTypeEnum getTargetType() { return targetType; }
    public void setTargetType(TargetTypeEnum targetType) { this.targetType = targetType; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}
