package com.cloud.backend.enums;

import lombok.Getter;

@Getter
public enum FileType {

    FILE(0),
    DIRECTORY(1);

    /** 【统一】改后需同步 DB 存量数据与 EnumOrdinalTypeHandler/fromValue 兜底逻辑（ordinal 映射 t_file.type） */
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
