package com.cloud.backend.controller.admin;

import com.cloud.backend.dto.Result;
import com.cloud.backend.dto.admin.AdminUserResponse;
import com.cloud.backend.dto.admin.CreateAdminRequest;
import com.cloud.backend.dto.admin.RoleChangeRequest;
import com.cloud.backend.dto.admin.UpdateRoleRequest;
import com.cloud.backend.enums.Role;
import com.cloud.backend.security.LoginUser;
import com.cloud.backend.service.user.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/admins")
public class AdminAccountController {

    private final UserService userService;

    public AdminAccountController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Result<List<AdminUserResponse>> listAdmins() {
        List<AdminUserResponse> admins = userService.findAll().stream()
                .filter(u -> u.getRole().getValue() >= Role.ADMIN.getValue())
                .filter(u -> u.getRole() != Role.SUPER_ADMIN)
                .map(u -> new AdminUserResponse(u.getId(), u.getUsername(), u.getEmail(),
                        u.getNickname(), u.getAvatar(), u.getRole(), u.getQuota(),
                        userService.calculateTotalQuota(u), u.getAdminBonusQuota(), u.getRewardQuota(),
                        u.getUsedSpace(), u.getIsVip(), u.getStatus(), u.getCreatedAt()))
                .toList();
        return Result.success(admins);
    }

    @PostMapping
    public Result<AdminUserResponse> createAdmin(@RequestBody CreateAdminRequest request) {
        var user = userService.createAdmin(
                request.getUsername(), request.getPassword(),
                request.getEmail(), request.getNickname(), request.getRole());
        return Result.success(new AdminUserResponse(user.getId(), user.getUsername(), user.getEmail(),
                user.getNickname(), user.getAvatar(), user.getRole(), user.getQuota(),
                userService.calculateTotalQuota(user), user.getAdminBonusQuota(), user.getRewardQuota(),
                user.getUsedSpace(), user.getIsVip(), user.getStatus(), user.getCreatedAt()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteAdmin(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        userService.deleteAdmin(id, loginUser.getUserId());
        return Result.success();
    }

    @PutMapping("/{id}/role")
    public Result<Void> updateRole(@PathVariable Long id, @RequestBody UpdateRoleRequest request) {
        userService.updateAdminRole(id, request.getRole());
        return Result.success();
    }

    /** 候选用户列表（排除已管理员）—— 供穿梭器左列使用 */
    @GetMapping("/candidates")
    public Result<List<AdminUserResponse>> listCandidates() {
        List<AdminUserResponse> candidates = userService.listCandidates().stream()
                .map(u -> new AdminUserResponse(u.getId(), u.getUsername(), u.getEmail(),
                        u.getNickname(), u.getAvatar(), u.getRole(), u.getQuota(),
                        userService.calculateTotalQuota(u), u.getAdminBonusQuota(), u.getRewardQuota(),
                        u.getUsedSpace(), u.getIsVip(), u.getStatus(), u.getCreatedAt()))
                .toList();
        return Result.success(candidates);
    }

    /** 批量变更角色 —— 请求体为变更项数组，降级也传目标角色（USER） */
    @PutMapping("/batch")
    public Result<Void> batchUpdateRole(@Valid @RequestBody List<RoleChangeRequest> changes) {
        userService.batchUpdateAdminRole(changes);
        return Result.success();
    }
}
