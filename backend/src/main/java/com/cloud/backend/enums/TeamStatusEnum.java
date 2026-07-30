package com.cloud.backend.enums;

import lombok.Getter;

@Getter
public enum TeamStatusEnum {

    DISSOLVED(0),
    NORMAL(1);

    private final int value;

    TeamStatusEnum(int value) {
        this.value = value;
    }

    public static TeamStatusEnum fromValue(int value) {
        for (TeamStatusEnum s : TeamStatusEnum.values()) {
            if (s.value == value) return s;
        }
        return NORMAL;
    }
}
