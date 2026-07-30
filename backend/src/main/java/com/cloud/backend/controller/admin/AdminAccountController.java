package com.cloud.backend.controller.admin;

import com.cloud.backend.dto.Result;
import com.cloud.backend.dto.admin.AdminUserResponse;
import com.cloud.backend.dto.admin.CreateAdminRequest;
import com.cloud.backend.dto.admin.UpdateRoleRequest;
import com.cloud.backend.enums.Role;
import com.cloud.backend.security.LoginUser;
import com.cloud.backend.service.user.UserService;
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
                .map(u -> new AdminUserResponse(u.getId(), u.getUsername(), u.getEmail(),
                        u.getNickname(), u.getAvatar(), u.getRole(), u.getQuota(),
                        u.getUsedSpace(), u.getStatus(), u.getCreatedAt()))
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
                user.getUsedSpace(), user.getStatus(), user.getCreatedAt()));
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
}
