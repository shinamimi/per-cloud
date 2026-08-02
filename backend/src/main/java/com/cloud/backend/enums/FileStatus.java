package com.cloud.backend.enums;

import lombok.Getter;

/**
 * 文件状态枚举 —— 对应数据库 t_file.status 字段，存储 TINYINT。
 * DELETED=0（已逻辑删除/回收站），NORMAL=1（可用），DISABLED=2（管理员禁用：用户可见但不可下载/预览/分享）。
 * 配合 EnumOrdinalTypeHandler，ordinal() 映射为 TINYINT 值。
 */
@Getter
public enum FileStatus {

    DELETED(0),
    NORMAL(1),
    DISABLED(2);

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