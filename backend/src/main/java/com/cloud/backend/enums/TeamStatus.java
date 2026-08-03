package com.cloud.backend.enums;

import lombok.Getter;

/**
 * 团队状态枚举 —— 对应 t_team.status 字段，存储 TINYINT（EnumOrdinalTypeHandler）。
 * 解散团队时成员记录 status 一并置 0，记录保留用于追溯。
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
