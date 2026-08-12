package com.cloud.backend.enums;

import lombok.Getter;

/**
 * 团队成员角色枚举 —— 决定成员在团队内的操作权限。
 *
 * 设计思路：
 * 1. 存储使用自定义 value（MEMBER=0 / ADMIN=10 / OWNER=20）而非 ordinal()，
 *    数据库映射注册了 TeamMemberRoleTypeHandler 专用处理器
 * 2. 权限大小按 value 递增，新增角色取未占用的中间档位即可，不影响存量数据
 *
 * 修改指引：
 * - 【统一】新增枚举值      → 在枚举末尾追加常量（value 取未占用的中间档位）；TeamMemberRoleTypeHandler 按 value 存取、
 *                     不依赖 ordinal，取中间档位不影响存量数据，同时需在团队权限判断处补充新档位逻辑；
 *                     改后需同步 TeamMemberRoleTypeHandler 与团队权限判断处
 * - 【统一】重命名枚举值    → 修改常量名；DB 按 value 存储不受影响，同步修改引用处即可；改后需同步引用处
 * - 【统一】修改 value     → 常量括号内数值；已存数据按旧 value 映射会错位（需数据迁移），且影响权限比较
 *                    （getValue() >= ADMIN 等判断）；改后需同步 DB 存量数据迁移与权限比较（getValue() >= ADMIN 等判断）
 * - 【习惯】调整声明顺序    → 不影响 DB（按 value 存储），仅影响 values() 遍历顺序，建议保持 value 从低到高
 * - 【习惯】修改兜底值      → fromValue() 返回的兜底枚举；影响脏数据/未知 value 时的默认角色
 */
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
