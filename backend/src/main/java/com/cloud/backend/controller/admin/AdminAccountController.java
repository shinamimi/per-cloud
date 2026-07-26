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

@RestController
@RequestMapping("/api/admin/admins")
public class AdminAccountController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public Result<List<User>> listAdmins() {
        List<User> all = userService.findAll();
        List<User> admins = all.stream()
                .filter(u -> u.getRole().getValue() >= Role.ADMIN.getValue())
                .toList();
        return Result.success(admins);
    }

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