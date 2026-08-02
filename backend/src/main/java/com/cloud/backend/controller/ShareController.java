package com.cloud.backend.controller;

import com.cloud.backend.authorization.AuthorizationPolicy;
import com.cloud.backend.dto.Result;
import com.cloud.backend.dto.share.ShareCreateRequest;
import com.cloud.backend.dto.share.ShareResponse;
import com.cloud.backend.dto.share.ShareUpdateRequest;
import com.cloud.backend.entity.Share;
import com.cloud.backend.service.share.ShareService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 我的分享 —— /api/shares（需登录）。
 * 创建分享、列表、修改有效期、取消分享（docs/share-module.md §七）。
 * 创建入口在文件列表（单文件/文件夹），访客访问走 GuestShareController。
 */
@RestController
@RequestMapping("/api/shares")
public class ShareController {

    private final ShareService shareService;

    public ShareController(ShareService shareService) {
        this.shareService = shareService;
    }

    /** 创建分享（单文件 + 文件夹快照锁定） */
    @PostMapping
    public Result<ShareResponse> createShare(@RequestBody ShareCreateRequest request) {
        Share share = shareService.createShare(AuthorizationPolicy.getCurrentUserId(), request);
        return Result.success(ShareResponse.from(share));
    }

    /** 我的分享列表 */
    @GetMapping
    public Result<List<ShareResponse>> listShares() {
        return Result.success(shareService.listShares(AuthorizationPolicy.getCurrentUserId()));
    }

    /** 修改有效期（延长/缩短/永久） */
    @PutMapping("/{id}")
    public Result<Void> updateExpire(@PathVariable Long id, @RequestBody ShareUpdateRequest request) {
        shareService.updateExpire(AuthorizationPolicy.getCurrentUserId(), id, request);
        return Result.success();
    }

    /** 取消分享 */
    @DeleteMapping("/{id}")
    public Result<Void> cancelShare(@PathVariable Long id) {
        shareService.cancelShare(AuthorizationPolicy.getCurrentUserId(), id);
        return Result.success();
    }
}
