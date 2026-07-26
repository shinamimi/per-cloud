package com.cloud.backend.controller.admin;

import com.cloud.backend.dto.Result;
import com.cloud.backend.entity.User;
import com.cloud.backend.enums.UserStatus;
import com.cloud.backend.service.LoginAttemptService;
import com.cloud.backend.service.UserService;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserService userService;
    private final LoginAttemptService loginAttemptService;

    public AdminUserController(UserService userService, LoginAttemptService loginAttemptService) {
        this.userService = userService;
        this.loginAttemptService = loginAttemptService;
    }

    @GetMapping
    public Result<List<User>> listUsers() {
        return Result.success(userService.findAll());
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody StatusRequest request) {
        User user = userService.findById(id);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        user.setStatus(request.getStatus());
        userService.update(user);
        return Result.success();
    }

    @PutMapping("/{id}/quota")
    public Result<Void> updateQuota(@PathVariable Long id, @RequestBody QuotaRequest request) {
        User user = userService.findById(id);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        user.setQuota(request.getQuota());
        userService.update(user);
        return Result.success();
    }

    @PutMapping("/{id}/unlock")
    public Result<Void> unlock(@PathVariable Long id) {
        User user = userService.findById(id);
        if (user == null) {
            return Result.fail("用户不存在");
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