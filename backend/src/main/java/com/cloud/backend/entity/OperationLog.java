package com.cloud.backend.entity;

import com.cloud.backend.enums.OperationTypeEnum;
import com.cloud.backend.enums.TargetTypeEnum;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 操作日志实体 —— 对应数据库 t_operation_log 表。
 *
 * 设计思路：
 * 审计日志，记录所有关键操作（登录、上传、删除等）。
 * 管理员可以从后台查看用户操作历史，排查问题。
 * - operation：操作类型枚举，存储为 VARCHAR（EnumTypeHandler 默认）
 * - targetType + targetId：关联操作对象（如 FILE + 123 表示对文件 123 的操作）
 * - detail：操作详情描述（如 "删除了文件 report.pdf"）
 * - ip / userAgent：来源信息，用于安全审计
 */
@Data
public class OperationLog {

    private Long id;
    private Long userId;
    private OperationTypeEnum operation;
    private TargetTypeEnum targetType;
    private Long targetId;
    private String detail;
    private String ip;
    private String userAgent;
    private LocalDateTime createdAt;
}