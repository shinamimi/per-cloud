package com.cloud.backend.dto.team;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** 邀请成员 —— 入参（可从好友列表勾选，非强制好友） */
@Data
public class TeamInviteRequest {

    @NotEmpty(message = "请选择要邀请的成员")
    private List<Long> userIds;
}
