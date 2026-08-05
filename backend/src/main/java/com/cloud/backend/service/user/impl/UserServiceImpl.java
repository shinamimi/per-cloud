package com.cloud.backend.service.user.impl;

import com.cloud.backend.annotation.Log;
import com.cloud.backend.authorization.AuthorizationPolicy;
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

/**
 * 用户服务实现 —— 用户 CRUD、配额计算、密码管理与管理端用户治理。
 *
 * 设计思路：
 * 1. 配额三来源模型：总配额 = 基础配额（VIP 档位 / 普通档位）+ adminBonusQuota + rewardQuota，
 *    基础档位来自配置项（quota.default-user / quota.default-vip）
 * 2. 管理端治理操作（状态/配额/解锁/角色/密码重置）统一先做目标校验：
 *    不能操作管理员账号（AuthorizationPolicy.canManageUser），
 *    ADMIN 角色的授予/删除/变更仅限超级管理员，且不能操作自己
 * 3. 治理操作大多通过 @Log 注解记录操作日志，部分手工写日志（含详情文本）
 * 4. 已用空间通过 Mapper 原子更新（SQL 内自增/自减），避免并发覆盖
 *
 * 修改指引：
 * - 【习惯】想改"配额三来源模型（基础配额 + adminBonusQuota + rewardQuota）" → calculateTotalQuota() 与
 *   defaultUserQuota/defaultVipQuota 配置项（quota.default-user / default-vip）；
 *   改动影响所有上传/恢复/批量调整的配额判定，须与 AuthServiceImpl 注册默认配额、AdminSettingsServiceImpl
 *   quotaBatch 内联计算口径一致
 * - 【习惯】想改"注册默认字段补齐（VIP=false、赠送/奖励配额 0、BCrypt 加密）" → register()；
 *   改动影响新用户初始状态（登录/配额/权限依赖这些字段）
 * - 【习惯】想改"管理端目标校验（不能操作管理员账号）" → updateUserStatus()/updateUserQuota()/unlockUser()/
 *   resetUserPassword() 中的 AuthorizationPolicy.canManageUser()；改动影响治理操作的保护边界
 * - 【习惯】想改"ADMIN 角色授予/变更/删除权限（仅超级管理员、不能操作自己/超级管理员）" → createAdmin()/
 *   updateAdminRole()/deleteAdmin()/batchUpdateAdminRole() 与 Role 枚举比较；改动影响管理员权限矩阵
 * - 【习惯】想改"账号锁定与解锁（UserStatus.LOCKED ↔ NORMAL + 清零失败计数）" → unlockUser() 与
 *   LoginAttemptService 联动（AuthServiceImpl 登录失败也会置 LOCKED）；改动影响锁定语义的一致性
 * - 【习惯】想改"已用空间原子增减" → changeUsedSpace()（userMapper.updateUsedSpace 原子 SQL）；
 *   改动影响并发上传/删除下的配额准确性，勿改成读改写
 * - 【习惯】操作日志：治理方法大多用 @Log 切面，deleteAdmin()/batchUpdateAdminRole() 手工写日志（含用户名/角色详情）；
 *   改动影响 OperationLogService 与管理端审计
 * - 【习惯】与接口联动：本类实现 UserService，改签名/行为须同步接口契约及 UserController、AdminUserController、
 *   AuthServiceImpl/FileServiceImpl/RecycleBinServiceImpl/TeamServiceImpl 等调用方
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final OperationLogService operationLogService;
    private final com.cloud.backend.service.admin.AdminSettingsService adminSettingsService;

    /** 普通用户基础配额（字节），配置项 quota.default-user，默认 5GB */
    @Value("${quota.default-user:5368709120}")
    private long defaultUserQuota;

    /** VIP 用户基础配额（字节），配置项 quota.default-vip，默认 100GB */
    @Value("${quota.default-vip:107374182400}")
    private long defaultVipQuota;

    public UserServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder,
                           LoginAttemptService loginAttemptService,
                           OperationLogService operationLogService,
                           com.cloud.backend.service.admin.AdminSettingsService adminSettingsService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
        this.operationLogService = operationLogService;
        this.adminSettingsService = adminSettingsService;
    }

    /**
     * 注册新用户：补齐默认字段（VIP=false、赠送/奖励配额 0），密码 BCrypt 加密后入库。
     * 前置条件：调用方需先校验用户名/邮箱唯一性（本方法不校验）。
     */
    @Override
    public User register(User user) {
        // 兼容外部直接构造的实体：空值统一补默认，避免库中写入 null
        if (user.getIsVip() == null) user.setIsVip(false);
        if (user.getAdminBonusQuota() == null) user.setAdminBonusQuota(0L);
        if (user.getRewardQuota() == null) user.setRewardQuota(0L);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userMapper.insert(user);
        return user;
    }

    /**
     * 按 ID 查询用户；不存在返回 null。
     */
    @Override
    public User findById(Long id) {
        return userMapper.findById(id);
    }

    /**
     * 按用户名查询用户；不存在返回 null。
     */
    @Override
    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    /**
     * 按账号（用户名或邮箱）查询用户，供登录使用；不存在返回 null。
     */
    @Override
    public User findByAccount(String account) {
        return userMapper.findByAccount(account);
    }

    /**
     * 按邮箱查询用户；不存在返回 null。
     */
    @Override
    public User findByEmail(String email) {
        return userMapper.findByEmail(email);
    }

    /**
     * 查询全部用户。
     */
    @Override
    public List<User> findAll() {
        return userMapper.findAll();
    }

    /**
     * 全字段覆盖更新用户（须先加载完整实体再修改）。
     */
    @Override
    public int update(User user) {
        return userMapper.update(user);
    }

    /**
     * 用户名是否已占用。
     */
    @Override
    public boolean existsByUsername(String username) {
        return userMapper.findByUsername(username) != null;
    }

    /**
     * 邮箱是否已占用。
     */
    @Override
    public boolean existsByEmail(String email) {
        return userMapper.findByEmail(email) != null;
    }

    /**
     * 重置用户密码（BCrypt 加密入库）；用户不存在抛 USER_NOT_FOUND。
     */
    @Override
    public void updatePassword(Long id, String rawPassword) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        user.setPassword(passwordEncoder.encode(rawPassword));
        userMapper.update(user);
    }

    /**
     * 创建管理员（OPERATOR / ADMIN）。
     * 权限约束：不能创建 SUPER_ADMIN；授予 ADMIN 角色仅限超级管理员。
     * 前置条件：用户名未占用；配额取管理端配置的默认用户配额。
     */
    @Override
    @Log(operation = OperationType.UPDATE_USER, target = TargetType.USER,
         targetId = "#result.id", detail = "'创建管理员: ' + #username")
    public User createAdmin(String username, String password, String email, String nickname, Role role) {
        if (role == null || role == Role.SUPER_ADMIN) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能创建超级管理员");
        }
        // 只有超级管理员可以授予 ADMIN（超级管理员）权限
        if (role == Role.ADMIN && !AuthorizationPolicy.isSuperAdmin(AuthorizationPolicy.getCurrentUser())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有超级管理员可以创建超级管理员");
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
        // 默认配额走配置中心（storage.default-quota-user），无配置时回落 yml/默认值
        user.setQuota(adminSettingsService.getDefaultQuotaUser());
        user.setUsedSpace(0L);
        return register(user);
    }

    /**
     * 修改用户状态（启用/禁用/锁定）。
     * 权限约束：目标不能是管理员账号。
     */
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

    /**
     * 调整用户配额（管理端赠送额度）。
     * 权限约束：目标不能是管理员账号。
     */
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

    /**
     * 解锁登录锁定账号，并清零登录失败计数。
     * 权限约束：目标不能是管理员账号。
     */
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

    /**
     * 删除管理员（逻辑禁用为 DISABLED）。
     * 权限约束：不能删除自己与超级管理员；删除 ADMIN 仅限超级管理员。
     * 副作用：手工写入操作日志（含被删账号用户名）。
     */
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
        // 只有超级管理员可以删除 ADMIN（超级管理员）账号
        if (user.getRole() == Role.ADMIN && !AuthorizationPolicy.isSuperAdmin(AuthorizationPolicy.getCurrentUser())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有超级管理员可以删除超级管理员");
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

    /**
     * 修改管理员角色。
     * 权限约束：不能授予 SUPER_ADMIN；不能修改自己与超级管理员；
     * 授予/变更 ADMIN 角色仅限超级管理员。
     */
    @Override
    @Log(operation = OperationType.UPDATE_USER, target = TargetType.USER,
         targetId = "#id", detail = "'修改角色为: ' + #role.name()")
    public void updateAdminRole(Long id, Role role) {
        if (role == null || role == Role.SUPER_ADMIN) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能授予超级管理员");
        }
        if (id.equals(AuthorizationPolicy.getCurrentUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "不能修改自己的角色");
        }
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (user.getRole() == Role.SUPER_ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "不能操作超级管理员");
        }
        // 只有超级管理员可以授予/变更 ADMIN（超级管理员）角色
        if ((role == Role.ADMIN || user.getRole() == Role.ADMIN)
                && !AuthorizationPolicy.isSuperAdmin(AuthorizationPolicy.getCurrentUser())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有超级管理员可以操作超级管理员角色");
        }
        user.setRole(role);
        userMapper.update(user);
    }

    /**
     * 管理员重置用户密码（BCrypt 加密入库）。
     * 权限约束：目标不能是管理员账号。
     */
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

    /**
     * 计算用户总配额 = 基础配额（VIP 用 VIP 档位）+ 管理端赠送 + 奖励。
     * 空值字段按 0 参与计算，不抛异常。
     */
    @Override
    public long calculateTotalQuota(User user) {
        long baseQuota = Boolean.TRUE.equals(user.getIsVip()) ? defaultVipQuota : defaultUserQuota;
        long adminBonus = user.getAdminBonusQuota() != null ? user.getAdminBonusQuota() : 0;
        long reward = user.getRewardQuota() != null ? user.getRewardQuota() : 0;
        return baseQuota + adminBonus + reward;
    }

    /**
     * 计算用户剩余可用空间 = 总配额 - 已用（用户不存在抛 USER_NOT_FOUND）。
     */
    @Override
    public long getRemainingQuota(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        long used = user.getUsedSpace() != null ? user.getUsedSpace() : 0;
        return calculateTotalQuota(user) - used;
    }

    /**
     * 原子调整已用空间（正数扣减、负数释放），并发安全由 SQL 表达式保证。
     */
    @Override
    public void changeUsedSpace(Long userId, long delta) {
        userMapper.updateUsedSpace(userId, delta);
    }

    /**
     * 候选用户列表（角色低于 ADMIN 的普通用户），供管理员穿梭器使用。
     */
    @Override
    public List<User> listCandidates() {
        return userMapper.findAll().stream()
                .filter(u -> u.getRole().getValue() < Role.ADMIN.getValue())
                .toList();
    }

    /**
     * 批量变更角色（穿梭器批量提交）。
     * 权限约束：不能授予 SUPER_ADMIN；不能修改自己与超级管理员；
     * 涉及 ADMIN 角色的变更仅限超级管理员；逐项记录操作日志。
     */
    @Override
    public void batchUpdateAdminRole(List<RoleChangeRequest> changes) {
        boolean isSuperAdmin = AuthorizationPolicy.isSuperAdmin(AuthorizationPolicy.getCurrentUser());
        Long currentUserId = AuthorizationPolicy.getCurrentUserId();
        for (RoleChangeRequest change : changes) {
            if (change.getNewRole() == Role.SUPER_ADMIN) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "不能通过批量接口授予超级管理员");
            }
            if (change.getUserId().equals(currentUserId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "不能修改自己的角色");
            }
            User user = userMapper.findById(change.getUserId());
            if (user == null) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
            }
            if (user.getRole() == Role.SUPER_ADMIN) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "不能修改超级管理员角色");
            }
            // 只有超级管理员可以授予/变更 ADMIN（超级管理员）角色
            if ((change.getNewRole() == Role.ADMIN || user.getRole() == Role.ADMIN) && !isSuperAdmin) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "只有超级管理员可以操作超级管理员角色");
            }
            user.setRole(change.getNewRole());
            userMapper.update(user);

            OperationLog log = new OperationLog();
            log.setUserId(currentUserId);
            log.setOperation(OperationType.UPDATE_USER);
            log.setTargetType(TargetType.USER);
            log.setTargetId(user.getId());
            log.setDetail("批量修改角色为: " + change.getNewRole().name());
            operationLogService.log(log);
        }
    }

    /**
     * 用户搜索（好友/团队拉人）：用户名/邮箱前缀模糊，最多 20 条。
     * 关键字为空或全空白时返回空列表。
     */
    @Override
    public List<User> searchUsers(String keyword) {
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isEmpty()) {
            return List.of();
        }
        return userMapper.searchByKeyword(kw, 20);
    }
}
