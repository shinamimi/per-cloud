package com.cloud.backend.enums;

import lombok.Getter;

@Getter
public enum Role {

    USER(0),
    ADMIN(1);

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