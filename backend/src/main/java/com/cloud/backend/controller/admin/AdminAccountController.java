package com.cloud.backend.controller.admin;

import com.cloud.backend.dto.Result;
import com.cloud.backend.entity.User;
import com.cloud.backend.enums.Role;
import com.cloud.backend.enums.UserStatus;
import com.cloud.backend.security.LoginUser;
import com.cloud.backend.service.UserService;
import lombok.Data;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员账号管理控制器 —— CRUD 管理员/运营人员。
 *
 * 路由权限：/api/admin/admins/** 需要 SUPER_ADMIN 角色。
 * 超级管理员可以创建/删除/修改管理员和运营人员的角色。
 */
@RestController
@RequestMapping("/api/admin/admins")
public class AdminAccountController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    /** 列出所有管理员和运营人员（角色 >= ADMIN） */
    @GetMapping
    public Result<List<User>> listAdmins() {
        List<User> all = userService.findAll();
        List<User> admins = all.stream()
                .filter(u -> u.getRole().getValue() >= Role.ADMIN.getValue())
                .toList();
        return Result.success(admins);
    }

    /** 创建管理员/运营人员 */
    @PostMapping
    public Result<User> createAdmin(@RequestBody CreateAdminRequest request) {
        if (userService.existsByUsername(request.getUsername())) {
            return Result.fail("用户名已存在");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setNickname(request.getNickname());
        user.setRole(request.getRole());
        user.setStatus(UserStatus.NORMAL);
        userService.register(user);
        return Result.success(user);
    }

    /** 删除管理员（禁用账号）—— 不允许删除自己或超级管理员 */
    @DeleteMapping("/{id}")
    public Result<Void> deleteAdmin(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        if (id.equals(loginUser.getUserId())) {
            return Result.fail("不能删除自己");
        }
        User user = userService.findById(id);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        if (user.getRole() == Role.SUPER_ADMIN) {
            return Result.fail("不能删除超级管理员");
        }
        user.setStatus(UserStatus.DISABLED);
        userService.update(user);
        return Result.success();
    }

    /** 修改角色（如将 ADMIN 降级为 OPERATOR） */
    @PutMapping("/{id}/role")
    public Result<Void> updateRole(@PathVariable Long id, @RequestBody UpdateRoleRequest request) {
        User user = userService.findById(id);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        user.setRole(request.getRole());
        userService.update(user);
        return Result.success();
    }

    @Data
    public static class CreateAdminRequest {
        private String username;
        private String password;
        private String email;
        private String nickname;
        private Role role;
    }

    @Data
    public static class UpdateRoleRequest {
        private Role role;
    }
}