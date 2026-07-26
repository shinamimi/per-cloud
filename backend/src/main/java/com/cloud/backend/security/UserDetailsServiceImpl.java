package com.cloud.backend.security;

import com.cloud.backend.entity.User;
import com.cloud.backend.service.UserService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

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