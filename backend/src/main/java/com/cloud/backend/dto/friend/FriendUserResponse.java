package com.cloud.backend.dto.friend;

import com.cloud.backend.entity.User;
import lombok.Data;

/** 好友列表项 —— 携带用户基本资料 */
@Data
public class FriendUserResponse {

    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private String email;

    public static FriendUserResponse from(User user) {
        FriendUserResponse response = new FriendUserResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setAvatar(user.getAvatar());
        response.setEmail(user.getEmail());
        return response;
    }
}
