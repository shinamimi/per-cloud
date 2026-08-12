package com.cloud.backend.enums;

import lombok.Getter;

/**
 * 文件状态枚举 —— 对应数据库 t_file.status 字段，存储 TINYINT。
 * DELETED=0（已逻辑删除/回收站），NORMAL=1（可用），DISABLED=2（管理员禁用：用户可见但不可下载/预览/分享）。
 * 配合 EnumOrdinalTypeHandler，ordinal() 映射为 TINYINT 值。
 *
 * 修改指引：
 * - 【统一】新增枚举值        → 在枚举末尾追加常量（value=下一个序号）；EnumOrdinalTypeHandler 按 ordinal() 存库，
 *                       只能追加在末尾且 value 与 ordinal 保持一致，否则 DB 值与 getValue() 错位；
 *                       改后需同步 DB 存量数据与 EnumOrdinalTypeHandler/fromValue 兜底逻辑
 * - 【统一】重命名枚举值      → 修改常量名；DB 按 ordinal 映射不受影响，同步修改引用处（含 fromValue 兜底逻辑）即可；
 *                       改后需同步引用处（含 fromValue 兜底逻辑）
 * - 【统一】修改 value       → 常量括号内数值；必须与声明顺序（ordinal）一致，否则 fromValue() 与 DB 读取不符；
 *                       改后需同步声明顺序（ordinal）与 DB 存量数据
 * - 【统一】调整声明顺序      → 禁止；存量 t_file.status 按 ordinal 存库，调序会导致历史文件状态错乱；
 *                       改后需同步 t_file.status DB 存量数据与 EnumOrdinalTypeHandler
 */
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