package com.cloud.backend.enums;

import lombok.Getter;

/**
 * 操作日志的操作目标类型 —— 标记本次操作关联什么类型的对象。
 * 配合 operation_log.target_id 显式操作对象 ID，方便审计追溯。
 *
 * 修改指引：
 * - 【统一】新增枚举值        → 在枚举末尾追加常量；t_operation_log.target_type 按枚举名称存储（默认 EnumTypeHandler），
 *                       新增值需在业务侧用 @Log(target = TargetType.XXX) 标记对应操作；
 *                       改后需同步业务侧 @Log(target = TargetType.XXX) 引用处
 * - 【统一】重命名枚举值      → 修改常量名；DB 按枚举名称存储，已存日志需一并迁移，且需同步修改所有 @Log 引用处；
 *                       改后需同步 DB 存量日志迁移与所有 @Log 引用处
 * - 【统一】删除枚举值        → 删除常量并清理引用；存量日志中的该名称保留但无法映射回枚举，管理端筛选/展示需兼容；
 *                       改后需同步清理引用处与管理端筛选/展示兼容
 */
@Getter
    public enum TargetType {

        /** 【统一】改后同步业务侧 @Log(target = TargetType.XXX) 引用处 */
        USER,
    FILE,
    SHARE,
    TEAM;
}