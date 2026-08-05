package com.cloud.backend.security;

import com.cloud.backend.entity.User;
import com.cloud.backend.enums.Role;
import com.cloud.backend.enums.UserStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 当前登录用户的信息封装 —— 实现 UserDetails 供 Spring Security 使用。
 *
 * 设计思路：
 * 1. 从 User 实体构造，只保留 Security 需要的字段，不持有整个 User 对象
 * 2. getAuthorities() 将角色枚举映射为 Spring Security 的 ROLE_ 格式权限字符串
 *    如 Role.ADMIN → "ROLE_ADMIN"，对应 SecurityConfig 中的 .hasRole("ADMIN")
 *    Spring Security 的 hasRole() 会自动拼接 "ROLE_" 前缀，所以配置里写 "ADMIN" 即可
 * 3. isEnabled() 返回 true 仅当用户状态为 NORMAL，被冻结的用户即使 Token 有效也无法操作
 *
 * 修改指引：
 * - 【习惯】修改角色 → 权限字符串映射   → getAuthorities() 中的 switch；改动后影响 SecurityConfig 中 hasRole("ADMIN") 等
 *                              权限判断与接口放行
 * - 【习惯】修改字段（新增/删减）      → 字段声明 + 构造器 LoginUser(User) + @Getter 生成 getter；改动后影响各 Controller
 *                              /Service 取用当前用户信息，权限判断需联动
 * - 【习惯】修改冻结用户是否可用       → isEnabled()；目前仅 NORMAL 可用，改动后影响被冻结用户能否操作
 * - 【习惯】修改账号/凭证过期锁定判断   → isAccountNonExpired() / isAccountNonLocked() / isCredentialsNonExpired()；
 *                              目前恒为 true，改动后影响对应状态用户能否通过认证
 */
@Getter
public class LoginUser implements UserDetails {

    private final Long userId;
    private final String username;
    private final String password;
    private final Role role;
    private final UserStatus status;

    public LoginUser(User user) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.role = user.getRole();
        this.status = user.getStatus();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String roleName = switch (role) {
            case SUPER_ADMIN -> "ROLE_SUPER_ADMIN";
            case ADMIN -> "ROLE_ADMIN";
            case OPERATOR -> "ROLE_OPERATOR";
            default -> "ROLE_USER";
        };
        return List.of(new SimpleGrantedAuthority(roleName));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.NORMAL;
    }
}