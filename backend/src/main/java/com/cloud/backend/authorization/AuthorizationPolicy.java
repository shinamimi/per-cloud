package com.cloud.backend.authorization;

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
                && loginUser.getRole().getValue() >= com.cloud.backend.enums.Role.ADMIN.getValue();
    }
}
