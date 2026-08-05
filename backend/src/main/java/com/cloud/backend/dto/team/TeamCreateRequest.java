package com.cloud.backend.dto.team;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建团队 —— 入参
 *
 * 修改指引：
 * - 【习惯】修改 name            → String name；团队名称，必填，@Size 上限 64 字符；请求体字段名对应 POST /api/teams 入参
 * - 【习惯】修改 description     → String description；团队描述，可空，@Size 上限 512 字符
 * - 【习惯】修改 avatar          → String avatar；团队头像
 * - 【习惯】修改校验注解 @NotBlank/@Size → 改上限会影响前端表单 maxlength 与接口契约；空名校验改动影响必填契约
 */
@Data
public class TeamCreateRequest {

    @NotBlank(message = "团队名称不能为空")
    @Size(max = 64, message = "团队名称最长 64 字符")
    private String name;

    @Size(max = 512, message = "团队描述最长 512 字符")
    private String description;

    private String avatar;
}
