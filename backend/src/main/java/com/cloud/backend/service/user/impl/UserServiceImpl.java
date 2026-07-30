package com.cloud.backend.service.user.impl;

import com.cloud.backend.annotation.Log;
import com.cloud.backend.constant.FileConstants;
import com.cloud.backend.entity.OperationLog;
import com.cloud.backend.entity.User;
import com.cloud.backend.enums.ErrorCodeEnum;
import com.cloud.backend.enums.OperationTypeEnum;
import com.cloud.backend.enums.RoleEnum;
import com.cloud.backend.enums.TargetTypeEnum;
import com.cloud.backend.enums.UserStatusEnum;
import com.cloud.backend.exception.BusinessException;
import com.cloud.backend.mapper.UserMapper;
import com.cloud.backend.service.system.LoginAttemptService;
import com.cloud.backend.service.system.OperationLogService;
import com.cloud.backend.service.user.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final OperationLogService operationLogService;

    public UserServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder,
                           LoginAttemptService loginAttemptService,
                           OperationLogService operationLogService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
        this.operationLogService = operationLogService;
    }

    @Override
    public User register(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userMapper.insert(user);
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

    @Override
    public void updatePassword(Long id, String rawPassword) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        user.setPassword(passwordEncoder.encode(rawPassword));
        userMapper.update(user);
    }

    @Override
    @Log(operation = OperationTypeEnum.UPDATE_USER, target = TargetTypeEnum.USER,
         targetId = "#result.id", detail = "'创建管理员: ' + #username")
    public User createAdmin(String username, String password, String email, String nickname, RoleEnum role) {
        if (existsByUsername(username)) {
            throw new BusinessException(ErrorCodeEnum.USER_ALREADY_EXISTS);
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setEmail(email);
        user.setNickname(nickname);
        user.setRole(role);
        user.setStatus(UserStatusEnum.NORMAL);
        user.setQuota(FileConstants.DEFAULT_QUOTA);
        user.setUsedSpace(0L);
        return register(user);
    }

    @Override
    @Log(operation = OperationTypeEnum.UPDATE_USER, target = TargetTypeEnum.USER,
         targetId = "#id", detail = "'修改用户状态为: ' + #status.name()")
    public void updateUserStatus(Long id, UserStatusEnum status) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        user.setStatus(status);
        userMapper.update(user);
    }

    @Override
    @Log(operation = OperationTypeEnum.UPDATE_USER, target = TargetTypeEnum.USER,
         targetId = "#id", detail = "'修改配额: ' + #quota")
    public void updateUserQuota(Long id, Long quota) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        user.setQuota(quota);
        userMapper.update(user);
    }

    @Override
    @Log(operation = OperationTypeEnum.UPDATE_USER, target = TargetTypeEnum.USER,
         targetId = "#id", detail = "'解锁登录锁定'")
    public void unlockUser(Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        loginAttemptService.loginSucceeded(user.getUsername());
    }

    @Override
    public void deleteAdmin(Long id, Long currentUserId) {
        if (id.equals(currentUserId)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "不能删除自己");
        }
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        if (user.getRole() == RoleEnum.SUPER_ADMIN) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "不能删除超级管理员");
        }
        user.setStatus(UserStatusEnum.DISABLED);
        userMapper.update(user);

        OperationLog log = new OperationLog();
        log.setUserId(currentUserId);
        log.setOperation(OperationTypeEnum.UPDATE_USER);
        log.setTargetType(TargetTypeEnum.USER);
        log.setTargetId(id);
        log.setDetail("禁用管理员: " + user.getUsername());
        operationLogService.log(log);
    }

    @Override
    @Log(operation = OperationTypeEnum.UPDATE_USER, target = TargetTypeEnum.USER,
         targetId = "#id", detail = "'修改角色为: ' + #role.name()")
    public void updateAdminRole(Long id, RoleEnum role) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        user.setRole(role);
        userMapper.update(user);
    }
}
