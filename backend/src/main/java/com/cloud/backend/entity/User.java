package com.cloud.backend.entity;

import com.cloud.backend.enums.Role;
import com.cloud.backend.enums.UserStatus;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户实体 —— 对应数据库 t_user 表。
 *
 * key 字段说明：
 * - role：角色枚举，存储为 TINYINT（0=USER, 1=OPERATOR, 2=ADMIN, 3=SUPER_ADMIN），
 *   但谨慎注意 EnumOrdinalTypeHandler 用 ordinal() 而非 getValue() 写入数据库，
 *   所以这里 ordinal 必须与 value 含义一致（USER 在最前 ordinal=0, value=0）
 * - quota：用户空间配额（字节），默认在 FileConstants.DEFAULT_QUOTA 中定义（保留向后兼容）
 * - usedSpace：已使用空间（字节），上传/删除时更新
 * - status：用户状态，NORMAL=1 正常，DISABLED=0 禁用，LOCKED=2 锁定，INACTIVE=3 未激活
 * - isVip：VIP 标记（TINYINT），影响 baseQuota 计算
 * - adminBonusQuota：管理员赠送容量（字节），默认 0
 * - rewardQuota：奖励容量（字节），默认 0
 *
 * 修改指引：
 * - 【习惯】修改 id / nickname / avatar → Long id（t_user.id 主键）/ String nickname（nickname）/ String avatar（avatar）；
 *                            仅展示，无业务联动
 * - 【习惯】修改 username / email → String username（t_user.username）/ String email（email）；两者均有唯一约束，
 *                            改字段名/长度需同步 DDL，并联动登录与找回密码逻辑
 * - 【习惯】修改 password         → String password；对应 t_user.password，BCrypt 加密存储，改加密策略需联动认证逻辑
 * - 【习惯】修改 role             → Role role；对应 t_user.role（TINYINT），USER=0/OPERATOR=10/ADMIN=20/SUPER_ADMIN=100
 *                            （见 enums/Role.java，按 ordinal 存库、按 value 判断权限大小），改枚举见 Role 修改指引
 * - 【习惯】修改 quota / usedSpace → Long quota（t_user.quota 基础配额）/ Long usedSpace（used_space 已用空间）；单位字节，
 *                            配额三来源模型（base + adminBonusQuota + rewardQuota）在 UserServiceImpl 计算，
 *                            上传/删除时更新 usedSpace
 * - 【习惯】修改 status           → UserStatus status；对应 t_user.status（TINYINT），NORMAL=1/DISABLED=0/LOCKED=2/INACTIVE=3
 *                            （见 enums/UserStatus.java，按 ordinal 存库），LoginUser.isEnabled() 依据它，改枚举见 UserStatus 修改指引
 * - 【习惯】修改 isVip            → Boolean isVip；对应 t_user.is_vip（TINYINT），影响 baseQuota 档位（普通/VIP）计算
 * - 【习惯】修改 adminBonusQuota / rewardQuota → Long adminBonusQuota（admin_bonus_quota 管理员赠送，默认 0）/
 *                            Long rewardQuota（reward_quota 奖励，默认 0）；均为字节单位，累加到总配额
 * - 【习惯】修改 createdAt / updatedAt → LocalDateTime；自动维护，无业务联动
 */
@Data
public class User {

    private Long id;
    private String username;
    private String password;
    private String email;
    private String nickname;
    private String avatar;
    private Role role;
    private Long quota;
    private Long usedSpace;
    private UserStatus status;
    private Boolean isVip;
    private Long adminBonusQuota;
    private Long rewardQuota;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}