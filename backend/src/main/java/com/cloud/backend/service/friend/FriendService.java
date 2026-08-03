package com.cloud.backend.service.friend;

import com.cloud.backend.dto.friend.FriendRequestResponse;
import com.cloud.backend.dto.friend.FriendSearchResponse;
import com.cloud.backend.dto.friend.FriendUserResponse;

import java.util.List;

/**
 * 好友服务 —— 双向确认 + 独立通用关系层。
 * 独立于团队，供团队拉人、定向分享复用。
 */
public interface FriendService {

    /** 好友列表 */
    List<FriendUserResponse> listFriends(Long userId);

    /** 搜索用户（用户名/邮箱前缀），标注与当前用户的关系 */
    List<FriendSearchResponse> search(Long userId, String keyword);

    /** 发送好友请求（不能加自己/已是好友/已有待处理请求时拒绝） */
    void sendRequest(Long userId, Long toUserId);

    /** 收到的待处理请求列表 */
    List<FriendRequestResponse> listPendingRequests(Long userId);

    /** 接受请求：置 ACCEPTED + 建立好友关系 */
    void accept(Long userId, Long requestId);

    /** 拒绝请求：置 REJECTED（对方可重新发送） */
    void reject(Long userId, Long requestId);

    /** 删除好友（单向解除） */
    void deleteFriend(Long userId, Long friendUserId);

    /** 关系校验（定向分享/团队拉人复用）：目标与自己是好友或同团队成员 */
    boolean isFriendOrTeamMate(Long userId, Long targetUserId);
}
