package com.cloud.backend.controller.admin;

import com.cloud.backend.dto.Result;
import com.cloud.backend.entity.Share;
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
    public Result<List<Share>> listShares() {
        return Result.success(shareService.findAll());
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancelShare(@PathVariable Long id) {
        shareService.adminCancelShare(id);
        return Result.success();
    }
}
