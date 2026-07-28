# Cloud 企业级云盘 — 概要设计文档

> 版本: v0.1
> 更新日期: 2026-07-28
> 状态: Draft
> 基于: PRD v0.1

---

## 1. 文档概述

### 1.1 目的

本文档在 PRD 的基础上进行概要设计，将需求拆解为具体模块，定义每个模块的职责、边界、对外接口以及模块间的协作关系，为后续详细设计和开发任务拆解提供依据。

### 1.2 适用范围

- 后端：Java 21 + Spring Boot 4.0.7
- 前端：Vue 3 + TypeScript + Element Plus
- 存储：MySQL 8.4 + Redis 7.2 + MinIO

---

## 2. 系统模块划分

整个系统按功能边界划分为 **10 个后端模块** 和 **6 个前端模块**：

```
┌──────────────────────────────────────────────────────────────┐
│                      前端模块 (6)                             │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────────┐ │
│  │ 认证页面  │ │文件管理页│ │分享页面  │ │   团队空间页面   │ │
│  └──────────┘ └──────────┘ └──────────┘ └──────────────────┘ │
│  ┌──────────┐ ┌──────────────────┐                           │
│  │ 个人中心  │ │   管理后台页面   │                            │
│  └──────────┘ └──────────────────┘                           │
└──────────────────────┬───────────────────────────────────────┘
                       │ HTTP / WebSocket
┌──────────────────────┴───────────────────────────────────────┐
│                     后端模块 (10)                             │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐    │
│  │ 认证  │ │ 用户  │ │ 文件  │ │ 分享  │ │ 回收站 │ │ 团队  │    │
│  │ 授权  │ │ 管理  │ │ 管理  │ │ 管理  │ │      │ │ 空间  │    │
│  └──┬───┘ └──┬───┘ └──┬───┘ └──┬───┘ └──┬───┘ └──┬───┘    │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────────┐                  │
│  │ 管理  │ │ 操作  │ │WebSocket│ 基础设施   │                  │
│  │ 后台  │ │ 审计  │ │ 通信   │(Config等) │                  │
│  └──┬───┘ └──┬───┘ └──┬───┘ └──────────┘                  │
│     └────────┴────────┴───────────────────────────────────┘ │
│                   StorageService (存储抽象层)                 │
└──────────────────────────────────────────────────────────────┘
```

### 2.1 后端模块清单

| 编号 | 模块名 | 包路径 | 现有状态 | 需要新增 |
|------|--------|--------|----------|----------|
| M1 | 认证授权 | `security/`, `AuthController` | 已完成 | 无需新增 |
| M2 | 用户管理 | `service/UserService` | Service 层完成 | UserController（个人中心） |
| M3 | 文件管理 | `service/FileService`, `service/StorageService` | Service 层完成 | FileController 全套 |
| M4 | 分享管理 | `service/ShareService` | Service 层完成 | ShareController, GuestShareController |
| M5 | 回收站 | `service/RecycleBinService` | Service 层完成 | RecycleBinController（回收站列表/恢复/彻底删除） |
| M6 | 团队空间 | 无 | 从零新建 | entity/mapper/service/controller |
| M7 | 管理后台 | `controller/admin/` | Controller 部分完成 | 新增团队管理接口 |
| M8 | 操作审计 | `service/OperationLogService` | 已完成 | 无需新增 |
| M9 | WebSocket 通信 | 无 | 从零新建 | Config + Handler |
| M10 | 基础设施 | `config/`, `exception/`, `constant/`, `utils/` | 已完成 | 必要时扩展 |

### 2.2 前端模块清单

| 编号 | 模块名 | 路由路径 | 对应后端模块 | 说明 |
|------|--------|----------|-------------|------|
| F1 | 认证页面 | `/login`, `/register`, `/forgot-password` | M1 | 公开页面，无需登录 |
| F2 | 文件管理 | `/files` | M3 | 主页面，含目录树、文件列表、传输队列 |
| F3 | 分享管理 | `/shares`, `/s/{token}` | M4 | 我的分享 + 公开访问页 |
| F4 | 团队空间 | `/teams` | M6 | 团队列表、详情、团队文件 |
| F5 | 个人中心 | `/profile` | M2 | 资料修改、配额查看 |
| F6 | 管理后台 | `/admin` | M7 | 仪表盘/用户/文件/分享/日志/团队管理 |

---

## 3. 模块间关系

### 3.1 依赖关系矩阵

```
         ┌──────────────────────────────────────────────────────────┐
         │ 依赖方 \ 被依赖方 │ M1 │ M2 │ M3 │ M4 │ M5 │ M6 │ M7 │ M8 │ M9 │ M10│
         ├──────────────────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┤
         │ M1 认证授权      │    │  Y │    │    │    │    │    │    │    │  Y │
         │ M2 用户管理      │  Y │    │    │    │    │    │    │    │    │  Y │
         │ M3 文件管理      │  Y │    │    │    │    │    │    │  Y │    │  Y │
         │ M4 分享管理      │  Y │    │  Y │    │    │    │    │  Y │    │  Y │
         │ M5 回收站        │  Y │    │  Y │    │    │    │    │  Y │    │  Y │
         │ M6 团队空间      │  Y │  Y │  Y │    │    │    │    │  Y │    │  Y │
         │ M7 管理后台      │  Y │  Y │  Y │  Y │    │  Y │    │  Y │    │  Y │
         │ M8 操作审计      │    │    │    │    │    │    │    │    │    │  Y │
         │ M9 WebSocket     │  Y │    │    │    │    │    │    │    │    │  Y │
         └──────────────────┴────┴────┴────┴────┴────┴────┴────┴────┴────┴────┘
```

说明：
- Y = 存在依赖关系
- 所有模块均依赖 M10（基础设施）提供的配置、异常处理、工具类
- M3（文件管理）是系统核心，被 M4、M5、M6、M7 依赖
- M8（操作审计）被所有涉及关键操作的模块依赖

### 3.2 模块协作场景

#### 场景 1：用户上传文件

```
用户 → F2 文件页面 → M3 FileController
  ├→ M10 基础设施（鉴权）
  ├→ M3 StorageService（存储到 MinIO）
  ├→ M3 FileService（写 t_file）
  ├→ M8 操作审计（记录上传日志）
  └→ M9 WebSocket（推送上传统计进度）
```

#### 场景 2：用户创建分享

```
用户 → F3 分享页面 → M4 ShareController
  ├→ M10 基础设施（鉴权）
  ├→ M3 FileService（校验文件存在）
  ├→ M4 ShareService（写 t_share）
  └→ M8 操作审计（记录分享日志）
```

#### 场景 3：管理员查看审计日志

```
管理员 → F6 管理后台 → M7 AdminController
  ├→ M10 基础设施（鉴权/角色校验）
  ├→ M8 OperationLogService（查询 t_operation_log）
  └→ 返回日志列表
```

---

## 4. 模块详细设计

### 4.1 M1 — 认证授权模块

**职责**: 用户注册、登录、登出、Token 签发与校验、登录锁定、密码重置

**现有实现**:
- `security/`: JwtAuthenticationFilter, LoginUser, UserDetailsServiceImpl, AuthenticationEntryPointImpl, AccessDeniedHandlerImpl
- `controller/AuthController`: login, register, logout, sendCode, forgotPassword, resetPassword
- `service/CaptchaService`: 验证码生成 + Redis 存储
- `service/EmailService`: 邮件发送
- `service/LoginAttemptService`: 登录失败计数 + Redis 锁定
- `service/JwtBlacklistService`: 登出 Token 黑名单

**对外接口** (均为公开，无需认证):

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 登录，返回 JWT Token |
| POST | `/api/auth/register` | 邮箱验证码注册 |
| POST | `/api/auth/logout` | 登出，Token 加入黑名单 |
| POST | `/api/auth/send-code` | 发送邮箱验证码 |
| POST | `/api/auth/forgot-password` | 忘记密码 |
| POST | `/api/auth/reset-password` | 重置密码 |

**存储**: Redis（验证码、登录锁定、Token 黑名单）

**依赖关系**: M2 UserService、M10 基础设施

---

### 4.2 M2 — 用户管理模块

**职责**: 用户信息查询与维护、个人中心

**现有实现**:
- `entity/User`: 用户实体
- `enums/Role`: USER(0), OPERATOR(10), ADMIN(20), SUPER_ADMIN(100)
- `enums/UserStatus`: DISABLED(0), NORMAL(1)
- `service/UserService`: register, findById, findByUsername, findByAccount, findByEmail, findAll, update, existsByUsername, existsByEmail

**需要新增**:
- `controller/UserController`: 个人中心接口

**新增接口**:

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/users/me` | 获取个人信息 |
| PUT | `/api/users/me` | 修改个人资料（昵称、头像） |
| PUT | `/api/users/me/password` | 修改密码 |
| GET | `/api/users/me/quota` | 获取空间使用情况 |

**存储**: MySQL (t_user)

---

### 4.3 M3 — 文件管理模块

**职责**: 文件上传（分片+秒传）、下载、目录管理、文件列表、搜索、预览、移动/复制/重命名/删除、打包下载

**现有实现**:
- `entity/File`: 文件实体（含 parent_id 树形结构, file_hash 用于秒传）
- `enums/FileStatus`: DELETED(0), NORMAL(1)
- `service/FileService`: save, findById, listByUserAndParent, findByPath, update, removeById, updateStatus, findAll
- `service/StorageService`: 对象存储抽象接口 + MinIO 实现
- `config/FileProperties`: 文件相关配置（分片大小、上传目录等）
- `constant/FileConstants`: DEFAULT_QUOTA, DEFAULT_CHUNK_SIZE

**需要新增**:
- `controller/FileController`: 用户端全部文件操作接口
- 分片上传逻辑（初始化、上传分片、合并）
- 秒传逻辑
- 缩略图生成
- 打包下载（异步任务）
- 文件搜索

**新增接口**:

| 方法 | 路径 | 说明 | 涉及子功能 |
|------|------|------|-----------|
| GET | `/api/files` | 文件列表 | 分页、按 parentId 过滤 |
| GET | `/api/files/tree` | 目录树 | 递归构建 |
| POST | `/api/files/directory` | 创建目录 | 校验重名 |
| POST | `/api/files/upload/init` | 初始化分片上传 | 生成 uploadId, 记录文件元信息 |
| POST | `/api/files/upload/chunk` | 上传分片 | 写入 MinIO 临时目录 |
| POST | `/api/files/upload/merge` | 合并分片 | 合并后写 t_file |
| POST | `/api/files/upload/sec` | 秒传 | 按 file_hash 匹配 |
| GET | `/api/files/upload/progress/{uploadId}` | 查询已上传分片 | 返回已上传的分片编号列表，用于断点续传 |
| GET | `/api/files/download/{id}` | 下载单文件 | 预签名 URL 或流式 |
| POST | `/api/files/download/batch` | 批量打包下载 | 异步创建打包任务 |
| PUT | `/api/files/{id}/rename` | 重命名 | |
| POST | `/api/files/{id}/move` | 移动 | 修改 parent_id |
| POST | `/api/files/{id}/copy` | 复制 | StorageService.copyObject + 新记录 |
| DELETE | `/api/files/{id}` | 移入回收站 | 调 M5 写入 t_recycle_bin |
| GET | `/api/files/search` | 搜索 | 模糊匹配 filename |
| GET | `/api/files/{id}/preview` | 预览 | 图片缩略图 + 文本预览 |

**MinIO 对象路径设计**:

```
uploads/{userId}/{uploadId}/chunk_{seq}    ← 分片临时存储
files/{userId}/{fileId}/{objectName}        ← 合并后完整文件
thumbnails/{userId}/{fileId}.jpg            ← 缩略图
packages/{taskId}.zip                       ← 打包下载临时文件
```

**团队文件路径**:

```
files/team/{teamId}/{fileId}/{objectName}   ← 团队空间文件
```

**关键流程 — 分片上传**:

```
1. 前端计算 SHA256 → 调用秒传接口 (POST /sec)
   2a. Hash 匹配 → 复制已有对象到当前用户目录，秒传完成
   2b. Hash 不匹配 → 继续分片上传

3. 前端调用初始化 (POST /upload/init)
   → 后端生成 uploadId (UUID), 记录文件名/大小/总分片数到 Redis
   → 返回 uploadId + chunkSize

4. 前端逐片上传 (POST /upload/chunk)
   → 参数: uploadId, chunkNumber, totalChunks, file(分片二进制)
   → 后端写入 MinIO: uploads/{userId}/{uploadId}/chunk_{chunkNumber}
   → 更新 Redis 分片上传统计
   → 通过 WebSocket 推送进度

5. 前端调用合并 (POST /upload/merge)
   → 后端读取所有分片流合并写入 files/{userId}/{fileId}/{objectName}
   → 删除临时分片
   → 写入 t_file 记录
   → 清理 Redis 上传状态
```

**关键流程 — 秒传**:

```
1. 前端计算文件 SHA256
2. POST /api/files/upload/sec { fileHash, fileName, ... }
3. 后端查询 t_file 是否有相同 file_hash 的记录
   4a. 有 → StorageService.copyObject 复制到当前用户目录（隔离存储，防止误删影响他人）, 创建 t_file 记录
   4b. 无 → 返回 "hash_not_found", 前端转入分片上传
```

**存储**: MySQL (t_file), MinIO (文件对象), Redis (上传进度)

**依赖关系**: M8 (日志), M5 (删除时移入回收站), M10

---

### 4.4 M4 — 分享管理模块

**职责**: 创建分享、取消分享、提取码验证、分享内文件预览

**现有实现**:
- `entity/Share`: 分享实体（含 share_token UUID, expire_time, access_password）
- `enums/ShareStatus`: NORMAL(0), EXPIRED(1), CANCELED(2)
- `service/ShareService`: create, findByToken, findById, listByUserId, update, removeById, findAll

**需要新增**:
- `controller/ShareController`: 用户端分享管理接口
- `controller/GuestShareController`: 公开访问分享接口（无需认证）

**新增接口**:

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/shares` | 创建分享 | 登录 |
| GET | `/api/shares` | 我的分享列表 | 登录 |
| DELETE | `/api/shares/{id}` | 取消分享 | 登录 |
| GET | `/api/shares/access/{token}` | 获取分享文件信息 | 公开 |
| POST | `/api/shares/access/{token}/verify` | 验证提取码 | 公开 |
| GET | `/api/shares/access/{token}/file/{fileId}/preview` | 分享内预览 | 公开 |
| GET | `/api/shares/access/{token}/file/{fileId}/download` | 分享内下载 | 公开 |

**分享 Token 设计**:
- 使用 UUID 作为 share_token
- 分享链接格式: `/s/{shareToken}` 或加提取码访问
- 访问时先校验过期时间和状态

**存储**: MySQL (t_share)

**下载计数**: 每次从分享下载文件时自增 `t_share.download_count`

**依赖关系**: M3 (文件查询、预览、下载), M8 (日志), M10

---

### 4.5 M5 — 回收站模块

**职责**: 文件软删除的存储记录 + 用户端回收站管理 + 过期记录自动清理

**现有实现**:
- `entity/RecycleBin`: 回收站实体（含 expire_time, original_name, parent_id 用于恢复）
- `service/RecycleBinService`: save, removeById, listByUserId

**需要新增**:
- `controller/RecycleBinController`: 回收站列表/恢复/彻底删除

**核心逻辑**:
- 删除文件：写 t_recycle_bin + 标记 t_file.status=DELETED（不物理删除 MinIO 对象）
- 恢复文件：还原 t_file.status=NORMAL + 删除 t_recycle_bin 记录
- 彻底删除：物理删除 MinIO 对象 + 删除 t_recycle_bin 记录

**新增接口**:

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/recycle-bin` | 回收站列表 |
| POST | `/api/recycle-bin/{id}/restore` | 恢复文件 |
| DELETE | `/api/recycle-bin/{id}` | 彻底删除 |

**存储**: MySQL (t_recycle_bin)

**定时清理**: RecycleBinCleanupTask 每天扫描 `expire_time < now()` 的记录，物理删除 MinIO 对象后删除回收站记录

**依赖关系**: M3 (文件查询), M8 (日志), M10

---

### 4.6 M6 — 团队空间模块

**职责**: 团队 CRUD、成员管理、团队文件管理

**需要新建**:
- `entity/Team`: id, name, owner_id, avatar, description, status, quota, used_space, created_at, updated_at
- `entity/TeamMember`: id, team_id, user_id, role(0-成员 10-管理员 20-所有者), status, joined_at
- `enums/TeamMemberRole`: MEMBER(0), ADMIN(10), OWNER(20)
- `enums/TeamStatus`: DISSOLVED(0), NORMAL(1)
- `mapper/TeamMapper`, `mapper/TeamMemberMapper`
- `service/TeamService`, `service/TeamMemberService`
- `controller/TeamController`: 团队管理接口
- `controller/TeamFileController`: 团队文件操作接口

**新增接口**:

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/teams` | 创建团队 |
| GET | `/api/teams` | 我的团队列表 |
| GET | `/api/teams/{id}` | 团队详情 |
| PUT | `/api/teams/{id}` | 更新团队信息 |
| DELETE | `/api/teams/{id}` | 解散团队（仅所有者） |
| POST | `/api/teams/{id}/members` | 邀请成员 |
| DELETE | `/api/teams/{id}/members/{userId}` | 移除成员 |
| GET | `/api/teams/{id}/members` | 成员列表 |
| POST | `/api/teams/{id}/leave` | 退出团队 |
| GET | `/api/teams/{id}/files` | 团队文件列表 |
| POST | `/api/teams/{id}/files/directory` | 团队目录创建 |
| POST | `/api/teams/{id}/files/upload/*` | 团队文件上传 |

> 团队文件操作复用的上传/下载/移动/复制等接口，增加 teamId 参数，文件记录写入时 team_id 设为对应团队 ID。

**权限规则**:

| 操作 | 允许角色 |
|------|---------|
| 编辑团队信息 | OWNER, ADMIN |
| 解散团队 | OWNER |
| 邀请/移除成员 | OWNER, ADMIN |
| 上传/修改团队文件 | OWNER, ADMIN, MEMBER |
| 删除团队文件 | OWNER, ADMIN, MEMBER (仅自己的文件) |
| 退出团队 | MEMBER, ADMIN (OWNER 不可退出) |

**存储**: MySQL (t_team, t_team_member, t_file.team_id)

**依赖关系**: M2 (用户查询), M3 (文件操作), M8 (日志), M10

---

### 4.7 M7 — 管理后台模块

**职责**: 系统仪表盘、用户管理、文件管理、分享管理、审计日志、管理员管理、团队管理

**现有实现**:
- `controller/admin/AdminController`: 仪表盘统计, 文件列表/删除, 分享列表/取消, 日志列表
- `controller/admin/AdminUserController`: 用户列表, 状态/配额修改, 解锁
- `controller/admin/AdminAccountController`: 管理员 CRUD (限 SUPER_ADMIN)

**需要新增**:
- `controller/admin/AdminTeamController`: 团队管理

**现有接口** (已完成):

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/dashboard/stats` | 仪表盘统计 |
| GET | `/api/admin/files` | 文件列表 |
| DELETE | `/api/admin/files/{id}` | 删除文件 |
| GET | `/api/admin/shares` | 分享列表 |
| POST | `/api/admin/shares/{id}/cancel` | 取消分享 |
| GET | `/api/admin/logs` | 操作日志 |
| GET | `/api/admin/settings` | 系统设置 |
| GET | `/api/admin/users` | 用户列表 |
| PUT | `/api/admin/users/{id}/status` | 修改用户状态 |
| PUT | `/api/admin/users/{id}/quota` | 修改配额 |
| PUT | `/api/admin/users/{id}/unlock` | 解锁用户 |
| GET | `/api/admin/admins` | 管理员列表 |
| POST | `/api/admin/admins` | 创建管理员 |
| DELETE | `/api/admin/admins/{id}` | 删除管理员 |
| PUT | `/api/admin/admins/{id}/role` | 修改管理员角色 |

**新增接口**:

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/teams` | 全局团队列表 |
| DELETE | `/api/admin/teams/{id}` | 强制解散团队 |

**权限层级**:

```
SUPER_ADMIN → 全部管理功能（含管理员管理）
ADMIN       → 大部分管理功能（不含管理员管理）
OPERATOR    → 只读操作（日志查看等）（可选，当前未启用）
```

**依赖关系**: M2, M3, M4, M6, M8, M10

---

### 4.8 M8 — 操作审计模块

**职责**: 记录关键操作日志、提供日志查询

**现有实现**:
- `entity/OperationLog`: 操作日志实体
- `enums/OperationType`: LOGIN, REGISTER, UPLOAD, DOWNLOAD, DELETE, SHARE, TEAM_CREATE 等
- `enums/TargetType`: USER, FILE, SHARE, TEAM 等
- `service/OperationLogService`: log, listByUserId, listAll

**需要新增**:
- 日志查询接口的过滤条件支持（操作类型、时间范围、用户 ID）

**存储**: MySQL (t_operation_log)，写入快无外键约束

**记录时机**: 各 Controller 在关键操作完成后调用 `operationLogService.log(...)`。

---

### 4.9 M9 — WebSocket 通信模块

**职责**: 上传进度实时推送、打包下载进度推送

**需要新建**:
- `config/WebSocketConfig`: WebSocket 端点配置
- `handler/ProgressHandler`: 统一进度推送 Handler（消息内 taskId 区分上传/打包任务）

**端点设计**:

| 路径 | 说明 | 认证方式 |
|------|------|---------|
| `/ws/progress` | 统一进度推送端点（消息内 taskId 区分任务类型） | Token 参数校验 |

**推送消息格式**:

```json
{
  "type": "upload_progress",
  "taskId": "xxx",
  "current": 5,
  "total": 20,
  "percentage": 25,
  "status": "processing"
}
```

**实现方案**: Spring WebSocket 原生支持，拦截器中校验 Token。

**依赖关系**: M10 (基础设施)

---

### 4.10 M10 — 基础设施模块

**职责**: 配置管理、安全策略、异常处理、工具类、常量定义

**现有实现**:

| 类别 | 文件 | 说明 |
|------|------|------|
| 安全 | `SecurityConfig.java` | Spring Security 配置 |
| 存储 | `MinioConfig.java`, `MinioProperties.java` | MinIO 客户端配置 |
| JWT | `JwtProperties.java` | JWT 密钥/过期配置 |
| 邮件 | `MailProperties.java` | 邮件服务器配置 |
| Redis | `RedisProperties.java` | Redis 连接配置 |
| 文件 | `FileProperties.java` | 文件上传配置 |
| API | `OpenAPIConfig.java` | OpenAPI/Swagger 配置 |
| 数据库 | `MyBatisTypeHandlerConfig.java` | 枚举类型处理器 |
| 初始化 | `SuperAdminInitializer.java` | 首次启动初始化超级管理员 |
| 异常 | `BusinessException.java`, `GlobalExceptionHandler.java` | 异常体系 |
| 常量 | `FileConstants.java`, `RedisConstants.java` | 全局常量 |
| 工具 | `FileUtil.java`, `IdUtil.java`, `IpUtil.java`, `JwtTokenUtil.java` | 工具类 |

**需要新增**（按需扩展）:
- WebSocket 配置
- 缩略图转换工具类

---

## 5. 数据模型概览

### 5.1 现有表结构

```
t_user          → 用户表（含角色、配额、状态）
t_file          → 文件表（树形结构 parent_id, 含 file_hash 秒传, 现有不含 team_id）
t_share         → 分享表（含 share_token, expire_time, 现有不含 team_id）
t_recycle_bin   → 回收站表
t_operation_log → 操作日志表
```

### 5.2 新增表

```
t_team          → 团队表（owner_id, name, description, quota, used_space, status）
t_team_member   → 团队成员表（team_id, user_id, role, status）
```

### 5.3 表结构变更

```
t_file:     + team_id BIGINT DEFAULT NULL (NULL 表示个人文件)
            + INDEX idx_team (team_id, parent_id, status)

t_share:    + team_id BIGINT DEFAULT NULL (NULL 表示个人文件分享)
```

---

## 6. 前端模块设计概要

### 6.1 路由设计

```
/login                          → F1 认证页面 - 登录
/register                       → F1 认证页面 - 注册
/forgot-password                → F1 认证页面 - 找回密码
/files                          → F2 文件管理（主页面）
/files?parentId={id}            → F2 进入子目录
/shares                         → F3 分享管理 - 我的分享
/s/{token}                      → F3 分享访问 - 公开页面
/teams                          → F4 团队空间列表
/teams/{id}                     → F4 团队详情
/teams/{id}/files               → F4 团队文件
/profile                        → F5 个人中心
/admin                          → F6 管理后台 - 仪表盘
/admin/users                    → F6 用户管理
/admin/files                    → F6 文件管理
/admin/shares                   → F6 分享管理
/admin/logs                     → F6 审计日志
/admin/admins                   → F6 管理员管理
/admin/teams                    → F6 团队管理
```

### 6.2 状态管理 (Pinia)

```
useUserStore    → token, userInfo, role
useFileStore    → fileList, currentDir, breadcrumb, selectedFiles
useUploadStore  → uploadQueue, uploadProgress
useShareStore   → shareList
useTeamStore    → teamList, currentTeam
useAdminStore   → dashboard stats
```

### 6.3 公共组件

```
FileList.vue         → 文件列表（列表/图标视图切换）
DirectoryTree.vue    → 目录树
UploadDialog.vue     → 上传对话框
TransferQueue.vue    → 传输队列面板
BreadcrumbNav.vue    → 面包屑导航
ShareDialog.vue      → 创建分享弹窗
```

---

## 7. 模块对应关系总表

```
┌──────────────┬────────────────────┬──────────────────────────────┐
│  后端模块     │  Controller        │  前端页面 / 组件              │
├──────────────┼────────────────────┼──────────────────────────────┤
│ M1 认证授权   │ AuthController     │ Login, Register, ForgotPwd  │
│ M2 用户管理   │ UserController     │ ProfilePage                 │
│ M3 文件管理   │ FileController     │ FilePage, UploadDialog,     │
│              │                    │ TransferQueue, DirectoryTree │
│ M4 分享管理   │ ShareController,   │ SharePage, ShareAccessPage  │
│              │ GuestShareController│                             │
│ M5 回收站     │ RecycleBinController│ RecycleBinPage               │
│ M6 团队空间   │ TeamController,    │ TeamPage, TeamFilePage      │
│              │ TeamFileController  │                             │
│ M7 管理后台   │ AdminController,   │ AdminPage/Dashboard,        │
│              │ AdminUserController │ UserManage, FileManage,     │
│              │ AdminAccountCtrl,   │ ShareManage, LogManage,     │
│              │ AdminTeamController │ AdminManage, TeamManage     │
│ M8 操作审计   │ (由各 Controller    │ (嵌入 AdminPage)            │
│              │  内部调用)          │                             │
│ M9 WebSocket │ (Handler)          │ (嵌入 UploadDialog /        │
│              │                    │  TransferQueue)              │
│ M10 基础设施  │ (无)              │ (全局)                       │
└──────────────┴────────────────────┴──────────────────────────────┘
```

---

## 8. 安全设计

| 维度 | 设计 |
|------|------|
| 认证 | JWT 无状态，24h 过期，Redis 黑名单实现登出 |
| 授权 | Spring Security Role 粒度（USER/ADMIN/SUPER_ADMIN） |
| 密码 | BCrypt 加密，不可逆 |
| 登录保护 | 5 次失败锁定 15 分钟（Redis 计数） |
| 接口权限 | 管理端接口 `/api/admin/**` 需 ADMIN 及以上角色 |
| 分享访问 | 公开分享使用 UUID Token，可选提取码 |
| 文件隔离 | 个人文件以 `files/{userId}/` 为前缀，团队文件以 `files/team/{teamId}/` 为前缀 |
| 操作审计 | 关键操作全部记录 t_operation_log |

---

## 9. 部署架构

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Nginx      │────▶│  Spring Boot │────▶│    MySQL     │
│ (反向代理)   │     │  (Backend)   │     │    (8.4)     │
└──────────────┘     │              │     └──────────────┘
                     │  Java 21     │     ┌──────────────┐
                     │              │────▶│    Redis     │
┌──────────────┐     │              │     │    (7.2)     │
│  Vue 3 SPA   │────▶│              │     └──────────────┘
│  (Frontend)  │     │              │     ┌──────────────┐
└──────────────┘     │              │────▶│    MinIO     │
                     └──────────────┘     └──────────────┘
```

- 部署方式：Docker Compose（MySQL + Redis + MinIO 已编排）
- 后端与前端分别构建 Docker 镜像
- Nginx 统一入口，代理前端静态资源和后端 API

---

## 10. 开放问题

1. **打包下载实现方案**: 异步任务（服务端打包后上传 MinIO，通过 WebSocket 通知前端下载）vs 同步流式压缩后直接返回流
2. **缩略图生成**: 是否引入 Thumbnailator 还是用其他方案
3. **文件搜索**: 是否一开始就支持全文搜索，还是 MVP 仅支持文件名 like 模糊匹配
4. **WebSocket 集群**: 未来多实例部署时 WebSocket 连接管理方案
5. **团队空间配额**: 独立配额（t_team 表加 quota / used_space 字段），团队文件上传时校验团队配额，不计入个人配额
