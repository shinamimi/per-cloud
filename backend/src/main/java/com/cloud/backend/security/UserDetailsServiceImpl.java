package com.cloud.backend.security;

import com.cloud.backend.entity.User;
import com.cloud.backend.service.UserService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * UserDetailsService 实现 —— Spring Security 认证流程的数据源。
 *
 * 设计思路：
 * findByAccount 支持用户名或邮箱登录（只要唯一即可）。
 * 当登录或 JWT 过滤器中需要加载用户时，统一走这里，保证认证路径一致。
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