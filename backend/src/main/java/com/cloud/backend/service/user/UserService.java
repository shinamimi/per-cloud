package com.cloud.backend.service.user;

import com.cloud.backend.dto.admin.RoleChangeRequest;
import com.cloud.backend.entity.User;
import com.cloud.backend.enums.Role;
import com.cloud.backend.enums.UserStatus;

import java.util.List;

/**
 * 用户服务接口 —— 用户 CRUD、配额计算、密码管理与管理端用户治理操作。
 *
 * 设计思路：
 * 1. 配额采用三来源模型：总配额 = 基础配额（VIP 与否）+ 管理端赠送 + 奖励
 * 2. 管理端操作（状态/配额/角色/密码重置）统一拦截对管理员账号的操作，
 *    部分高危动作仅超级管理员可执行，且不允许操作自己
 * 3. 管理端敏感操作记录操作日志（OperationLog）供审计
 *
 * 修改指引：
 * - 【习惯】想改"注册默认字段（VIP=false、赠送/奖励配额 0、BCrypt 密码）" → register() 对应 UserServiceImpl.register()；
 *   改动影响新用户初始状态
 * - 【习惯】想改"全字段覆盖更新语义" → update()（需先加载完整实体再修改，避免覆盖未改动字段）；改动影响所有走
 *   update() 的调用方
 * - 【习惯】想改"配额三来源模型（基础 + adminBonusQuota + rewardQuota）" → calculateTotalQuota()/getRemainingQuota()；
 *   改动影响配额计算与上传/恢复准入
 * - 【习惯】想改"已用空间原子调整" → changeUsedSpace()（Mapper 原子 SQL 自增自减，勿改读改写）；改动影响并发上传/删除
 *   的配额一致性
 * - 【习惯】想改"管理端治理权限约束（不能操作管理员账号/不能改自己/ADMIN 角色仅限超级管理员）" →
 *   updateUserStatus()/updateUserQuota()/unlockUser()/resetUserPassword()/deleteAdmin()/updateAdminRole()/
 *   batchUpdateAdminRole() 中 AuthorizationPolicy 校验；改动影响管理端治理边界
 * - 【习惯】想改"创建管理员/角色变更" → createAdmin()/updateAdminRole()/batchUpdateAdminRole()（逐项写日志）；
 *   改动影响管理员体系
 * - 【习惯】想改"解锁登录锁定" → unlockUser()（置 NORMAL + loginAttemptService.loginSucceeded() 清计数）；
 *   改动影响锁定解锁语义
 * - 【习惯】操作日志：createAdmin()/updateUserStatus()/updateUserQuota()/unlockUser()/updateAdminRole()/
 *   resetUserPassword() 用 @Log，deleteAdmin()/batchUpdateAdminRole() 手工写日志；改动影响 OperationLogService
 * - 【习惯】新增方法 → 需同步实现类 UserServiceImpl 及 AuthServiceImpl、AdminUserController、
 *   FileService/UploadService/TeamService 等大量调用方
 */
public interface UserService {

    /**
     * 注册新用户：补齐默认字段（VIP 标记、赠送/奖励配额默认 0），密码 BCrypt 加密入库。
     */
    User register(User user);

    /**
     * 按 ID 查询用户；不存在返回 null。
     */
    User findById(Long id);

    /**
     * 按用户名查询用户；不存在返回 null。
     */
    User findByUsername(String username);

    /**
     * 按账号查询用户（用户名或邮箱均可，用于登录）；不存在返回 null。
     */
    User findByAccount(String account);

    /**
     * 按邮箱查询用户；不存在返回 null。
     */
    User findByEmail(String email);

    /**
     * 查询全部用户。
     */
    List<User> findAll();

    /**
     * 更新用户全部可变字段（密码、昵称、头像、角色、配额、状态等，按主键定位）。
     * 注意：属全字段覆盖更新，调用前需加载完整实体再修改，避免覆盖未改动字段。
     */
    int update(User user);

    /**
     * 用户名是否已占用。
     */
    boolean existsByUsername(String username);

    /**
     * 邮箱是否已占用。
     */
    boolean existsByEmail(String email);

    /**
     * 重置用户密码（BCrypt 加密入库）；用户不存在抛 USER_NOT_FOUND。
     */
    void updatePassword(Long id, String rawPassword);

    /**
     * 创建管理员账号。
     * 权限约束：角色不能为 SUPER_ADMIN；授予 ADMIN 角色仅限超级管理员。
     * 前置条件：用户名未占用。
     */
    User createAdmin(String username, String password, String email, String nickname, Role role);

    /**
     * 修改用户状态。
     * 权限约束：仅 ADMIN 可调，目标不能是管理员账号（由 AuthorizationPolicy 拦截）。
     */
    void updateUserStatus(Long id, UserStatus status);

    /**
     * 调整用户配额（管理端赠送额度）。
     * 权限约束：仅 ADMIN 可调，目标不能是管理员账号（由 AuthorizationPolicy 拦截）。
     */
    void updateUserQuota(Long id, Long quota);

    /**
     * 解锁被登录锁定（LOCKED）的用户，并清零登录失败计数。
     * 权限约束：仅 ADMIN 可调，目标不能是管理员账号（由 AuthorizationPolicy 拦截）。
     */
    void unlockUser(Long id);

    /**
     * 删除管理员（逻辑禁用为 DISABLED）。
     * 权限约束：不能删除自己与超级管理员；删除 ADMIN 仅限超级管理员。
     */
    void deleteAdmin(Long id, Long currentUserId);

    /**
     * 修改管理员角色。
     * 权限约束：不能授予 SUPER_ADMIN、不能修改自己与超级管理员；
     * 授予/变更 ADMIN 角色仅限超级管理员。
     */
    void updateAdminRole(Long id, Role role);

    /**
     * 管理员重置用户密码。
     * 权限约束：仅 ADMIN 可调，目标不能是管理员账号（由 AuthorizationPolicy 拦截）。
     */
    void resetUserPassword(Long userId, String newPassword);

    /**
     * 计算用户总配额 = 基础配额（VIP 使用 VIP 档位）+ 管理端赠送 + 奖励。
     */
    long calculateTotalQuota(User user);

    /** 用户剩余可用空间 = 总配额 - 已用（用户不存在抛 USER_NOT_FOUND） */
    long getRemainingQuota(Long userId);

    /** 原子调整已用空间（上传扣减为正、删除释放为负） */
    void changeUsedSpace(Long userId, long delta);

    /**
     * 候选用户列表（角色低于 ADMIN 的普通用户），供管理员穿梭器使用。
     */
    List<User> listCandidates();

    /**
     * 批量变更角色。
     * 权限约束：不能授予 SUPER_ADMIN、不能修改自己与超级管理员；
     * 涉及 ADMIN 角色的变更仅限超级管理员；逐项记录操作日志。
     */
    void batchUpdateAdminRole(List<RoleChangeRequest> changes);

    /** 用户搜索（好友/团队拉人）：用户名/邮箱前缀模糊，最多 20 条 */
    List<User> searchUsers(String keyword);
}
