package com.cloud.backend.entity;

import com.cloud.backend.enums.FriendRequestStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FriendRequest {

    private Long id;
    private Long fromUserId;
    private Long toUserId;
    private FriendRequestStatus status;
    private LocalDateTime createdAt;
}
