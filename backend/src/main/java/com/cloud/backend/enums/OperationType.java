package com.cloud.backend.enums;

import lombok.Getter;

@Getter
    public enum OperationType {

        /** 【统一】改后需同步业务方法 @Log 注解引用处与日志筛选（LogFilterRequest） */
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