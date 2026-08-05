package com.cloud.backend.dto.admin;

import lombok.Data;

/**
 * 日志配置更新请求（null 字段恢复配置默认值）。
 *
 * 修改指引：
 * - 【习惯】修改单位             → operationDays/loginDays 单位均为天（日志保留天数）；改动需同步日志清理任务逻辑与前端
 * - 【习惯】修改 null 语义         → null 字段恢复配置默认值；改动需同步 service 的空值判断，否则会影响未传字段
 * - 【习惯】修改保留天数          → 影响操作日志/登录日志的清理周期与存储量
 */
@Data
public class LogSettingsRequest {

    /** 操作日志保存天数 */
    private Integer operationDays;

    /** 登录日志保存天数 */
    private Integer loginDays;
}
