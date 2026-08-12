package com.cloud.backend.dto.admin;

import com.cloud.backend.enums.OperationType;
import com.cloud.backend.enums.TargetType;
import java.time.LocalDateTime;

/**
 * 操作日志筛选条件 DTO —— 后台日志查询的可选过滤项，全部字段缺省时查询全部。
 *
 * 修改指引：
 * - 【统一】修改筛选字段名/类型    → userId（操作者）、operation、targetType、startTime/endTime（时间区间，含上下限）；
 *                           字段为后台日志查询接口入参，改动需同步日志查询 SQL 与前端筛选条件
 * - 【统一】修改 operation         → 自定义枚举 OperationType（enums/OperationType.java，取值 LOGIN/UPLOAD_FILE 等）；
 *                           改动需同步枚举定义与前端类型筛选下拉
 * - 【统一】修改 targetType        → 自定义枚举 TargetType（enums/TargetType.java：USER/FILE/SHARE/TEAM）；
 *                           改动需同步枚举定义与前端类型筛选下拉
 * - 【统一】修改时间区间语义       → startTime/endTime 为含边界过滤（>= / <=）；改动影响区间查询结果，需同步 SQL 与前端
 * - 【统一】新增筛选项            → 新增字段并同步日志查询 SQL 与前端，否则该条件不生效
 */
public class LogFilterRequest {

    /** 操作者用户 ID */
    private Long userId;
    /** 操作类型（LOGIN / UPLOAD_FILE 等） */
    private OperationType operation;
    /** 操作目标类型（USER / FILE / SHARE / TEAM） */
    private TargetType targetType;
    /** 操作时间下限（含） */
    private LocalDateTime startTime;
    /** 操作时间上限（含） */
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
