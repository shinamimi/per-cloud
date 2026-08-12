package com.cloud.backend.enums;

import lombok.Getter;

/**
 * 用户状态枚举 —— 对应 t_user.status 字段，存储 TINYINT。
 * NORMAL=1 正常，DISABLED=0 禁用，LOCKED=2 登录锁定，INACTIVE=3 长期未登录。
 * LoginUser.isEnabled() 基于此判断。
 *
 * 修改指引：
 * - 【统一】新增枚举值        → 在枚举末尾追加常量（value=下一个序号）；EnumOrdinalTypeHandler 按 ordinal 存库，
 *                       只能追加在末尾且 value 与 ordinal 保持一致；改后需同步 DB 存量数据与 EnumOrdinalTypeHandler/fromValue 兜底逻辑
 * - 【统一】重命名枚举值      → 修改常量名；DB 按 ordinal 映射不受影响，同步修改引用处（含 LoginUser.isEnabled()）即可；
 *                       改后需同步引用处（含 LoginUser.isEnabled()）
 * - 【统一】修改 value       → 常量括号内数值；必须与声明顺序（ordinal）一致，否则 fromValue() 与 DB 存储错位，
 *                       且影响登录启用的状态判断；改后需同步声明顺序（ordinal）、DB 存量数据与登录启用状态判断
 * - 【统一】调整声明顺序      → 禁止；存量 t_user.status 按 ordinal 存库，调序会导致用户状态错乱；
 *                       改后需同步 t_user.status DB 存量数据与 EnumOrdinalTypeHandler
 */
@Getter
public enum UserStatus {

    DISABLED(0),
    NORMAL(1),
    LOCKED(2),
    INACTIVE(3);

    /** 【统一】改后需同步 DB 存量数据、EnumOrdinalTypeHandler/fromValue 兜底逻辑与登录启用的状态判断（ordinal 映射 t_user.status） */
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