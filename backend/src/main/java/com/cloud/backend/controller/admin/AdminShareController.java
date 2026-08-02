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

    @PostMapping("/{id}/cancel")
    public Result<Void> cancelShare(@PathVariable Long id) {
        shareService.adminCancelShare(id);
        return Result.success();
    }

    /** 切换下载开关（allowDownload） */
    @PutMapping("/{id}/download")
    public Result<Void> setAllowDownload(@PathVariable Long id, @RequestBody AdminShareDownloadRequest request) {
        shareService.adminSetAllowDownload(id, request.isAllowDownload());
        return Result.success();
    }

    /** 删除分享记录（物理删除） */
    @DeleteMapping("/{id}/record")
    public Result<Void> deleteShare(@PathVariable Long id) {
        shareService.adminDeleteShare(id);
        return Result.success();
    }
}
