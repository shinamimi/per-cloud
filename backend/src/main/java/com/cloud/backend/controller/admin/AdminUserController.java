package com.cloud.backend.controller.admin;

import com.cloud.backend.dto.Result;
import com.cloud.backend.entity.User;
import com.cloud.backend.enums.ErrorCode;
import com.cloud.backend.enums.UserStatus;
import com.cloud.backend.service.LoginAttemptService;
import com.cloud.backend.service.UserService;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理控制器 —— 管理员对普通用户的操作。
 *
 * 路由权限：/api/admin/users/** 需要 ADMIN 或以上角色。
 * 支持：查看用户列表、禁用/启用用户、修改配额、解锁登录锁定。
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserService userService;
    private final LoginAttemptService loginAttemptService;

    public AdminUserController(UserService userService, LoginAttemptService loginAttemptService) {
        this.userService = userService;
        this.loginAttemptService = loginAttemptService;
    }

    /** 用户列表 */
    @GetMapping
    public Result<List<User>> listUsers() {
        return Result.success(userService.findAll());
    }

    /** 修改用户状态（禁用/启用） */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody StatusRequest request) {
        User user = userService.findById(id);
        if (user == null) {
            return Result.fail(ErrorCode.USER_NOT_FOUND);
        }
        user.setStatus(request.getStatus());
        userService.update(user);
        return Result.success();
    }

    /** 修改用户空间配额 */
    @PutMapping("/{id}/quota")
    public Result<Void> updateQuota(@PathVariable Long id, @RequestBody QuotaRequest request) {
        User user = userService.findById(id);
        if (user == null) {
            return Result.fail(ErrorCode.USER_NOT_FOUND);
        }
        user.setQuota(request.getQuota());
        userService.update(user);
        return Result.success();
    }

    /** 解锁用户登录锁定（清除 Redis 中的失败计数） */
    @PutMapping("/{id}/unlock")
    public Result<Void> unlock(@PathVariable Long id) {
        User user = userService.findById(id);
        if (user == null) {
            return Result.fail(ErrorCode.USER_NOT_FOUND);
        }
        loginAttemptService.loginSucceeded(user.getUsername());
        return Result.success();
    }

    @Data
    public static class StatusRequest {
        private UserStatus status;
    }

    @Data
    public static class QuotaRequest {
        private Long quota;
    }
}