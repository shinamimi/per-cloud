package com.cloud.backend.enums;

import lombok.Getter;

/**
 * 团队状态枚举 —— 对应 t_team.status 字段，存储 TINYINT（EnumOrdinalTypeHandler）。
 * 解散团队时成员记录 status 一并置 0，记录保留用于追溯。
 *
 * 修改指引：
 * - 【习惯】新增枚举值        → 在枚举末尾追加常量（value=下一个序号）；EnumOrdinalTypeHandler 按 ordinal 存库，
 *                       只能追加在末尾且 value 与 ordinal 保持一致
 * - 【习惯】重命名枚举值      → 修改常量名；DB 按 ordinal 映射不受影响，同步修改引用处（含 fromValue 兜底逻辑）即可
 * - 【习惯】修改 value       → 常量括号内数值；必须与声明顺序（ordinal）一致，否则 fromValue() 与 DB 存储错位
 * - 【习惯】调整声明顺序      → 禁止；存量 t_team.status 按 ordinal 存库，调序会导致历史团队状态错乱
 */
@Getter
public enum TeamStatus {

    /** 已解散（解散后成员关系记录保留但全部置为退出） */
    DISSOLVED(0),
    /** 正常 */
    NORMAL(1);

    private final int value;

    TeamStatus(int value) {
        this.value = value;
    }

    /**
     * 按 value 反查枚举；未匹配时兜底为 NORMAL，避免脏数据导致 NPE。
     */
    public static TeamStatus fromValue(int value) {
        for (TeamStatus s : TeamStatus.values()) {
            if (s.value == value) return s;
        }
        return NORMAL;
    }
}
