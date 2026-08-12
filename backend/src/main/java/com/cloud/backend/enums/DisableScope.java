package com.cloud.backend.enums;

import lombok.Getter;

/**
 * 对象级禁用范围（禁用粒度）。
 * GLOBAL=全站禁（按内容 hash，全站引用该内容的文件全部禁用）；
 * USER=仅用户（用户×内容，只禁该用户对该内容，其他用户不受影响）。
 *
 * 修改指引：
 * - 【统一】新增枚举值        → 在枚举末尾追加常量（value 取未占用序号）；需在管理员禁用/批量禁用请求
 *                      （FileStatusRequest、BatchFileStatusRequest 等）与禁用执行逻辑中补充新粒度，否则新粒度无法生效；
 *                      改后需同步管理员禁用/批量禁用请求（FileStatusRequest、BatchFileStatusRequest 等）与禁用执行逻辑
 * - 【统一】重命名枚举值      → 修改常量名；该枚举作为请求参数（JSON）传入，前端需同步更新，无 DB 存储影响；
 *                      改后需同步前端请求参数（JSON 反序列化）
 * - 【统一】修改 value       → 常量括号内数值；value 是请求参数内部标识，改动需同步调用方判断逻辑，无存量数据迁移；
 *                      改后需同步调用方判断逻辑（AdminFileServiceImpl 等按 getValue() 命中判定）
 * - 【习惯】修改默认值        → 请求 DTO 中默认 DisableScope.USER（仅用户）；影响未显式传 scope 时的默认禁用粒度
 */
@Getter
public enum DisableScope {

    GLOBAL(1),
    USER(2);

    /** 【统一】改后需同步调用方判断逻辑（AdminFileServiceImpl 等按 getValue() 命中判定） */
    private final int value;

    DisableScope(int value) {
        this.value = value;
    }
}
