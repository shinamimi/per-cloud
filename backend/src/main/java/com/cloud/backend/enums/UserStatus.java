package com.cloud.backend.enums;

import lombok.Getter;

@Getter
public enum UserStatus {

    DISABLED(0),
    NORMAL(1),
    LOCKED(2),
    INACTIVE(3);

    /** 【统一】改后需同步 DB 存量数据、EnumOrdinalTypeHandler/fromValue 兜底逻辑与登录启用的状态判断（ordinal 映射 t_user.status） */
    private final int value;

    UserStatus(int value) {
        this.value = value;
    }

    public static UserStatus fromValue(int value) {
        for (UserStatus status : UserStatus.values()) {
            if (status.value == value) {
                return status;
            }
        }
        return NORMAL;
    }
}