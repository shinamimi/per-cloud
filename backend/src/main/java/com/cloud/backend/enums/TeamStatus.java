package com.cloud.backend.enums;

import lombok.Getter;

@Getter
public enum TeamStatus {

    /** 已解散（解散后成员关系记录保留但全部置为退出） */
    DISSOLVED(0),
    /** 正常 */
    NORMAL(1);

    /** 【统一】改后需同步 DB 存量数据与 EnumOrdinalTypeHandler/fromValue 兜底逻辑（ordinal 映射 t_team.status） */
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
