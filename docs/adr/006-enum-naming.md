# ADR-006 — 枚举命名：去 Enum 后缀

> 日期: 2026-07-30
> 状态: 已接受

## 背景

命名规范曾要求枚举类统一 `XxxEnum` 后缀（RoleEnum、UserStatusEnum 等）。上线后发现 Jackson 序列化 JSON 字段名被 getter 名污染。

## 问题根因

Jackson 按 JavaBean 规范从 getter 方法名推导 JSON 字段名：

```
类型名 RoleEnum → getter getRoleEnum() → 属性名 "roleEnum" → JSON key "roleEnum"
```

`Enum` 后缀通过 getter 名泄漏到 JSON 序列化结果，前端拿到 `roleEnum` 而非 `role`。不是 Jackson 配置问题，是命名规范与 JavaBean 规范冲突。

## 方案对比

| 维度 | 加 @JsonProperty 修补 | 去掉 Enum 后缀 |
|------|---------------------|---------------|
| 改动量 | 每个枚举 getter 都要加，漏写则运行时出问题 | 一次性全项目重构 |
| 根治性 | 只修当前，未来新增仍会踩坑 | 命名规范层面根治 |
| 可读性 | 类型名仍带冗余后缀 | `Role.ADMIN` 本身已清晰 |

## 决策

**移除枚举类的 `Enum` 后缀**，命名规范从 `{领域}Enum` 改为 `{领域}`：

- 10 个枚举重命名（RoleEnum→Role、UserStatusEnum→UserStatus 等）
- 全量替换 ~74 个引用文件
- 同步更新 `CODING_STANDARDS.md`、`admin-user-management.md`

## 理由

1. 根治问题：JSON 字段名与字段/类名天然一致
2. 常规 Java/Spring 项目枚举本就不加后缀，减少认知负担
3. 避免未来每个新枚举 getter 都要记着加 @JsonProperty

## 影响

- 枚举引用全量重命名（编译期强制检查，漏改直接编译失败）
- 顺带修复 AdminUserResponse.getRoleEnum()、AdminLogResponse.getTargetTypeEnum() 两个 JSON 泄漏点

## 附录：踩坑记录

macOS BSD sed 不支持 `\b` 词边界，批量替换静默失败。详情见 `docs/bugs/BUG_FIXES_3.md`。
