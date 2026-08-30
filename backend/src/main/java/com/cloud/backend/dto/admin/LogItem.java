package com.cloud.backend.dto.admin;

import com.cloud.backend.enums.OperationType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LogItem {

    private Long id;
    private Long userId;
    private String username;
    private OperationType operation;
    private String detail;
    private String ip;
    private LocalDateTime createdAt;

    public LogItem() {
    }

    public LogItem(Long id, Long userId, String username, OperationType operation, String detail,
                   String ip, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.operation = operation;
        this.detail = detail;
        this.ip = ip;
        this.createdAt = createdAt;
    }
}
