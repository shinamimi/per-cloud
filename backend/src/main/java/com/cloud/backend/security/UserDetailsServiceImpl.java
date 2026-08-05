package com.cloud.backend.security;

import com.cloud.backend.entity.User;
import com.cloud.backend.service.user.UserService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * UserDetailsService 实现 —— Spring Security 认证流程的数据源。
 *
 * 设计思路：
 * findByAccount 支持用户名或邮箱登录（只要唯一即可）。
 * 当登录或 JWT 过滤器中需要加载用户时，统一走这里，保证认证路径一致。
 *
 * 修改指引：
 * - 【习惯】修改用户数据来源（如加缓存）→ loadUserByUsername() 内 userService.findByAccount()；改动后影响登录与
 *                              JWT 过滤器加载用户的一致性
 * - 【习惯】修改账号状态/角色加载       → 影响 LoginUser 的 status/role 字段，进而影响权限判断与被冻结用户拦截；
 *                              改动需与 LoginUser 及 SecurityConfig 权限矩阵联动
 * - 【习惯】修改用户不存在时的异常      → throw new UsernameNotFoundException(...)；改动后影响登录失败的提示文案
 * - 【习惯】扩展登录方式（手机号等）    → findByAccount() 的查询语义；需保证查询结果唯一
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserService userService;

    public UserDetailsServiceImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public LoginUser loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userService.findByAccount(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        return new LoginUser(user);
    }
}