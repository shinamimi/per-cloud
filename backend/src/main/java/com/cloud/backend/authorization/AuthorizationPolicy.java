package com.cloud.backend.authorization;

import com.cloud.backend.entity.User;
import com.cloud.backend.enums.ErrorCode;
import com.cloud.backend.enums.Role;
import com.cloud.backend.exception.BusinessException;
import com.cloud.backend.security.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 业务权限校验工具 —— 从 Spring Security 上下文读取当前登录用户，并提供角色判断与目标校验。
 *
 * 设计思路：
 * 1. 集中管理权限判断，避免各层重复读取 SecurityContextHolder
 * 2. 角色比较基于枚举 value 大小（USER=0 < OPERATOR=10 < ADMIN=20 < SUPER_ADMIN=100），
 *    而非 ordinal（声明顺序），保证新增角色时大小关系语义稳定
 * 3. 管理操作前统一调用 canManageUser 拦截对管理员账号的操作
 *
 * 修改指引：
 * - 【习惯】新增权限判断方法        → 在类中新增静态方法；集中管理 SecurityContextHolder 读取与角色判断
 * - 【统一】修改角色大小关系        → isAdmin 中 value &gt;= ADMIN 判断；角色值定义于 enums/Role.java（USER=0/OPERATOR=10/ADMIN=20/SUPER_ADMIN=100）；
 *                             改后需同步 enums/Role.java 角色 value 定义与权限判断（isAdmin/isSuperAdmin/canManageUser）
 * - 【统一】修改管理操作拦截规则    → canManageUser；当前禁止操作 ADMIN / SUPER_ADMIN 账号，改动影响管理端操作安全；
 *                             改后需同步 enums/Role.java 角色定义与新增角色的权限拦截
 * - 【统一】修改当前用户获取逻辑    → getCurrentUser / getCurrentUserId；principal 类型为 LoginUser，改动需同步认证流程；
 *                             改后需同步认证流程（Security 过滤器设置 principal）
 */
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
