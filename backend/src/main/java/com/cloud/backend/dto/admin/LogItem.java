package com.cloud.backend.dto.admin;

import com.cloud.backend.enums.OperationType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 日志查询结果项 —— 操作日志 join 用户表后返回给管理端展示。
 *
 * 修改指引：
 * - 【习惯】修改响应字段名/类型    → 字段为前端日志列表取值依据，改动需同步日志查询 SQL（join 用户表）与前端
 * - 【习惯】修改 operation         → 自定义枚举 OperationType（enums/OperationType.java，取值 LOGIN/UPLOAD_FILE 等）；
 *                           改动需同步枚举定义与前端操作类型展示
 * - 【习惯】修改 username          → 由日志表 join 用户表得到；改动需同步 SQL join 与前端展示
 * - 【习惯】新增响应字段          → 新增字段并同步日志查询 SQL 与前端，否则该字段恒为 null
 */
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
