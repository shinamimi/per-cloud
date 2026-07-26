package com.cloud.backend.enums;

import lombok.Getter;

@Getter
public enum ShareStatus {

    NORMAL(0),
    EXPIRED(1),
    CANCELED(2);

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