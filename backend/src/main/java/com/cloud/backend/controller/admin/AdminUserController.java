package com.cloud.backend.controller.admin;

import com.cloud.backend.dto.Result;
import com.cloud.backend.dto.admin.AdminResetPasswordRequest;
import com.cloud.backend.dto.admin.AdminUserResponse;
import com.cloud.backend.dto.admin.QuotaRequest;
import com.cloud.backend.dto.admin.StatusRequest;
import com.cloud.backend.enums.Role;
import com.cloud.backend.service.user.UserService;
import jakarta.validation.Valid;
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
    public Result<List<AdminUserResponse>> listUsers() {
        List<AdminUserResponse> users = userService.findAll().stream()
                .filter(u -> u.getRole() != Role.ADMIN && u.getRole() != Role.SUPER_ADMIN)
                .map(u -> new AdminUserResponse(u.getId(), u.getUsername(), u.getEmail(),
                        u.getNickname(), u.getAvatar(), u.getRole(), u.getQuota(),
                        userService.calculateTotalQuota(u), u.getAdminBonusQuota(), u.getRewardQuota(),
                        u.getUsedSpace(), u.getIsVip(), u.getStatus(), u.getCreatedAt()))
                .toList();
        return Result.success(users);
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody StatusRequest request) {
        userService.updateUserStatus(id, request.getStatus());
        return Result.success();
    }

    @PutMapping("/{id}/quota")
    public Result<Void> updateQuota(@PathVariable Long id, @RequestBody QuotaRequest request) {
        userService.updateUserQuota(id, request.getAdminBonusQuota());
        return Result.success();
    }

    @PutMapping("/{id}/unlock")
    public Result<Void> unlock(@PathVariable Long id) {
        userService.unlockUser(id);
        return Result.success();
    }

    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id,
                                      @Valid @RequestBody AdminResetPasswordRequest request) {
        userService.resetUserPassword(id, request.getNewPassword());
        return Result.success();
    }
}
