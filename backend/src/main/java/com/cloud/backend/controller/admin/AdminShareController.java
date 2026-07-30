package com.cloud.backend.controller.admin;

import com.cloud.backend.dto.Result;
import com.cloud.backend.dto.admin.AdminShareResponse;
import com.cloud.backend.service.share.ShareService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/shares")
public class AdminShareController {

    private final ShareService shareService;

    public AdminShareController(ShareService shareService) {
        this.shareService = shareService;
    }

    @GetMapping
    public Result<List<AdminShareResponse>> listShares() {
        List<AdminShareResponse> shares = shareService.findAll().stream()
                .map(s -> new AdminShareResponse(s.getId(), s.getUserId(), s.getFileId(),
                        s.getShareToken(), s.getStatus(), s.getExpireTime(), s.getMaxDownload(),
                        s.getDownloadCount(), s.getCreatedAt(), s.getUpdatedAt()))
                .toList();
        return Result.success(shares);
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancelShare(@PathVariable Long id) {
        shareService.adminCancelShare(id);
        return Result.success();
    }
}
