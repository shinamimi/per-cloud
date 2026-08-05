package com.cloud.backend.service.friend.impl;

import com.cloud.backend.dto.friend.FriendRequestResponse;
import com.cloud.backend.dto.friend.FriendSearchResponse;
import com.cloud.backend.dto.friend.FriendUserResponse;
import com.cloud.backend.entity.FriendRequest;
import com.cloud.backend.entity.Friendship;
import com.cloud.backend.entity.TeamMember;
import com.cloud.backend.entity.User;
import com.cloud.backend.enums.ErrorCode;
import com.cloud.backend.enums.FriendRequestStatus;
import com.cloud.backend.exception.BusinessException;
import com.cloud.backend.mapper.FriendRequestMapper;
import com.cloud.backend.mapper.FriendshipMapper;
import com.cloud.backend.mapper.TeamMemberMapper;
import com.cloud.backend.service.friend.FriendService;
import com.cloud.backend.service.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 好友服务实现 —— 双向确认流程 + 关系层复用。
 * 规则：
 * - 不能加自己；已是好友/已有待处理请求不能重复发送；REJECTED 后可重发
 * - 接受请求时写入 t_friendship（成对存储 user_a < user_b）
 * - isFriendOrTeamMate：好友 或 同团队成员（团队拉人/定向分享复用）
 *
 * 修改指引：
 * - 【习惯】想改"好友请求发送限制（不能加自己/已是好友/已有待处理请求/REJECTED 后可重发）" → sendRequest()；
 *   改动影响好友关系建立入口与重复请求的拦截
 * - 【习惯】想改"请求状态机（PENDING/ACCEPTED/REJECTED 流转）" → sendRequest()/accept()/reject() 与
 *   FriendRequestStatus 枚举；改动影响请求处理语义，须保持状态值（TINYINT）与枚举一致
 * - 【习惯】想改"好友关系存储（成对存储 user_a < user_b）" → accept()/deleteFriend()/listFriends()/search()
 *   中 Math.min/max 归一化；改动影响成对唯一约束与查询语义
 * - 【习惯】想改"好友/团队成员判定" → isFriendOrTeamMate()：先查 t_friendship，再遍历本人正常团队（status=1）
 *   检查对方是否同团队；改动影响团队拉人/定向分享的可见范围
 * - 【习惯】事务说明：accept() 为 @Transactional（改请求状态 + 写好友关系同事务）；改动须保持原子一致
 * - 【习惯】与接口联动：本类实现 FriendService，改签名/行为须同步接口契约及 FriendController、
 *   TeamServiceImpl（团队拉人校验）等调用方
 */
@Service
public class FriendServiceImpl implements FriendService {

    private final FriendRequestMapper friendRequestMapper;
    private final FriendshipMapper friendshipMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final UserService userService;

    public FriendServiceImpl(FriendRequestMapper friendRequestMapper, FriendshipMapper friendshipMapper,
                             TeamMemberMapper teamMemberMapper, UserService userService) {
        this.friendRequestMapper = friendRequestMapper;
        this.friendshipMapper = friendshipMapper;
        this.teamMemberMapper = teamMemberMapper;
        this.userService = userService;
    }

    @Override
    public List<FriendUserResponse> listFriends(Long userId) {
        List<Friendship> friendships = friendshipMapper.findByUserId(userId);
        List<FriendUserResponse> friends = new ArrayList<>(friendships.size());
        for (Friendship friendship : friendships) {
            Long friendId = friendship.getUserAId().equals(userId)
                    ? friendship.getUserBId() : friendship.getUserAId();
            User friend = userService.findById(friendId);
            if (friend != null) {
                friends.add(FriendUserResponse.from(friend));
            }
        }
        return friends;
    }

    @Override
    public List<FriendSearchResponse> search(Long userId, String keyword) {
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isEmpty()) {
            return List.of();
        }
        List<User> candidates = userService.searchUsers(kw);
        List<FriendSearchResponse> results = new ArrayList<>(candidates.size());
        for (User user : candidates) {
            if (user.getId().equals(userId)) {
                continue; // 不能加自己
            }
            results.add(toSearchResponse(userId, user));
        }
        return results;
    }

    private FriendSearchResponse toSearchResponse(Long userId, User user) {
        FriendSearchResponse response = new FriendSearchResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setAvatar(user.getAvatar());
        response.setEmail(user.getEmail());
        if (friendshipMapper.findByPair(Math.min(userId, user.getId()), Math.max(userId, user.getId())) != null) {
            response.setRelation("FRIEND");
            return response;
        }
        FriendRequest latest = friendRequestMapper.findLatestBetween(userId, user.getId());
        if (latest != null && latest.getStatus() == FriendRequestStatus.PENDING) {
            response.setRelation(latest.getFromUserId().equals(userId) ? "PENDING_SENT" : "PENDING_RECEIVED");
            return response;
        }
        response.setRelation("NONE");
        return response;
    }

    @Override
    public void sendRequest(Long userId, Long toUserId) {
        if (toUserId == null || toUserId.equals(userId)) {
            throw new BusinessException(ErrorCode.FRIEND_CANNOT_ADD_SELF);
        }
        User toUser = userService.findById(toUserId);
        if (toUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        // 已是好友
        if (friendshipMapper.findByPair(Math.min(userId, toUserId), Math.max(userId, toUserId)) != null) {
            throw new BusinessException(ErrorCode.FRIEND_ALREADY);
        }
        // 已有待处理请求（任一方向）不可重复发送
        FriendRequest latest = friendRequestMapper.findLatestBetween(userId, toUserId);
        if (latest != null && latest.getStatus() == FriendRequestStatus.PENDING) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_PENDING);
        }
        FriendRequest request = new FriendRequest();
        request.setFromUserId(userId);
        request.setToUserId(toUserId);
        request.setStatus(FriendRequestStatus.PENDING);
        friendRequestMapper.insert(request);
    }

    @Override
    public List<FriendRequestResponse> listPendingRequests(Long userId) {
        List<FriendRequest> requests = friendRequestMapper.findPendingByToUserId(userId);
        List<FriendRequestResponse> responses = new ArrayList<>(requests.size());
        for (FriendRequest request : requests) {
            responses.add(FriendRequestResponse.from(request, userService.findById(request.getFromUserId())));
        }
        return responses;
    }

    @Override
    @Transactional
    public void accept(Long userId, Long requestId) {
        FriendRequest request = getPendingRequest(userId, requestId);
        friendRequestMapper.updateStatus(requestId, FriendRequestStatus.ACCEPTED);
        // 建立好友关系：成对存储 user_a < user_b
        long a = Math.min(request.getFromUserId(), request.getToUserId());
        long b = Math.max(request.getFromUserId(), request.getToUserId());
        if (friendshipMapper.findByPair(a, b) == null) {
            Friendship friendship = new Friendship();
            friendship.setUserAId(a);
            friendship.setUserBId(b);
            friendshipMapper.insert(friendship);
        }
    }

    @Override
    public void reject(Long userId, Long requestId) {
        FriendRequest request = getPendingRequest(userId, requestId);
        friendRequestMapper.updateStatus(requestId, FriendRequestStatus.REJECTED);
    }

    /** 校验请求存在、目标是当前用户、状态待接受 */
    private FriendRequest getPendingRequest(Long userId, Long requestId) {
        FriendRequest request = friendRequestMapper.findById(requestId);
        if (request == null || !request.getToUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND);
        }
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND, "请求已处理");
        }
        return request;
    }

    @Override
    public void deleteFriend(Long userId, Long friendUserId) {
        long a = Math.min(userId, friendUserId);
        long b = Math.max(userId, friendUserId);
        int deleted = friendshipMapper.deleteByPair(a, b);
        if (deleted == 0) {
            throw new BusinessException(ErrorCode.FRIEND_NOT_FOUND);
        }
    }

    @Override
    public boolean isFriendOrTeamMate(Long userId, Long targetUserId) {
        if (userId == null || targetUserId == null || userId.equals(targetUserId)) {
            return true;
        }
        long a = Math.min(userId, targetUserId);
        long b = Math.max(userId, targetUserId);
        if (friendshipMapper.findByPair(a, b) != null) {
            return true;
        }
        // 同团队：两用户在任一正常团队中有交集
        List<TeamMember> myTeams = teamMemberMapper.findByUserIdAndStatus(userId, 1);
        for (TeamMember member : myTeams) {
            if (teamMemberMapper.findByTeamIdAndUserId(member.getTeamId(), targetUserId) != null) {
                return true;
            }
        }
        return false;
    }
}
