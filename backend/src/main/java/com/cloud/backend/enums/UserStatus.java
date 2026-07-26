package com.cloud.backend.enums;

import lombok.Getter;

@Getter
public enum UserStatus {

    DISABLED(0),
    NORMAL(1);

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