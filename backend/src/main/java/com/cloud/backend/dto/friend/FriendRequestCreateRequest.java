package com.cloud.backend.dto.friend;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 发送好友请求 —— 入参
 *
 * 修改指引：
 * - 【统一】修改 toUserId        → Long toUserId；目标用户 id，请求体字段名对应 POST /api/friends/requests 入参；改名需同步前端 API 层与 Service 组装
 * - 【统一】修改校验注解 @NotNull → 目标用户缺失直接 400；双向确认流程中该用户已发送过请求时服务层会拒绝重复请求；改后需同步前端必填契约
 */
@Data
public class FriendRequestCreateRequest {

    @NotNull(message = "目标用户不能为空")
    private Long toUserId;
}
