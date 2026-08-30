package com.cloud.backend.enums;

import lombok.Getter;

@Getter
public enum TeamMemberRole {

    /** 普通成员 */
    MEMBER(0),
    /** 团队管理员（value >= ADMIN 可管理团队文件等） */
    ADMIN(10),
    /** 团队所有者（最高权限，可转让/解散团队） */
    OWNER(20);

    /** 【统一】改后需同步 TeamMemberRoleTypeHandler 与团队权限判断处（getValue() >= ADMIN 等判断） */
    private final int value;

    TeamMemberRole(int value) {
        this.value = value;
    }

    /**
     * 按 value 反查枚举；未匹配（如历史脏数据）时兜底为 MEMBER，避免 NPE。
     */
    public static TeamMemberRole fromValue(int value) {
        for (TeamMemberRole r : TeamMemberRole.values()) {
            if (r.value == value) return r;
        }
        return MEMBER;
    }
}
