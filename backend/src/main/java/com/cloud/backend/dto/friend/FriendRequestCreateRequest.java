package com.cloud.backend.dto.friend;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

/** 发送好友请求 —— 入参 */
@Data
public class FriendRequestCreateRequest {

    @NotNull(message = "目标用户不能为空")
    private Long toUserId;
}
