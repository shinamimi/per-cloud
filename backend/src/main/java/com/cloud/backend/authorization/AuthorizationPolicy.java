package com.cloud.backend.authorization;

import com.cloud.backend.entity.User;
import com.cloud.backend.enums.ErrorCode;
import com.cloud.backend.enums.Role;
import com.cloud.backend.exception.BusinessException;
import com.cloud.backend.security.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuthorizationPolicy {

    public static Long getCurrentUserId() {
        LoginUser loginUser = getCurrentUser();
        return loginUser != null ? loginUser.getUserId() : null;
    }

    public static LoginUser getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        return null;
    }

    public static boolean isAdmin(LoginUser loginUser) {
        return loginUser != null && loginUser.getRole() != null
                && loginUser.getRole().getValue() >= Role.ADMIN.getValue();
    }

    public static boolean isSuperAdmin(LoginUser loginUser) {
        return loginUser != null && loginUser.getRole() == Role.SUPER_ADMIN;
    }

    public static void canManageUser(User targetUser) {
        if (targetUser.getRole() == Role.ADMIN || targetUser.getRole() == Role.SUPER_ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "不能操作管理员账号");
        }
    }
}
