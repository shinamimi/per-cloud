package com.cloud.backend.enums;

import lombok.Getter;

/**
 * 好友请求状态 —— t_friend_request.status（PENDING/ACCEPTED/REJECTED）。
 * 状态机（docs/friend-system.md §4.2）：
 * 发起 → 待接受（PENDING）→ 接受（ACCEPTED，同时写入 t_friendship）| 拒绝（REJECTED，可重发）。
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
