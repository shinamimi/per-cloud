package com.cloud.backend.dto.friend;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class FriendRequestCreateRequest {

    @NotNull(message = "目标用户不能为空")
    private Long toUserId;
}
