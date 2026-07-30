# 管理后台 — 用户管理功能方案

## 一、功能边界

| 功能 | 所属控制器 | 可用角色 | 本次 |
|------|-----------|---------|:----:|
| 用户列表 / 状态 / 配额 / 解锁 / 密码重置 | AdminUserController | SUPER_ADMIN / ADMIN / OPERATOR | ✅ |
| 提升用户为 OPERATOR | AdminUserController | SUPER_ADMIN / ADMIN | — |
| 仪表盘统计 | AdminDashboardController | SUPER_ADMIN / ADMIN / OPERATOR | — |
| 所有文件查看/删除 | AdminFileController | SUPER_ADMIN / ADMIN | — |
| 所有分享查看/取消 | AdminShareController | SUPER_ADMIN / ADMIN | — |
| 操作日志 | AdminLogController | SUPER_ADMIN / ADMIN / OPERATOR | — |
| 系统设置 | AdminSettingsController | SUPER_ADMIN / ADMIN / OPERATOR | — |
| 管理员账号管理 | AdminAccountController | SUPER_ADMIN 独占 | — |

## 二、接口清单

| 操作 | 方法 | 路径 | 角色 | 说明 |
|------|------|------|------|------|
| 列表 | GET | `/api/admin/users` | OPERATOR | 返回 USER 和 OPERATOR 用户，不返回 ADMIN 和 SUPER_ADMIN |
| 状态 | PUT | `/api/admin/users/{id}/status` | OPERATOR | 修改用户状态（NORMAL / DISABLED / LOCKED / INACTIVE），不能操作 ADMIN+ |
| 配额 | PUT | `/api/admin/users/{id}/quota` | OPERATOR | 设置 adminBonusQuota，不能操作 ADMIN+ |
| 解锁 | PUT | `/api/admin/users/{id}/unlock` | OPERATOR | LOCKED → NORMAL + 清除 Redis 登录计数，不能操作 ADMIN+ |
| 密码重置 | PUT | `/api/admin/users/{id}/reset-password` | OPERATOR | 重置密码，不能操作 ADMIN+ |

## 三、业务规则

### 3.1 角色展示
- 列表中的 role 字段仅展示，不可编辑（下拉框只读显示 + 后端拒绝修改）
- 后端校验：即使前端提交 role 变更也拒绝

### 3.2 列表过滤
- 排除 Role.ADMIN 和 Role.SUPER_ADMIN

### 3.3 操作校验
- 所有修改操作前检查目标用户角色，若为 ADMIN 或 SUPER_ADMIN 则拒绝

### 3.4 密码规则
- 大于 8 位，同时包含数字和英文
- DTO 层 `@Valid` 校验

### 3.5 操作日志
- 每次修改（状态/配额/解锁/密码重置）均记录 `OperationLog`

### 3.6 自动解锁
- `LOCKED` 状态持久化到数据库
- 登录时检查锁定是否超时（如 30 分钟），超时自动恢复为 `NORMAL`
- 解锁接口：LOCKED → NORMAL + 清除 Redis 登录失败计数

## 四、用户状态定义

| 状态 | 说明 |
|------|------|
| `NORMAL` | 正常 |
| `DISABLED` | 管理员禁用 |
| `LOCKED` | 登录锁定（超出失败次数） |
| `INACTIVE` | 长期未登录（预留，本次仅定义，触发逻辑后续实现） |

## 五、配额模型（Quota System）

### 5.1 配额组成

```
totalQuota = baseQuota + rewardQuota + adminBonusQuota

remainingQuota = totalQuota - usedSpace
```

| 来源 | 说明 | 本次实现 |
|------|------|:-------:|
| baseQuota | 系统默认配额，按角色动态计算 | ✅ |
| rewardQuota | 邀请/签到/活动奖励 | —（预留） |
| adminBonusQuota | 管理员后台赠送 | ✅ |

### 5.2 基础配额策略

系统维护默认值，后续移入 AdminSettingsController：

```
defaultUserQuota = 5GB
defaultVipQuota = 100GB（预留）
```

用户自身不保存 baseQuota，通过独立标记 `isVip` 动态计算：

```
if isVip == true → baseQuota = defaultVipQuota
if isVip == false → baseQuota = defaultUserQuota
```

`t_user` 表新增 `is_vip` 字段（TINYINT，默认 0），不影响 Role 现有值。

### 5.3 管理员赠送

`PUT /api/admin/users/{id}/quota` 接口每次**设置** `adminBonusQuota`（不是增量累加）。前端传绝对值。

示例流程：

```
用户当前配额：baseQuota=5GB, adminBonusQuota=0GB, totalQuota=5GB

管理员设置 +10GB → adminBonusQuota=10GB → totalQuota=15GB

管理员改为 +20GB → adminBonusQuota=20GB → totalQuota=25GB

管理员改为 0 → adminBonusQuota=0GB → totalQuota=5GB（恢复默认）
```

### 5.4 数据库字段变更

`t_user` 表新增字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| `is_vip` | TINYINT | VIP 标记，默认 0 |
| `admin_bonus_quota` | BIGINT | 管理员赠送容量，默认 0 |
| `reward_quota` | BIGINT | 奖励容量，默认 0（预留字段） |
| `used_space` | BIGINT | 已用空间（已有字段） |

### 5.5 容量计算逻辑

```java
// UserServiceImpl
long baseQuota = switch (user.getRole()) {
    case VIP -> defaultVipQuota;       // 预留
    default -> defaultUserQuota;
};
long adminBonus = user.getAdminBonusQuota() != null ? user.getAdminBonusQuota() : 0;
long reward = user.getRewardQuota() != null ? user.getRewardQuota() : 0;
long totalQuota = baseQuota + adminBonus + reward;
```

接口返回值随用户身份变化，管理员赠送容量不丢失。

## 六、需要做的事

1. **AdminUserController 列表过滤**：排除 ADMIN 和 SUPER_ADMIN，role 只读展示
2. **AdminUserController 新增密码重置端点**：`PUT /api/admin/users/{id}/reset-password`
3. **AdminUserController 其他操作增加角色校验**：状态/配额/解锁/密码重置前检查目标不是 ADMIN+
4. **UserService 新增方法**：`resetUserPassword(userId, newPassword)`
5. **DTO 新增**：`AdminResetPasswordRequest`
6. **UserStatus 新增值**：`LOCKED`、`INACTIVE`
7. **OperationType 新增值**：`RESET_PASSWORD`
8. **User entity 新增字段**：`adminBonusQuota`、`rewardQuota`
9. **t_user 表 DDL 变更**：新增 `admin_bonus_quota`、`reward_quota` 字段
10. **容量计算逻辑**：`totalQuota = baseQuota + adminBonusQuota + rewardQuota`
11. **AuthorizationPolicy 新增方法**：`canManageUser(loginUser, targetUser)`
12. **自动解锁逻辑**：登录时检查 LOCKED 是否超时，超时自动恢复 NORMAL

## 七、变更范围

### 涉及文件
- `controller/admin/AdminUserController.java`
- `service/user/UserService.java`
- `service/user/impl/UserServiceImpl.java`
- `dto/admin/AdminResetPasswordRequest.java`
- `enums/UserStatus.java`
- `enums/OperationType.java`
- `entity/User.java`
- `mapper/UserMapper.xml`（仅字段映射，不改 SQL）
- `authorization/AuthorizationPolicy.java`
- `config/SuperAdminInitializer.java`（初始化字段默认值）
- `resources/application-local.yml`（默认配额配置）

### 禁止修改
- 其他 Admin 控制器
- `config/SecurityConfig.java`
- `security/`
- 现有实体字段命名和已有枚举值
