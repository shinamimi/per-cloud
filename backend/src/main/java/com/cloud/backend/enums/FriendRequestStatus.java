package com.cloud.backend.enums;

import lombok.Getter;

@Getter
    public enum FriendRequestStatus {

        /** 【统一】改后需同步好友请求状态机流转逻辑（接受/拒绝处理）与 t_friend_request.status 写入 */
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
