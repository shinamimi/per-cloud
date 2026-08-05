package com.cloud.backend.controller.admin;

/**
 * 已拆分为五个专注的控制器：
 * - AdminDashboardController — 仪表盘统计
 * - AdminFileController      — 文件管理
 * - AdminShareController     — 分享管理
 * - AdminLogController       — 操作日志
 * - AdminSettingsController   — 系统设置
 *
 * 修改指引：
 * - 【习惯】新增管理端能力       → 写入对应专注控制器（AdminDashboard/AdminFile/AdminShare/AdminLog/AdminSettings 等），
 *                          本类已废弃，不改动；影响：避免聚合路由职责混乱
 * - 【习惯】恢复聚合路由        → 若需重新声明为 @RestController 聚合入口，需注册映射并确认 SecurityConfig
 *                         中 /api/admin/** 的权限分级（OPERATOR+/ADMIN+），改动影响管理端路由结构
 */
@Deprecated
public class AdminController {
}
