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

/**
 * 后台用户管理控制器 —— 用户列表（排除超级管理员）、状态调整、配额调整、解锁、重置密码。
 *
 * 设计思路：
 * 1. 列表始终排除超级管理员账号，防止普通管理员看到/操作超管
 * 2. 状态/配额/密码等敏感操作全部下沉到服务层，统一做目标校验（不能操作管理员账号）
 * 3. 配额调整走管理端赠送额度（adminBonusQuota），基础配额与奖励配额不可被后台直接修改
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 用户列表（排除超级管理员，含总配额/已用空间等展示字段）。
     */
    @GetMapping
    public Result<List<AdminUserResponse>> listUsers() {
        List<AdminUserResponse> users = userService.findAll().stream()
                .filter(u -> u.getRole() != Role.SUPER_ADMIN)
                .map(u -> new AdminUserResponse(u.getId(), u.getUsername(), u.getEmail(),
                        u.getNickname(), u.getAvatar(), u.getRole(), u.getQuota(),
                        userService.calculateTotalQuota(u), u.getAdminBonusQuota(), u.getRewardQuota(),
                        u.getUsedSpace(), u.getIsVip(), u.getStatus(), u.getCreatedAt()))
                .toList();
        return Result.success(users);
    }

    /**
     * 启用/禁用/锁定用户，服务层拦截对管理员账号的操作。
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody StatusRequest request) {
        userService.updateUserStatus(id, request.getStatus());
        return Result.success();
    }

    /**
     * 调整用户配额（管理端赠送额度），服务层拦截对管理员账号的操作。
     */
    @PutMapping("/{id}/quota")
    public Result<Void> updateQuota(@PathVariable Long id, @RequestBody QuotaRequest request) {
        userService.updateUserQuota(id, request.getAdminBonusQuota());
        return Result.success();
    }

    /**
     * 解锁被登录锁定（LOCKED）的用户，并清零其登录失败计数。
     */
    @PutMapping("/{id}/unlock")
    public Result<Void> unlock(@PathVariable Long id) {
        userService.unlockUser(id);
        return Result.success();
    }

    /**
     * 重置用户密码（新密码需满足长度与复杂度校验），记录操作日志。
     */
    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id,
                                      @Valid @RequestBody AdminResetPasswordRequest request) {
        userService.resetUserPassword(id, request.getNewPassword());
        return Result.success();
    }
}
