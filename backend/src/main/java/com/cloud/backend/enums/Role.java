package com.cloud.backend.enums;

import lombok.Getter;

/**
 * 用户角色枚举 —— 决定接口访问权限。
 * value 字段越大权限越高，存储为 TINYINT。
 *
 * USER=0 → 普通用户（默认）
 * OPERATOR=10 → 运营人员
 * ADMIN=20 → 管理员
 * SUPER_ADMIN=100 → 超级管理员
 *
 * 配合 EnumOrdinalTypeHandler 将枚举映射为 TINYINT（ordinal() = 0/1/2/3，不等于 value，
 * 但由于枚举声明顺序与 value 递增一致，代码中用 getValue() 而非 ordinal() 判断大小）。
 *
 * 修改指引：
 * - 【习惯】新增枚举值        → 在枚举末尾追加常量（value 取未占用的递增档位）；只能追加在末尾（EnumOrdinalTypeHandler
 *                       按 ordinal 存库），且 value 决定权限高低，需确认权限判断（getValue() 比较）符合预期
 * - 【习惯】重命名枚举值      → 修改常量名；DB 按 ordinal 映射不受影响，同步修改引用处即可
 * - 【习惯】修改 value       → 常量括号内数值；直接影响权限比较（getValue() 大小判断），必须保持声明顺序与 value 递增一致
 * - 【习惯】调整声明顺序      → 禁止；存量 t_user.role 按 ordinal 存库，调序会导致用户角色错乱
 */
@Getter
public enum Role {

    USER(0),
    OPERATOR(10),
    ADMIN(20),
    SUPER_ADMIN(100);

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