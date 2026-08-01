package com.cloud.backend.dto.team;

import lombok.Data;

import jakarta.validation.constraints.Size;

/** 更新团队信息 —— 入参 */
@Data
public class TeamUpdateRequest {

    @Size(max = 64, message = "团队名称最长 64 字符")
    private String name;

    @Size(max = 512, message = "团队描述最长 512 字符")
    private String description;

    private String avatar;
}
