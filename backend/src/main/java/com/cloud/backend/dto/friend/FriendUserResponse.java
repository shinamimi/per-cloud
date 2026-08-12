package com.cloud.backend.dto.friend;

import com.cloud.backend.entity.User;
import lombok.Data;

/**
 * 好友列表项 —— 携带用户基本资料
 *
 * 修改指引：
 * - 【统一】修改 userId          → Long userId；好友用户 id，删除好友接口 DELETE /api/friends/{userId} 使用该值；改名需同步前端删除好友操作与 FriendService
 * - 【习惯】修改 username / nickname / avatar / email → 好友基本资料，仅展示用；改字段名需同步前端好友列表渲染
 */
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
