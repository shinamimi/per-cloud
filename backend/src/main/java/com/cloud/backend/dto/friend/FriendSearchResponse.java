package com.cloud.backend.dto.friend;

import lombok.Data;

/**
 * 搜索结果项 —— 加好友搜索用户。
 * relation：SELF（自己，不可加）/ FRIEND（已是好友）/ PENDING_SENT（已发请求待接受）/
 * PENDING_RECEIVED（对方已向你发请求）/ NONE（可添加）。
 */
@Data
public class FriendSearchResponse {

    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private String relation;
}
