package com.cloud.backend.controller.admin;

import com.cloud.backend.dto.Result;
import com.cloud.backend.dto.admin.StatusRequest;
import com.cloud.backend.dto.admin.QuotaRequest;
import com.cloud.backend.entity.User;
import com.cloud.backend.service.user.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Result<List<User>> listUsers() {
        return Result.success(userService.findAll());
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody StatusRequest request) {
        userService.updateUserStatus(id, request.getStatus());
        return Result.success();
    }

    @PutMapping("/{id}/quota")
    public Result<Void> updateQuota(@PathVariable Long id, @RequestBody QuotaRequest request) {
        userService.updateUserQuota(id, request.getQuota());
        return Result.success();
    }

    @PutMapping("/{id}/unlock")
    public Result<Void> unlock(@PathVariable Long id) {
        userService.unlockUser(id);
        return Result.success();
    }
}
