package com.cloud.backend.dto.team;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 邀请成员 —— 入参（可从好友列表勾选，非强制好友）
 *
 * 修改指引：
 * - 【习惯】修改 userIds         → List&lt;Long&gt; userIds；被邀请用户 id 列表，请求体字段名对应 POST /api/teams/{id}/members 入参；
 *                         非强制好友，受邀用户直接成为成员
 * - 【习惯】修改校验注解 @NotEmpty → 空列表直接 400，前端需保证至少勾选一位成员；重复邀请/已存在成员由服务层处理
 */
@Data
public class TeamInviteRequest {

    @NotEmpty(message = "请选择要邀请的成员")
    private List<Long> userIds;
}
