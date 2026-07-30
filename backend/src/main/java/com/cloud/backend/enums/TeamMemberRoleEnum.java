package com.cloud.backend.enums;

import lombok.Getter;

@Getter
public enum TeamMemberRoleEnum {

    MEMBER(0),
    ADMIN(10),
    OWNER(20);

    private final int value;

    TeamMemberRoleEnum(int value) {
        this.value = value;
    }

    public static TeamMemberRoleEnum fromValue(int value) {
        for (TeamMemberRoleEnum r : TeamMemberRoleEnum.values()) {
            if (r.value == value) return r;
        }
        return MEMBER;
    }
}
