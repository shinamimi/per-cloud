package com.cloud.backend.enums;

import lombok.Getter;

/**
 * 文件类型枚举 —— 对应数据库 t_file.type 字段，存储 TINYINT。
 * FILE=0（文件），DIRECTORY=1（目录）。
 * 注意 ordinal 必须与 value 一致（EnumOrdinalTypeHandler 用 ordinal() 写入数据库）。
 */
@Getter
public enum FileType {

    FILE(0),
    DIRECTORY(1);

    private final int value;

    FileType(int value) {
        this.value = value;
    }

    public static FileType fromValue(int value) {
        for (FileType type : FileType.values()) {
            if (type.value == value) {
                return type;
            }
        }
        return FILE;
    }
}
