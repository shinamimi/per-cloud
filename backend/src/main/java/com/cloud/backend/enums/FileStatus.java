package com.cloud.backend.enums;

import lombok.Getter;

/**
 * 文件状态枚举 —— 对应数据库 t_file.status 字段，存储 TINYINT。
 * NORMAL=1（可用），DELETED=0（已逻辑删除/回收站）。
 * 配合 EnumOrdinalTypeHandler，ordinal() 映射为 TINYINT 值。
 */
@Getter
public enum FileStatus {

    DELETED(0),
    NORMAL(1);

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