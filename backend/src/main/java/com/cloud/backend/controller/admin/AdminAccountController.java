package com.cloud.backend.controller.admin;

import com.cloud.backend.authorization.AuthorizationPolicy;
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

/**
 * 管理员账号管理控制器（后台）—— 管理员列表、创建、删除、角色调整、候选用户穿梭器。
 *
 * 设计思路：
 * 1. 列表按当前操作者权限分级展示：OPERATOR 可见运营人员，超级管理员额外可见 ADMIN
 * 2. 创建/删除/改角色等高危操作集中在服务层做权限与自我保护校验（不能操作自己/超管）
 * 3. 响应统一组装为 AdminUserResponse，配额字段含总配额（基础 + 赠送 + 奖励）
 */
@RestController
@RequestMapping("/api/admin/admins")
public class AdminAccountController {

    private final UserService userService;

    public AdminAccountController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 管理员列表：运营人员可见 OPERATOR，超级管理员额外可见 ADMIN；按权限过滤展示。
     */
    @GetMapping
    public Result<List<AdminUserResponse>> listAdmins(@AuthenticationPrincipal LoginUser loginUser) {
        boolean isSuperAdmin = AuthorizationPolicy.isSuperAdmin(loginUser);
        List<AdminUserResponse> admins = userService.findAll().stream()
                .filter(u -> u.getRole() == Role.OPERATOR || (isSuperAdmin && u.getRole() == Role.ADMIN))
                .map(u -> new AdminUserResponse(u.getId(), u.getUsername(), u.getEmail(),
                        u.getNickname(), u.getAvatar(), u.getRole(), u.getQuota(),
                        userService.calculateTotalQuota(u), u.getAdminBonusQuota(), u.getRewardQuota(),
                        u.getUsedSpace(), u.getIsVip(), u.getStatus(), u.getCreatedAt()))
                .toList();
        return Result.success(admins);
    }

    /**
     * 创建管理员（OPERATOR / ADMIN），服务层校验角色可授予范围（超管角色不可创建）。
     */
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

    /**
     * 删除管理员（逻辑禁用），服务层拦截删除自己与超级管理员。
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteAdmin(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        userService.deleteAdmin(id, loginUser.getUserId());
        return Result.success();
    }

    /**
     * 修改管理员角色，服务层拦截授予超管角色与修改自己。
     */
    @PutMapping("/{id}/role")
    public Result<Void> updateRole(@PathVariable Long id, @RequestBody UpdateRoleRequest request) {
        userService.updateAdminRole(id, request.getRole());
        return Result.success();
    }

    /**
     * 候选用户列表（排除已管理员）—— 供穿梭器左列使用
     */
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

    /**
     * 批量变更角色 —— 请求体为变更项数组，降级也传目标角色（USER）
     */
    @PutMapping("/batch")
    public Result<Void> batchUpdateRole(@Valid @RequestBody List<RoleChangeRequest> changes) {
        userService.batchUpdateAdminRole(changes);
        return Result.success();
    }
}
