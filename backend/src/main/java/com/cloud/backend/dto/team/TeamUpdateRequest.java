package com.cloud.backend.dto.team;

import lombok.Data;

import jakarta.validation.constraints.Size;

/**
 * 更新团队信息 —— 入参
 *
 * 修改指引：
 * - 【习惯】修改 name            → String name；团队名称，可空（不传则不更新），@Size 上限 64 字符
 * - 【习惯】修改 description     → String description；团队描述，可空，@Size 上限 512 字符
 * - 【习惯】修改 avatar          → String avatar；团队头像，可空
 * - 【习惯】修改校验注解 @Size   → 改上限会影响前端表单 maxlength 与接口契约；PUT /api/teams/{id} 仅更新传入字段
 */
@Data
public class TeamUpdateRequest {

    @Size(max = 64, message = "团队名称最长 64 字符")
    private String name;

    @Size(max = 512, message = "团队描述最长 512 字符")
    private String description;

    private String avatar;
}
