package com.cloud.backend.enums;

import lombok.Getter;

@Getter
public enum OperationType {

    LOGIN,
    REGISTER,
    UPLOAD_FILE,
    DOWNLOAD_FILE,
    DELETE_FILE,
    RESTORE_FILE,
    CREATE_SHARE,
    CANCEL_SHARE,
    UPDATE_USER;
}