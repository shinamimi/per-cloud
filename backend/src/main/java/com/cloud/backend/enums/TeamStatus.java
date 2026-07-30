package com.cloud.backend.enums;

import lombok.Getter;

@Getter
public enum TeamStatus {

    DISSOLVED(0),
    NORMAL(1);

    private final int value;

    TeamStatus(int value) {
        this.value = value;
    }

    public static TeamStatus fromValue(int value) {
        for (TeamStatus s : TeamStatus.values()) {
            if (s.value == value) return s;
        }
        return NORMAL;
    }
}
