package com.cloud.backend.entity;

import com.cloud.backend.enums.OperationType;
import com.cloud.backend.enums.TargetType;
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
 *
 * 修改指引：
 * - 【习惯】修改 id / userId        → Long id（t_operation_log.id 主键）/ Long userId（user_id，0=系统/匿名）；仅审计查询
 * - 【统一】修改 operation          → OperationType operation；对应 t_operation_log.operation（VARCHAR 存枚举名称），
 *                            取值 LOGIN/UPLOAD_FILE/DELETE_FILE 等（见 enums/OperationType.java），
 *                            由 @Log 注解 + OperationLogAspect 切面写入，改枚举见 OperationType 修改指引；
 *                            改后需同步 DB 存量数据与 @Log 注解/OperationLogAspect 切面写入处
 * - 【统一】修改 targetType / targetId → TargetType targetType（t_operation_log.target_type，VARCHAR 存枚举名称，
 *                            USER/FILE/SHARE/TEAM，见 enums/TargetType.java）/ Long targetId（target_id）；关联操作对象，
 *                            改枚举见 TargetType 修改指引；
 *                            改后需同步 DB 存量数据与 @Log 注解/切面写入处
 * - 【习惯】修改 detail             → String detail；对应 t_operation_log.detail（VARCHAR(500)），操作详情描述，仅展示
 * - 【习惯】修改 ip / userAgent     → String ip（t_operation_log.ip）/ String userAgent（user_agent）；安全审计来源信息，仅展示
 * - 【习惯】修改 createdAt          → LocalDateTime createdAt；自动维护，无业务联动
 */
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