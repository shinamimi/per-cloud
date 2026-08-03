package com.cloud.backend.dto.admin;

import com.cloud.backend.enums.OperationType;
import com.cloud.backend.enums.TargetType;

import java.time.LocalDateTime;

/**
 * 后台操作日志响应 DTO —— 操作日志列表接口的展示结构。
 */
public class AdminLogResponse {

    /** 日志记录 ID */
    private Long id;
    /** 操作者用户 ID */
    private Long userId;
    /** 操作类型（LOGIN / UPLOAD_FILE / DELETE_FILE 等） */
    private OperationType operation;
    /** 操作目标类型（USER / FILE / SHARE / TEAM） */
    private TargetType targetType;
    /** 操作目标 ID（配合 targetType 定位具体对象） */
    private Long targetId;
    /** 操作详情描述 */
    private String detail;
    /** 操作来源 IP（用于安全审计） */
    private String ip;
    /** 操作时间 */
    private LocalDateTime createdAt;

    public AdminLogResponse(Long id, Long userId, OperationType operation, TargetType targetType,
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
    public OperationType getOperation() { return operation; }
    public TargetType getTargetType() { return targetType; }
    public Long getTargetId() { return targetId; }
    public String getDetail() { return detail; }
    public String getIp() { return ip; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
