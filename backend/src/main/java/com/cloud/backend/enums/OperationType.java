package com.cloud.backend.enums;

import lombok.Getter;

/**
 * 操作日志类型 —— 数据库 t_operation_log.type 字段是 VARCHAR，存枚举名称。
 * 如 LOGIN、REGISTER、UPLOAD_FILE 等，用默认的 EnumTypeHandler 即可。
 *
 * 修改指引：
 * - 【习惯】新增枚举值        → 在枚举末尾追加常量；业务方法需加 @Log(operation = OperationType.XXX) 才会计入操作日志，
 *                       并可配置日志筛选（LogFilterRequest）
 * - 【习惯】重命名枚举值      → 修改常量名；t_operation_log.type 按枚举名称存储，已存日志需一并迁移，
 *                       且需同步修改所有 @Log 注解引用处
 * - 【习惯】删除枚举值        → 删除常量并清理引用；存量日志中的该名称保留但无法映射回枚举，管理端展示需兼容
 */
@Getter
public enum OperationType {

    LOGIN,
    REGISTER,
    UPLOAD_FILE,
    DOWNLOAD_FILE,
    DELETE_FILE,
    RESTORE_FILE,
    CREATE_DIRECTORY,
    CREATE_SHARE,
    CANCEL_SHARE,
    DELETE_SHARE,
    UPDATE_USER,
    DISABLE_FILE,
    ENABLE_FILE,

    TEAM_CREATE,
    TEAM_DISSOLVE,
    TEAM_INVITE,
    TEAM_REMOVE,
    TEAM_LEAVE,
    RESET_PASSWORD;
}