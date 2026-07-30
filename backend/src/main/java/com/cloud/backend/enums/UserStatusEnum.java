package com.cloud.backend.enums;

import lombok.Getter;

/**
 * 用户状态枚举 —— 对应 t_user.status 字段，存储 TINYINT。
 * NORMAL=1 正常，DISABLED=0 禁用/冻结。
 * LoginUser.isEnabled() 基于此判断。
 */
@Getter
public enum UserStatusEnum {

    DISABLED(0),
    NORMAL(1);

    private final int value;

    UserStatusEnum(int value) {
        this.value = value;
    }

    public static UserStatusEnum fromValue(int value) {
        for (UserStatusEnum status : UserStatusEnum.values()) {
            if (status.value == value) {
                return status;
            }
        }
        return NORMAL;
    }
}