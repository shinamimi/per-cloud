package com.cloud.backend.enums;

import lombok.Getter;

/**
 * 团队成员角色枚举 —— 决定成员在团队内的操作权限。
 *
 * 设计思路：
 * 1. 存储使用自定义 value（MEMBER=0 / ADMIN=10 / OWNER=20）而非 ordinal()，
 *    数据库映射注册了 TeamMemberRoleTypeHandler 专用处理器
 * 2. 权限大小按 value 递增，新增角色取未占用的中间档位即可，不影响存量数据
 */
@Getter
public enum TeamMemberRole {

    /** 普通成员 */
    MEMBER(0),
    /** 团队管理员（value >= ADMIN 可管理团队文件等） */
    ADMIN(10),
    /** 团队所有者（最高权限，可转让/解散团队） */
    OWNER(20);

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
