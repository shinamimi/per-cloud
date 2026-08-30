package com.cloud.backend.dto.team;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class TeamInviteRequest {

    @NotEmpty(message = "请选择要邀请的成员")
    private List<Long> userIds;
}
