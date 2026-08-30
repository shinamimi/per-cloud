package com.cloud.backend.authorization;

import com.cloud.backend.entity.User;
import com.cloud.backend.enums.ErrorCode;
import com.cloud.backend.enums.Role;
import com.cloud.backend.exception.BusinessException;
import com.cloud.backend.security.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuthorizationPolicy {

    /**
     * 获取当前登录用户 ID；未登录返回 null（不抛异常）。
     */
    public static Long getCurrentUserId() {
        LoginUser loginUser = getCurrentUser();
        return loginUser != null ? loginUser.getUserId() : null;
    }

    /**
     * 从安全上下文获取当前登录用户封装；认证信息缺失或 principal 不是 LoginUser 时返回 null。
     */
    public static LoginUser getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        return null;
    }

    /**
     * 判断是否管理员及以上（value >= ADMIN），OPERATOR 不算管理员。
     */
    public static boolean isAdmin(LoginUser loginUser) {
        return loginUser != null && loginUser.getRole() != null
                && loginUser.getRole().getValue() >= Role.ADMIN.getValue();
    }

    /**
     * 判断是否超级管理员。
     */
    public static boolean isSuperAdmin(LoginUser loginUser) {
        return loginUser != null && loginUser.getRole() == Role.SUPER_ADMIN;
    }

    /**
     * 管理操作前置校验：目标用户是管理员/超级管理员时拒绝操作。
     * 前置条件：targetUser 非空；校验失败抛 FORBIDDEN 业务异常。
     */
    public static void canManageUser(User targetUser) {
        if (targetUser.getRole() == Role.ADMIN || targetUser.getRole() == Role.SUPER_ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "不能操作管理员账号");
        }
    }
}
