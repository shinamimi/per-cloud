package com.cloud.backend.entity;

import com.cloud.backend.enums.FriendRequestStatus;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 好友请求实体 —— t_friend_request 表（ADR-010：双向确认关系层）。
 */
@Data
public class FriendRequest {

    private Long id;
    private Long fromUserId;
    private Long toUserId;
    private FriendRequestStatus status;
    private LocalDateTime createdAt;
}
