package com.cloud.backend.enums;

import lombok.Getter;

/**
 * 分享状态枚举 —— 对应 t_share.status 字段，存储 TINYINT。
 * NORMAL=0 生效中，EXPIRED=1 已过期，CANCELED=2 已取消，EXHAUSTED=3 下载次数已达上限。
 *
 * 修改指引：
 * - 【统一】新增枚举值        → 在枚举末尾追加常量（value=下一个序号）；EnumOrdinalTypeHandler 按 ordinal 存库，
 *                       只能追加在末尾且 value 与 ordinal 保持一致；改后需同步 DB 存量数据与 EnumOrdinalTypeHandler/fromValue 兜底逻辑
 * - 【统一】重命名枚举值      → 修改常量名；DB 按 ordinal 映射不受影响，同步修改引用处即可；改后需同步引用处
 * - 【统一】修改 value       → 常量括号内数值；必须与声明顺序（ordinal）一致，否则 fromValue() 与 DB 存储错位；
 *                       改后需同步声明顺序（ordinal）与 DB 存量数据
 * - 【统一】调整声明顺序      → 禁止；存量 t_share.status 按 ordinal 存库，调序会导致历史分享状态错乱；
 *                       改后需同步 t_share.status DB 存量数据与 EnumOrdinalTypeHandler
 */
@Getter
public enum ShareStatus {

    NORMAL(0),
    EXPIRED(1),
    CANCELED(2),
    EXHAUSTED(3);

    /** 【统一】改后需同步 DB 存量数据与 EnumOrdinalTypeHandler/fromValue 兜底逻辑（ordinal 映射 t_share.status） */
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