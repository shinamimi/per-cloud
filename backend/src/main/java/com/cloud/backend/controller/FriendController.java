package com.cloud.backend.controller;

import com.cloud.backend.authorization.AuthorizationPolicy;
import com.cloud.backend.dto.Result;
import com.cloud.backend.dto.friend.FriendRequestCreateRequest;
import com.cloud.backend.dto.friend.FriendRequestResponse;
import com.cloud.backend.dto.friend.FriendSearchResponse;
import com.cloud.backend.dto.friend.FriendUserResponse;
import com.cloud.backend.service.friend.FriendService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 好友控制器（docs/friend-system.md §三 接口清单）。
 * 双向确认流程：发送请求 → 接受/拒绝；好友列表/搜索/删除。
 * 关系层供团队拉人、定向分享复用（isFriendOrTeamMate）。
 */
@RestController
@RequestMapping("/api/friends")
public class FriendController {

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    /** 好友列表 */
    @GetMapping
    public Result<List<FriendUserResponse>> list() {
        return Result.success(friendService.listFriends(AuthorizationPolicy.getCurrentUserId()));
    }

    /** 搜索用户（用户名/邮箱前缀） */
    @GetMapping("/search")
    public Result<List<FriendSearchResponse>> search(@RequestParam String keyword) {
        return Result.success(friendService.search(AuthorizationPolicy.getCurrentUserId(), keyword));
    }

    /** 发送好友请求 */
    @PostMapping("/requests")
    public Result<Void> sendRequest(@Valid @RequestBody FriendRequestCreateRequest request) {
        friendService.sendRequest(AuthorizationPolicy.getCurrentUserId(), request.getToUserId());
        return Result.success();
    }

    /** 待处理请求列表（收到的） */
    @GetMapping("/requests")
    public Result<List<FriendRequestResponse>> pendingRequests() {
        return Result.success(friendService.listPendingRequests(AuthorizationPolicy.getCurrentUserId()));
    }

    /** 接受请求 */
    @PutMapping("/requests/{id}/accept")
    public Result<Void> accept(@PathVariable Long id) {
        friendService.accept(AuthorizationPolicy.getCurrentUserId(), id);
        return Result.success();
    }

    /** 拒绝请求 */
    @PutMapping("/requests/{id}/reject")
    public Result<Void> reject(@PathVariable Long id) {
        friendService.reject(AuthorizationPolicy.getCurrentUserId(), id);
        return Result.success();
    }

    /** 删除好友（单向解除） */
    @DeleteMapping("/{userId}")
    public Result<Void> delete(@PathVariable Long userId) {
        friendService.deleteFriend(AuthorizationPolicy.getCurrentUserId(), userId);
        return Result.success();
    }
}
