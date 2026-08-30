package com.cloud.backend.enums;

import lombok.Getter;

@Getter
public enum Role {

    USER(0),
    OPERATOR(10),
    ADMIN(20),
    SUPER_ADMIN(100);

    /** 【统一】改后需同步 DB 存量数据、EnumOrdinalTypeHandler 与权限判断（getValue() 比较） */
    private final int value;

    Role(int value) {
        this.value = value;
    }

    public static Role fromValue(int value) {
        for (Role role : Role.values()) {
            if (role.value == value) {
                return role;
            }
        }
        return USER;
    }
}