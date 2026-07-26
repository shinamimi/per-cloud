package com.cloud.backend.service.impl;

import com.cloud.backend.entity.User;
import com.cloud.backend.mapper.UserMapper;
import com.cloud.backend.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public User register(User user) {
        userMapper.insert(user);
        return user;
    }

    @Override
    public User login(String username, String password) {
        User user = userMapper.findByUsername(username);
        return user;
    }

    @Override
    public User findById(Long id) {
        return userMapper.findById(id);
    }

    @Override
    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    @Override
    public User findByAccount(String account) {
        return userMapper.findByAccount(account);
    }

    @Override
    public User findByEmail(String email) {
        return userMapper.findByEmail(email);
    }

    @Override
    public int update(User user) {
        return userMapper.update(user);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userMapper.findByUsername(username) != null;
    }

    @Override
    public boolean existsByEmail(String email) {
        return userMapper.findByEmail(email) != null;
    }
}