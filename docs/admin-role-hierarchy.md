# 管理员角色层级与权限方案

## 一、角色定义

```
SUPER_ADMIN (100)  >  ADMIN (20)  >  OPERATOR (10)  >  USER (0)
```

- **SUPER_ADMIN**: 全部权限 + 管理 ADMIN 管理员账号。最终目标：仅物理接触服务器登录
- **ADMIN**: 全部权限 + 跨用户文件管理 + 将用户提升为 OPERATOR
- **OPERATOR**: 用户管理（密码重置/状态/配额）、系统设置（token TTL 等）、日志、仪表盘
- **USER**: 仅自己

## 二、角色权限矩阵

| 功能 | SUPER_ADMIN | ADMIN | OPERATOR | USER |
|------|:-----------:|:-----:|:--------:|:----:|
| 管理 ADMIN 账号（创建/删除/改角色） | ✅ | — | — | — |
| 所有用户文件查看/删除 | ✅ | ✅ | — | — |
| 所有分享查看/取消 | ✅ | ✅ | — | — |
| 提升用户为 OPERATOR | ✅ | ✅ | — | — |
| 用户状态/配额/解锁 | ✅ | ✅ | ✅ | — |
| 用户密码重置 | ✅ | ✅ | ✅ | — |
| 系统设置（token TTL 等） | ✅ | ✅ | ✅ | — |
| 操作日志 | ✅ | ✅ | ✅ | — |
| 仪表盘 | ✅ | ✅ | ✅ | — |

## 三、SUPER_ADMIN 保护规则

- 管理端列表（`GET /api/admin/admins`）**不返回** SUPER_ADMIN，UI 不展示
- **拒绝创建** SUPER_ADMIN：`POST /api/admin/admins` 校验 role ≠ SUPER_ADMIN
- **拒绝操作** SUPER_ADMIN：改角色（单个/批量）、删除等写入接口，目标为 SUPER_ADMIN 一律拒绝；批量/单个改角色均不可授予 SUPER_ADMIN
- 字典接口 role 组不暴露 SUPER_ADMIN

## 四、需要做的事

1. **配置角色继承**：新增 `RoleHierarchyConfig`，定义 SUPER_ADMIN > ADMIN > OPERATOR > USER。Spring Security 目前是精确匹配（`hasRole("ADMIN")` 不匹配 `ROLE_SUPER_ADMIN`），配置后高级角色自动拥有低级角色的全部权限。

2. **SecurityConfig 补全规则**：当前只匹配了 `/api/admin/admins/**` + `/api/admin/**` 两级。补全 OPERATOR 级别匹配：`/api/admin/users/**`、`/api/admin/settings/**`、`/api/admin/logs/**`、`/api/admin/dashboard/**`。注意 `/api/admin/users/promote` 的路径匹配顺序要放在 `/api/admin/users/**` 之前。

3. **AdminUserController 新增两个端点**：
   - `PUT /api/admin/users/{id}/promote` — ADMIN 将普通用户（USER）提升为运营人员（OPERATOR）。需校验目标用户当前角色为 USER，且不可提升 ADMIN/SUPER_ADMIN。
   - `PUT /api/admin/users/{id}/reset-password` — OPERATOR 重置指定用户的密码。接收新密码，校验长度。

4. **UserService 新增两个方法**：
   - `promoteToOperator(userId)` — 校验 + 改角色 + 记操作日志
   - `adminResetPassword(userId, newPassword)` — 校验 + 改密码 + 记操作日志

5. **OperationType 枚举新增两个值**：`PROMOTE_USER`、`RESET_PASSWORD`

6. **新增 DTO**：`ResetPasswordRequest`（含 `newPassword` 字段 + 校验）

7. **SUPER_ADMIN 物理服务器访问**（本期不做，仅记录）：
   最终将 AdminAccountController 的 API 限制为仅本地回环地址可访问。
