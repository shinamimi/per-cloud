package com.cloud.backend.dto.friend;

import lombok.Data;

@Data
public class FriendSearchResponse {

    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private String relation;
}
