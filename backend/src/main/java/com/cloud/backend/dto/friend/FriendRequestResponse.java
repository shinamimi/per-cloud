package com.cloud.backend.dto.friend;

import com.cloud.backend.entity.FriendRequest;
import com.cloud.backend.entity.User;
import com.cloud.backend.enums.FriendRequestStatus;
import lombok.Data;

/**
 * 好友请求项 —— 请求记录 + 对方（发起方）资料
 *
 * 修改指引：
 * - 【统一】修改 requestId       → Long requestId；好友请求记录 id，接受/拒绝接口的路径参数
 *                         （PUT /api/friends/requests/{id}/accept、/reject）；改名需同步前端接受/拒绝操作与 FriendRequestService
 * - 【习惯】修改 fromUserId / fromUsername / fromNickname / fromAvatar → 发起方资料（请求记录 + 对方资料），
 *                         仅展示用，改动影响前端请求列表渲染
 * - 【统一】修改 status          → FriendRequestStatus status；自定义枚举（enums/FriendRequestStatus.java）：
 *                         PENDING / ACCEPTED / REJECTED；待处理列表仅返回 PENDING 项，改动取值需同步状态机；改后需同步 enums/FriendRequestStatus.java 与状态流转逻辑
 * - 【习惯】修改 createdAt       → String createdAt；发起时间（LocalDateTime.toString 转字符串），
 *                         前端排序/格式化时注意是字符串类型而非时间对象
 */
@Data
public class FriendRequestResponse {

    private Long requestId;
    private Long fromUserId;
    private String fromUsername;
    private String fromNickname;
    private String fromAvatar;
    private FriendRequestStatus status;
    private String createdAt;

    public static FriendRequestResponse from(FriendRequest request, User fromUser) {
        FriendRequestResponse response = new FriendRequestResponse();
        response.setRequestId(request.getId());
        response.setFromUserId(request.getFromUserId());
        response.setStatus(request.getStatus());
        response.setCreatedAt(request.getCreatedAt() == null ? null : request.getCreatedAt().toString());
        if (fromUser != null) {
            response.setFromUsername(fromUser.getUsername());
            response.setFromNickname(fromUser.getNickname());
            response.setFromAvatar(fromUser.getAvatar());
        }
        return response;
    }
}
