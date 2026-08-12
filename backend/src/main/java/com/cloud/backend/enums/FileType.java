package com.cloud.backend.enums;

import lombok.Getter;

/**
 * 文件类型枚举 —— 对应数据库 t_file.type 字段，存储 TINYINT。
 * FILE=0（文件），DIRECTORY=1（目录）。
 * 注意 ordinal 必须与 value 一致（EnumOrdinalTypeHandler 用 ordinal() 写入数据库）。
 *
 * 修改指引：
 * - 【统一】新增枚举值      → 在枚举末尾追加常量（value=下一个序号）；EnumOrdinalTypeHandler 按 ordinal() 写入数据库，
 *                     只能追加在末尾且 value 与 ordinal 保持一致；改后需同步 DB 存量数据与 EnumOrdinalTypeHandler/fromValue 兜底逻辑
 * - 【统一】重命名枚举值    → 修改常量名；DB 按 ordinal 映射不受影响，同步修改引用处（含 fromValue 兜底逻辑）即可；
 *                     改后需同步引用处（含 fromValue 兜底逻辑）
 * - 【统一】修改 value     → 常量括号内数值；必须与声明顺序（ordinal）一致，否则 fromValue() 与 DB 存储错位；
 *                     改后需同步声明顺序（ordinal）与 DB 存量数据
 * - 【统一】调整声明顺序    → 禁止；存量 t_file.type 按 ordinal 存库，调序会导致历史文件类型错乱；
 *                     改后需同步 t_file.type DB 存量数据与 EnumOrdinalTypeHandler
 */
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
