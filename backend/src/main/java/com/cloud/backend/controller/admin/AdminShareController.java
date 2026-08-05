package com.cloud.backend.controller.admin;

import com.cloud.backend.dto.Result;
import com.cloud.backend.dto.admin.AdminShareDownloadRequest;
import com.cloud.backend.dto.admin.AdminShareResponse;
import com.cloud.backend.entity.Share;
import com.cloud.backend.mapper.FileMapper;
import com.cloud.backend.mapper.UserMapper;
import com.cloud.backend.service.share.ShareService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 后台分享管理控制器 —— 查看全部分享、取消分享、切换下载开关、物理删除分享记录。
 *
 * 设计思路：
 * 1. 列表查询后补充展示性字段（分享者昵称、文件名），供后台表格直接展示
 * 2. 管理端操作（取消/删除/开关）为强制性治理动作，服务层不做归属校验
 *
 * 修改指引：
 * - 【习惯】分享列表           → GET /api/admin/shares，调 shareService.findAll 并补充分享者昵称/文件名
 *                        （对象已删除时保持为空）；权限 OPERATOR+（SecurityConfig /api/admin/**）
 * - 【习惯】取消分享           → POST /api/admin/shares/{id}/cancel，调 adminCancelShare；立即失效分享链接
 * - 【习惯】切换下载开关       → PUT /api/admin/shares/{id}/download，调 adminSetAllowDownload(allowDownload)
 * - 【习惯】删除分享记录       → DELETE /api/admin/shares/{id}/record，调 adminDeleteShare；物理删除
 * - 【习惯】新增/修改接口       → 管理端操作为强制性治理动作（服务层 adminXxx 方法不做归属校验），改动需注意治理边界；
 *                        注意 SecurityConfig 权限级别并同步前端管理端 API 层
 */
@RestController
@RequestMapping("/api/admin/shares")
public class AdminShareController {

    private final ShareService shareService;
    private final UserMapper userMapper;
    private final FileMapper fileMapper;

    public AdminShareController(ShareService shareService, UserMapper userMapper, FileMapper fileMapper) {
        this.shareService = shareService;
        this.userMapper = userMapper;
        this.fileMapper = fileMapper;
    }

    /**
     * 查询全部分享记录，并补充分享者昵称与文件名（对象可能已被删除，缺失时保持为空）。
     */
    @GetMapping
    public Result<List<AdminShareResponse>> listShares() {
        List<AdminShareResponse> shares = shareService.findAll().stream()
                .map(AdminShareResponse::from)
                .peek(share -> {
                    var user = userMapper.findById(share.getUserId());
                    if (user != null) {
                        share.setOwnerName(user.getNickname() != null ? user.getNickname() : user.getUsername());
                    }
                    var file = fileMapper.findById(share.getFileId());
                    if (file != null) {
                        share.setFileName(file.getName());
                    }
                })
                .toList();
        return Result.success(shares);
    }

    /**
     * 取消分享：立即失效分享链接。
     */
    @PostMapping("/{id}/cancel")
    public Result<Void> cancelShare(@PathVariable Long id) {
        shareService.adminCancelShare(id);
        return Result.success();
    }

    /**
     * 切换下载开关（allowDownload）
     */
    @PutMapping("/{id}/download")
    public Result<Void> setAllowDownload(@PathVariable Long id, @RequestBody AdminShareDownloadRequest request) {
        shareService.adminSetAllowDownload(id, request.isAllowDownload());
        return Result.success();
    }

    /**
     * 删除分享记录（物理删除）
     */
    @DeleteMapping("/{id}/record")
    public Result<Void> deleteShare(@PathVariable Long id) {
        shareService.adminDeleteShare(id);
        return Result.success();
    }
}
