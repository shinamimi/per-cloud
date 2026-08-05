package com.cloud.backend.enums;

import lombok.Getter;

/**
 * 好友请求状态 —— t_friend_request.status（PENDING/ACCEPTED/REJECTED）。
 * 状态机：
 * 发起 → 待接受（PENDING）→ 接受（ACCEPTED，同时写入 t_friendship）| 拒绝（REJECTED，可重发）。
 *
 * 修改指引：
 * - 【习惯】新增枚举值        → 在枚举末尾追加常量；需同步补充好友请求状态机流转逻辑（接受/拒绝处理），
 *                       否则新状态无法到达，也无法写入 t_friend_request.status
 * - 【习惯】重命名枚举值      → 修改常量名；DB 按枚举名称存储（默认 EnumTypeHandler），已存记录需一并迁移
 * - 【习惯】删除枚举值        → 删除常量并清理引用；存量记录中的该名称将无法反查，fromName() 会兜底为 PENDING
 * - 【习惯】修改兜底值        → fromName() 返回的兜底枚举；影响脏数据/未知名称时的默认状态
 */
@Getter
public enum FriendRequestStatus {

    PENDING,
    ACCEPTED,
    REJECTED;

    public static FriendRequestStatus fromName(String name) {
        for (FriendRequestStatus status : values()) {
            if (status.name().equalsIgnoreCase(name)) {
                return status;
            }
        }
        return PENDING;
    }
}
