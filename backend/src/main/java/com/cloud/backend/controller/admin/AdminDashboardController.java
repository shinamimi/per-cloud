package com.cloud.backend.controller.admin;

import com.cloud.backend.dto.Result;
import com.cloud.backend.entity.File;
import com.cloud.backend.entity.User;
import com.cloud.backend.service.file.FileService;
import com.cloud.backend.service.user.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final UserService userService;
    private final FileService fileService;

    public AdminDashboardController(UserService userService, FileService fileService) {
        this.userService = userService;
        this.fileService = fileService;
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        List<User> users = userService.findAll();
        List<File> files = fileService.findAll();

        long userCount = users.size();
        long fileCount = files.size();
        long totalSize = files.stream().mapToLong(File::getSize).sum();
        long totalQuota = users.stream().mapToLong(User::getQuota).sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("userCount", userCount);
        stats.put("fileCount", fileCount);
        stats.put("totalSize", totalSize);
        stats.put("totalQuota", totalQuota);
        stats.put("usagePercent", totalQuota > 0 ? (double) totalSize / totalQuota * 100 : 0);
        return Result.success(stats);
    }
}
