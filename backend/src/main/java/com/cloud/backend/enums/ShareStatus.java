package com.cloud.backend.enums;

import lombok.Getter;

@Getter
public enum ShareStatus {

    NORMAL(0),
    EXPIRED(1),
    CANCELED(2),
    EXHAUSTED(3);

    /** 【统一】改后需同步 DB 存量数据与 EnumOrdinalTypeHandler/fromValue 兜底逻辑（ordinal 映射 t_share.status） */
    private final int value;

    ShareStatus(int value) {
        this.value = value;
    }

    public static ShareStatus fromValue(int value) {
        for (ShareStatus status : ShareStatus.values()) {
            if (status.value == value) {
                return status;
            }
        }
        return NORMAL;
    }
}