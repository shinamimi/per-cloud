# ADR-005 — 角色体系：四级角色 + Spring Security 角色继承

> 日期: 2026-07-31
> 状态: 已接受

## 背景

管理后台需要多级管理员体系，且高级角色应自动拥有低级角色的全部权限，避免每个接口重复配置。

## 方案对比

| 维度 | 精确匹配（hasRole） | 角色继承（RoleHierarchy） |
|------|--------------------|--------------------------|
| 配置方式 | 每个接口按角色逐个声明 | 配置一次层级，自动向下兼容 |
| 新增角色 | 所有接口都要改 | 只需调整层级 |
| 越权风险 | 配置遗漏即越权 | 层级清晰，单点维护 |

## 决策

**四级角色 + RoleHierarchyConfig 继承**：

```
SUPER_ADMIN (100) > ADMIN (20) > OPERATOR (10) > USER (0)
```

- 角色值设计为阶梯数值（0/10/20/100），数值即等级，便于比较
- 配置角色继承后，SUPER_ADMIN 自动拥有 ADMIN/OPERATOR/USER 的全部权限
- SUPER_ADMIN 最终目标：仅物理接触服务器登录（本期不做，仅记录）
- 核心权限矩阵见 `admin-role-hierarchy.md`

## 理由

1. 角色从属关系明确，继承配置一次解决"高级角色拥有低级权限"
2. 阶梯数值便于授权校验（`role >= requiredLevel`），也便于前端 ROLE_LEVEL 对齐
3. 单点维护层级，接口无需重复声明多角色

## 影响

- 新增 `config/RoleHierarchyConfig`
- SecurityConfig 需补全 OPERATOR 级别路由匹配（users/settings/logs/dashboard）
- 注意 `/api/admin/users/promote` 匹配顺序需在 `/api/admin/users/**` 之前
