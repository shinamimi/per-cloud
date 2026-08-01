package com.cloud.backend.dto.friend;

import com.cloud.backend.entity.FriendRequest;
import com.cloud.backend.entity.User;
import com.cloud.backend.enums.FriendRequestStatus;
import lombok.Data;

/** 好友请求项 —— 请求记录 + 对方（发起方）资料 */
@Data
public class FriendRequestResponse {

    private Long requestId;
    private Long fromUserId;
    private String fromUsername;
    private String fromNickname;
    private String fromAvatar;
    private FriendRequestStatus status;
    private String createdAt;

    public static FriendRequestResponse from(FriendRequest request, User fromUser) {
        FriendRequestResponse response = new FriendRequestResponse();
        response.setRequestId(request.getId());
        response.setFromUserId(request.getFromUserId());
        response.setStatus(request.getStatus());
        response.setCreatedAt(request.getCreatedAt() == null ? null : request.getCreatedAt().toString());
        if (fromUser != null) {
            response.setFromUsername(fromUser.getUsername());
            response.setFromNickname(fromUser.getNickname());
            response.setFromAvatar(fromUser.getAvatar());
        }
        return response;
    }
}
