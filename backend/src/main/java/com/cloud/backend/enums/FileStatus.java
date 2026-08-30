package com.cloud.backend.enums;

import lombok.Getter;

@Getter
public enum FileStatus {

    DELETED(0),
    NORMAL(1),
    DISABLED(2);

    /** 【统一】改后需同步 DB 存量数据与 EnumOrdinalTypeHandler/fromValue 兜底逻辑（ordinal 映射 t_file.status） */
    private final int value;

    FileStatus(int value) {
        this.value = value;
    }

    public static FileStatus fromValue(int value) {
        for (FileStatus status : FileStatus.values()) {
            if (status.value == value) {
                return status;
            }
        }
        return NORMAL;
    }
}