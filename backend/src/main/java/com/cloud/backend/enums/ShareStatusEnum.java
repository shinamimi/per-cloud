package com.cloud.backend.enums;

import lombok.Getter;

/**
 * 分享状态枚举 —— 对应 t_share.status 字段，存储 TINYINT。
 * NORMAL=0 生效中，EXPIRED=1 已过期，CANCELED=2 已取消。
 */
@Getter
public enum ShareStatusEnum {

    NORMAL(0),
    EXPIRED(1),
    CANCELED(2);

    private final int value;

    ShareStatusEnum(int value) {
        this.value = value;
    }

    public static ShareStatusEnum fromValue(int value) {
        for (ShareStatusEnum status : ShareStatusEnum.values()) {
            if (status.value == value) {
                return status;
            }
        }
        return NORMAL;
    }
}