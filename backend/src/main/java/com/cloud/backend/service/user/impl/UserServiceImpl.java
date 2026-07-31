package com.cloud.backend.service.user.impl;

import com.cloud.backend.annotation.Log;
import com.cloud.backend.authorization.AuthorizationPolicy;
import com.cloud.backend.constant.FileConstants;
import com.cloud.backend.dto.admin.RoleChangeRequest;
import com.cloud.backend.entity.OperationLog;
import com.cloud.backend.entity.User;
import com.cloud.backend.enums.ErrorCode;
import com.cloud.backend.enums.OperationType;
import com.cloud.backend.enums.Role;
import com.cloud.backend.enums.TargetType;
import com.cloud.backend.enums.UserStatus;
import com.cloud.backend.exception.BusinessException;
import com.cloud.backend.mapper.UserMapper;
import com.cloud.backend.service.system.LoginAttemptService;
import com.cloud.backend.service.system.OperationLogService;
import com.cloud.backend.service.user.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final OperationLogService operationLogService;

    @Value("${quota.default-user:5368709120}")
    private long defaultUserQuota;

    @Value("${quota.default-vip:107374182400}")
    private long defaultVipQuota;

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
        if (user.getIsVip() == null) user.setIsVip(false);
        if (user.getAdminBonusQuota() == null) user.setAdminBonusQuota(0L);
        if (user.getRewardQuota() == null) user.setRewardQuota(0L);
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
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        user.setPassword(passwordEncoder.encode(rawPassword));
        userMapper.update(user);
    }

    @Override
    @Log(operation = OperationType.UPDATE_USER, target = TargetType.USER,
         targetId = "#result.id", detail = "'创建管理员: ' + #username")
    public User createAdmin(String username, String password, String email, String nickname, Role role) {
        if (role == null || role == Role.SUPER_ADMIN) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能创建超级管理员");
        }
        if (existsByUsername(username)) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setEmail(email);
        user.setNickname(nickname);
        user.setRole(role);
        user.setStatus(UserStatus.NORMAL);
        user.setIsVip(false);
        user.setAdminBonusQuota(0L);
        user.setRewardQuota(0L);
        user.setQuota(FileConstants.DEFAULT_QUOTA);
        user.setUsedSpace(0L);
        return register(user);
    }

    @Override
    @Log(operation = OperationType.UPDATE_USER, target = TargetType.USER,
         targetId = "#id", detail = "'修改用户状态为: ' + #status.name()")
    public void updateUserStatus(Long id, UserStatus status) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        AuthorizationPolicy.canManageUser(user);
        user.setStatus(status);
        userMapper.update(user);
    }

    @Override
    @Log(operation = OperationType.UPDATE_USER, target = TargetType.USER,
         targetId = "#id", detail = "'设置 adminBonusQuota: ' + #adminBonusQuota")
    public void updateUserQuota(Long id, Long adminBonusQuota) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        AuthorizationPolicy.canManageUser(user);
        user.setAdminBonusQuota(adminBonusQuota);
        userMapper.update(user);
    }

    @Override
    @Log(operation = OperationType.UPDATE_USER, target = TargetType.USER,
         targetId = "#id", detail = "'解锁登录锁定'")
    public void unlockUser(Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        AuthorizationPolicy.canManageUser(user);
        user.setStatus(UserStatus.NORMAL);
        userMapper.update(user);
        loginAttemptService.loginSucceeded(user.getUsername());
    }

    @Override
    public void deleteAdmin(Long id, Long currentUserId) {
        if (id.equals(currentUserId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能删除自己");
        }
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (user.getRole() == Role.SUPER_ADMIN) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能删除超级管理员");
        }
        user.setStatus(UserStatus.DISABLED);
        userMapper.update(user);

        OperationLog log = new OperationLog();
        log.setUserId(currentUserId);
        log.setOperation(OperationType.UPDATE_USER);
        log.setTargetType(TargetType.USER);
        log.setTargetId(id);
        log.setDetail("禁用管理员: " + user.getUsername());
        operationLogService.log(log);
    }

    @Override
    @Log(operation = OperationType.UPDATE_USER, target = TargetType.USER,
         targetId = "#id", detail = "'修改角色为: ' + #role.name()")
    public void updateAdminRole(Long id, Role role) {
        if (role == null || role == Role.SUPER_ADMIN) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能授予超级管理员");
        }
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (user.getRole() == Role.SUPER_ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "不能操作超级管理员");
        }
        user.setRole(role);
        userMapper.update(user);
    }

    @Override
    @Log(operation = OperationType.RESET_PASSWORD, target = TargetType.USER,
         targetId = "#userId", detail = "'管理员密码重置'")
    public void resetUserPassword(Long userId, String newPassword) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        AuthorizationPolicy.canManageUser(user);
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.update(user);
    }

    @Override
    public long calculateTotalQuota(User user) {
        long baseQuota = Boolean.TRUE.equals(user.getIsVip()) ? defaultVipQuota : defaultUserQuota;
        long adminBonus = user.getAdminBonusQuota() != null ? user.getAdminBonusQuota() : 0;
        long reward = user.getRewardQuota() != null ? user.getRewardQuota() : 0;
        return baseQuota + adminBonus + reward;
    }

    @Override
    public List<User> listCandidates() {
        return userMapper.findAll().stream()
                .filter(u -> u.getRole().getValue() < Role.ADMIN.getValue())
                .toList();
    }

    @Override
    public void batchUpdateAdminRole(List<RoleChangeRequest> changes) {
        for (RoleChangeRequest change : changes) {
            if (change.getNewRole() == Role.SUPER_ADMIN) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "不能通过批量接口授予超级管理员");
            }
            User user = userMapper.findById(change.getUserId());
            if (user == null) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
            }
            if (user.getRole() == Role.SUPER_ADMIN) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "不能修改超级管理员角色");
            }
            user.setRole(change.getNewRole());
            userMapper.update(user);

            OperationLog log = new OperationLog();
            log.setUserId(AuthorizationPolicy.getCurrentUserId());
            log.setOperation(OperationType.UPDATE_USER);
            log.setTargetType(TargetType.USER);
            log.setTargetId(user.getId());
            log.setDetail("批量修改角色为: " + change.getNewRole().name());
            operationLogService.log(log);
        }
    }
}
