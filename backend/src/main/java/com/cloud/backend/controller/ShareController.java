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
 * 创建分享、列表、修改有效期、取消分享。
 * 创建入口在文件列表（单文件/文件夹），访客访问走 GuestShareController。
 *
 * 修改指引：
 * - 【习惯】创建分享           → POST /api/shares，调 shareService.createShare(当前用户, request)；单文件 + 文件夹快照锁定，
 *                        创建后访客通过 /api/shares/access/** 访问
 * - 【习惯】我的分享列表        → GET /api/shares，调 shareService.listShares(当前用户)
 * - 【习惯】修改有效期         → PUT /api/shares/{id}，调 shareService.updateExpire(当前用户, id, request)；可延长/缩短/永久
 * - 【习惯】取消分享           → DELETE /api/shares/{id}，调 shareService.cancelShare；状态置 CANCELED 保留记录
 * - 【习惯】删除分享记录       → DELETE /api/shares/{id}/record，调 shareService.deleteShareRecord；物理删除彻底移除
 * - 【习惯】新增/修改接口       → 在 @RequestMapping("/api/shares") 下新增；需登录，若为公开接口须在 SecurityConfig 放行
 *                        （注意勿与 /api/shares/access/** 白名单前缀冲突）并同步前端 API 层
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

    /** 取消分享（状态置 CANCELED，保留记录） */
    @DeleteMapping("/{id}")
    public Result<Void> cancelShare(@PathVariable Long id) {
        shareService.cancelShare(AuthorizationPolicy.getCurrentUserId(), id);
        return Result.success();
    }

    /** 删除分享记录（物理删除，彻底移除） */
    @DeleteMapping("/{id}/record")
    public Result<Void> deleteShareRecord(@PathVariable Long id) {
        shareService.deleteShareRecord(AuthorizationPolicy.getCurrentUserId(), id);
        return Result.success();
    }
}
