package com.cloud.backend.enums;

import lombok.Getter;

@Getter
public enum TeamMemberRole {

    MEMBER(0),
    ADMIN(10),
    OWNER(20);

    private final int value;

    TeamMemberRole(int value) {
        this.value = value;
    }

    public static TeamMemberRole fromValue(int value) {
        for (TeamMemberRole r : TeamMemberRole.values()) {
            if (r.value == value) return r;
        }
        return MEMBER;
    }
}
