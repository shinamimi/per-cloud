package com.cloud.backend.enums;

import lombok.Getter;

/**
 * 用户角色枚举 —— 决定接口访问权限。
 * value 字段越大权限越高，存储为 TINYINT。
 *
 * USER=0 → 普通用户（默认）
 * OPERATOR=10 → 运营人员
 * ADMIN=20 → 管理员
 * SUPER_ADMIN=100 → 超级管理员
 *
 * 配合 EnumOrdinalTypeHandler 将枚举映射为 TINYINT（ordinal() = 0/1/2/3，不等于 value，
 * 但由于枚举声明顺序与 value 递增一致，代码中用 getValue() 而非 ordinal() 判断大小）。
 */
@Getter
public enum Role {

    USER(0),
    OPERATOR(10),
    ADMIN(20),
    SUPER_ADMIN(100);

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