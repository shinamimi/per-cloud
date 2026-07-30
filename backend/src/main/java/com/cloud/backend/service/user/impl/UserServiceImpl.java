package com.cloud.backend.service.user.impl;

import com.cloud.backend.entity.User;
import com.cloud.backend.mapper.UserMapper;
import com.cloud.backend.service.user.UserService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户服务实现 —— 直接委托 Mapper 层，不做业务校验。
 *
 * 设计思路：
 * 注册、登录的业务校验（密码加密、校验验证码等）在 Controller 层完成，
 * Service 层保持简洁，只做数据访问转发，方便后续拆分为微服务时 Mapper 调用逻辑可复用。
 */
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
        return userMapper.findByUsername(username);
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
    public List<User> findAll() {
        return userMapper.findAll();
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