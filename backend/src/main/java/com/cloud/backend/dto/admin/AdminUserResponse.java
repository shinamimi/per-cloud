package com.cloud.backend.dto.admin;

import com.cloud.backend.enums.Role;
import com.cloud.backend.enums.UserStatus;

import java.time.LocalDateTime;

/**
 * 后台用户/管理员响应 DTO —— 用户基本信息、角色、配额构成与使用情况。
 *
 * 设计思路：
 * 配额字段按三来源拆分（quota 基础 + adminBonusQuota 赠送 + rewardQuota 奖励），
 * 并提供汇总字段 totalQuota，前端展示总配额时无需自行累加。
 *
 * 修改指引：
 * - 【习惯】修改响应字段名/类型    → 字段为前端后台用户列表取值依据，改动需同步用户查询 SQL 与前端
 * - 【习惯】修改 role             → 自定义枚举 Role（enums/Role.java：USER=0/OPERATOR=10/ADMIN=20/SUPER_ADMIN=100），存储 TINYINT，
 *                           value 越大权限越高；改动需同步枚举定义与前端角色判断
 * - 【习惯】修改 status           → 自定义枚举 UserStatus（enums/UserStatus.java：DISABLED=0/NORMAL=1/LOCKED=2/INACTIVE=3），
 *                           存储 TINYINT，LoginUser.isEnabled() 基于此判断；改动需同步枚举定义与前端状态展示
 * - 【习惯】修改配额字段单位       → quota/totalQuota/adminBonusQuota/rewardQuota/usedSpace 均为字节，前端需换算展示；
 *                           totalQuota = 基础+赠送+奖励 汇总，改动需同步配额计算逻辑与前端容量展示
 * - 【习惯】修改 isVip            → Boolean，影响基础配额（quota）计算；改动需同步 VIP 判定与配额计算逻辑
 * - 【习惯】新增响应字段          → 新增字段并同步用户查询 SQL 与前端，否则该字段恒为默认值
 */
public class AdminUserResponse {

    /** 用户 ID */
    private Long id;
    /** 用户名（登录账号） */
    private String username;
    /** 邮箱 */
    private String email;
    /** 昵称 */
    private String nickname;
    /** 头像地址 */
    private String avatar;
    /** 角色（USER / OPERATOR / ADMIN / SUPER_ADMIN） */
    private Role role;
    /** 基础配额（单位：字节，受 VIP 状态影响） */
    private Long quota;
    /** 总配额 = 基础 + 赠送 + 奖励（单位：字节） */
    private Long totalQuota;
    /** 管理端赠送配额（单位：字节） */
    private Long adminBonusQuota;
    /** 奖励配额（单位：字节） */
    private Long rewardQuota;
    /** 已使用空间（单位：字节） */
    private Long usedSpace;
    /** 是否 VIP（影响基础配额计算） */
    private Boolean isVip;
    /** 用户状态（NORMAL / DISABLED / LOCKED / INACTIVE） */
    private UserStatus status;
    /** 注册时间 */
    private LocalDateTime createdAt;

    public AdminUserResponse(Long id, String username, String email, String nickname, String avatar,
                             Role role, Long quota, Long totalQuota, Long adminBonusQuota,
                             Long rewardQuota, Long usedSpace, Boolean isVip,
                             UserStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.nickname = nickname;
        this.avatar = avatar;
        this.role = role;
        this.quota = quota;
        this.totalQuota = totalQuota;
        this.adminBonusQuota = adminBonusQuota;
        this.rewardQuota = rewardQuota;
        this.usedSpace = usedSpace;
        this.isVip = isVip;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getNickname() { return nickname; }
    public String getAvatar() { return avatar; }
    public Role getRole() { return role; }
    public Long getQuota() { return quota; }
    public Long getTotalQuota() { return totalQuota; }
    public Long getAdminBonusQuota() { return adminBonusQuota; }
    public Long getRewardQuota() { return rewardQuota; }
    public Long getUsedSpace() { return usedSpace; }
    public Boolean getIsVip() { return isVip; }
    public UserStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
