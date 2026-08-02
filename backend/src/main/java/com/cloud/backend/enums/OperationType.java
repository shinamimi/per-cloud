package com.cloud.backend.enums;

import lombok.Getter;

/**
 * 操作日志类型 —— 数据库 t_operation_log.type 字段是 VARCHAR，存枚举名称。
 * 如 LOGIN、REGISTER、UPLOAD_FILE 等，用默认的 EnumTypeHandler 即可。
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