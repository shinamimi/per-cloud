# ADR-007 — 配额模型：baseQuota + rewardQuota + adminBonusQuota

> 日期: 2026-07-31
> 状态: 已接受

## 背景

用户空间配额由多种来源构成：系统默认、活动奖励、管理员赠送。需确定模型与扣减时机。

## 方案对比

| 维度 | 单一 quota 字段 | 三来源相加模型 |
|------|---------------|---------------|
| 灵活性 | 改默认配额会覆盖管理员赠送 | 各来源独立，互不覆盖 |
| 管理员操作 | 无法区分"改默认"还是"赠送" | 可只调 adminBonusQuota |
| 扩展性 | 新增来源要迁移 | 新来源加字段即可 |

## 决策

**三来源相加模型**：

```
totalQuota = baseQuota + rewardQuota + adminBonusQuota
remainingQuota = totalQuota - usedSpace
```

| 来源 | 说明 | 实现 |
|------|------|:----:|
| baseQuota | 系统默认配额，按 VIP 状态动态计算 | ✅ |
| rewardQuota | 邀请/签到/活动奖励 | —（预留字段） |
| adminBonusQuota | 管理员后台赠送 | ✅ |

- 用户不保存 baseQuota，通过 `isVip` 标记动态计算（VIP 用独立字段，非角色）
- 管理员接口 `PUT /api/admin/users/{id}/quota` 每次**设置绝对值**，非累加
- 默认值（defaultUserQuota / defaultVipQuota）由系统维护，后续移入 AdminSettingsController
- 扣减时机：上传 merge 完成后一次性 `used_space += size` 原子更新
- 相关错误码：FILE_QUOTA_EXCEEDED

## 理由

1. 各来源独立可追溯，管理员赠送不随系统默认值变化而丢失
2. VIP 独立标记，与角色体系解耦，避免"升角色=升配额"绑定
3. 预留 rewardQuota 字段，活动功能上线无需改表结构

## 影响

- t_user 新增 `is_vip`、`admin_bonus_quota`、`reward_quota` 字段
- 配额计算逻辑集中在 UserServiceImpl
