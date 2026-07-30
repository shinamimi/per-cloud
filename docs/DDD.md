# Cloud 企业级云盘 — 详细设计文档

> 版本: v0.1
> 更新日期: 2026-07-28
> 状态: Draft
> 基于: PRD v0.1, HLD v0.1

---

## 1. 通用约定

### 1.1 响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

成功 code=200，失败 code=其他值。分页响应含 page, size, total, records。

### 1.2 错误码

| 范围 | 模块 | 错误码 |
|------|------|--------|
| 10000-10099 | 通用 | BAD_REQUEST, UNAUTHORIZED, FORBIDDEN, NOT_FOUND |
| 10100-10199 | 认证 | LOGIN_LOCKED, CAPTCHA_INVALID, CAPTCHA_COOLDOWN, OLD_PASSWORD_INVALID, USER_NOT_FOUND, USER_ALREADY_EXISTS, EMAIL_ALREADY_EXISTS, WRONG_CREDENTIALS, ACCOUNT_DISABLED, INVALID_TOKEN, TOKEN_EXPIRED |
| 10200-10299 | 文件 | FILE_NAME_DUPLICATE, FILE_QUOTA_EXCEEDED, FILE_NOT_FOUND, UPLOAD_INVALID, UPLOAD_CHUNK_MISSING, UPLOAD_MERGE_FAILED, FILE_UPLOAD_FAILED, FILE_DOWNLOAD_FAILED |
| 10300-10399 | 分享 | SHARE_EXPIRED, SHARE_PASSWORD_REQUIRED, SHARE_PASSWORD_INVALID, SHARE_NOT_FOUND |
| 10400-10499 | 团队 | TEAM_NAME_DUPLICATE, TEAM_NOT_FOUND, TEAM_MEMBER_EXISTS, TEAM_OWNER_CANNOT_LEAVE, TEAM_QUOTA_EXCEEDED |
| 10500-10599 | 存储 | INTERNAL_ERROR, MINIO_ERROR |

### 1.3 认证上下文

Controller 中通过 `@AuthenticationPrincipal LoginUser loginUser` 获取当前用户信息（userId, username, role）。

---

## 2. 模块 M1 — 认证授权（已有）

| 类 | 路径 | 说明 |
|---|------|------|
| AuthController | `controller/AuthController.java` | 登录/注册/登出/验证码/忘记密码/重置密码 |
| JwtTokenUtil | `utils/JwtTokenUtil.java` | Token 生成/解析/校验 |
| JwtBlacklistService | `service/system/JwtBlacklistService.java` | Token 登出黑名单 |
| LoginAttemptService | `service/system/LoginAttemptService.java` | 登录失败计数+锁定（Redis） |
| CaptchaService | `service/system/CaptchaService.java` | 验证码生成/校验 |
| EmailService | `service/system/EmailService.java` | 邮件发送 |
| JwtAuthenticationFilter | `security/JwtAuthenticationFilter.java` | 请求拦截解析 Token |
| LoginUser | `security/LoginUser.java` | 认证用户上下文 |
| SecurityConfig | `config/SecurityConfig.java` | 安全配置 |

---

## 3. 模块 M2 — 用户管理

### 3.1 控制器

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    @GetMapping("/me")         → UserProfileResponse     // 获取个人信息
    @PutMapping("/me")         → Void                    // 修改资料
    @PutMapping("/me/password")→ Void                    // 修改密码
    @GetMapping("/me/quota")   → QuotaResponse           // 空间使用
}
```

### 3.2 请求/响应

```
UserUpdateRequest:    { nickname: string (max 50), avatar: string (max 255) }
PasswordUpdateRequest: { oldPassword: string, newPassword: string (6-32) }
UserProfileResponse:  { id, username, email, nickname, avatar, role, quota, usedSpace, status, createdAt }
QuotaResponse:        { quota, usedSpace, usagePercent }
```

---

## 4. 模块 M3 — 文件管理

### 4.1 接口清单

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/files` | 文件列表（分页，按 parentId 过滤） |
| GET | `/api/files/tree` | 目录树 |
| POST | `/api/files/directory` | 创建目录 |
| POST | `/api/files/upload/init` | 初始化分片上传 |
| POST | `/api/files/upload/chunk` | 上传分片 |
| POST | `/api/files/upload/merge` | 合并分片 |
| POST | `/api/files/upload/sec` | 秒传 |
| GET | `/api/files/download/{id}` | 下载 |
| POST | `/api/files/download/batch` | 批量打包下载 |
| PUT | `/api/files/{id}/rename` | 重命名 |
| POST | `/api/files/{id}/move` | 移动 |
| POST | `/api/files/{id}/copy` | 复制 |
| DELETE | `/api/files/{id}` | 移入回收站 |
| GET | `/api/files/search` | 搜索 |
| GET | `/api/files/{id}/preview` | 预览 |

### 4.2 分片上传流程

```
1. 前端计算 SHA256 → POST /sec
   2a. Hash 匹配 → copyObject 秒传完成
   2b. 不匹配 → 继续

3. POST /upload/init → 生成 uploadId, 返回 chunkSize
4. POST /upload/chunk → 写入 MinIO 临时目录, 推送 WebSocket 进度
5. POST /upload/merge → 合并分片 → 写 t_file → 清理临时数据
```

### 4.3 MinIO 对象路径

| 用途 | 路径 |
|------|------|
| 分片临时 | `uploads/{userId}/{uploadId}/chunk_{seq}` |
| 个人文件 | `files/{userId}/{fileId}/{objectName}` |
| 团队文件 | `files/team/{teamId}/{fileId}/{objectName}` |
| 缩略图 | `thumbnails/{userId}/{fileId}.jpg` |
| 打包 | `packages/{taskId}.zip` |

### 4.4 并发处理

| 场景 | 策略 |
|------|------|
| 分片上传 | 每个 uploadId 独立，Redis 记录分片状态 |
| 合并冲突 | Redis 分布式锁 `lock:merge:{uploadId}` |
| 目录重名 | `(user_id, parent_id, name, team_id)` 唯一索引保证 |
| 配额更新 | `UPDATE t_user SET used_space = used_space + #{size}` 原子操作 |

---

## 5. 模块 M4 — 分享管理

### 5.1 控制器

```java
@RestController @RequestMapping("/api/shares")
public class ShareController {
    @PostMapping       → ShareCreateResponse    // 创建分享
    @GetMapping        → List<ShareListItem>    // 我的分享
    @DeleteMapping("/{id}") → Void             // 取消分享
}

@RestController @RequestMapping("/api/shares/access")
public class GuestShareController {
    @GetMapping("/{token}")                         → ShareAccessResponse
    @PostMapping("/{token}/verify")                 → Void          // 验证提取码
    @GetMapping("/{token}/file/{fileId}/preview")   → FilePreviewResponse
}
```

### 5.2 状态机

```
NORMAL → (过期) → EXPIRED
NORMAL → (取消) → CANCELED
```

---

## 6. 模块 M5 — 回收站

MVP 仅作为存储层——删除时标记逻辑删除 + 写入回收站记录，不物理删除 MinIO 对象。v0.2 补充管理界面。

删除逻辑内联在 FileController.delete 中：

```java
markAsDeleted(userId, file) {
    // 写入 t_recycle_bin（含 objectName）
    // 逻辑删除 t_file.status = DELETED
    // 不删除 MinIO 对象
}
```

---

## 7. 模块 M6 — 团队空间

### 7.1 控制器

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/teams` | 创建团队 |
| GET | `/api/teams` | 我的团队列表 |
| GET | `/api/teams/{id}` | 团队详情 |
| PUT | `/api/teams/{id}` | 更新信息 |
| DELETE | `/api/teams/{id}` | 解散（仅 OWNER） |
| POST | `/api/teams/{id}/members` | 邀请成员 |
| DELETE | `/api/teams/{id}/members/{userId}` | 移除成员 |
| GET | `/api/teams/{id}/members` | 成员列表 |
| POST | `/api/teams/{id}/leave` | 退出团队 |
| GET | `/api/teams/{id}/files` | 团队文件列表 |

### 7.2 权限规则

| 操作 | 允许角色 |
|------|---------|
| 编辑信息 | OWNER, ADMIN |
| 解散团队 | OWNER |
| 邀请/移除成员 | OWNER, ADMIN |
| 上传/修改团队文件 | OWNER, ADMIN, MEMBER |
| 删除团队文件 | OWNER, ADMIN |
| 退出团队 | MEMBER, ADMIN (OWNER 不可退出) |

---

## 8. 管理后台（M7）

| 模块 | 路径 | 说明 |
|------|------|------|
| 仪表盘 | `GET /api/admin/dashboard/stats` | 系统统计 |
| 用户管理 | `GET/PUT /api/admin/users/**` | 用户 CRUD |
| 文件管理 | `GET/DELETE /api/admin/files/**` | 文件管理 |
| 分享管理 | `GET/POST /api/admin/shares/**` | 分享管理 |
| 审计日志 | `GET /api/admin/logs` | 日志查询过滤 |
| 管理员管理 | `GET/POST/DELETE /api/admin/admins/**` | 管理员 CRUD |
| 团队管理 | `GET/DELETE /api/admin/teams/**` | 全局团队管理 |

---

## 9. WebSocket 通信（M9）

| 端点 | 说明 | 认证 |
|------|------|------|
| `/ws/progress?token=xxx` | 统一进度推送 | Token 参数校验 |

消息格式：
```json
{ "type": "upload_progress", "taskId": "xxx", "current": 5, "total": 20, "percentage": 25, "status": "processing" }
```

实现：Spring WebSocket，拦截器中校验 Token，每个用户一条连接，taskId 区分任务。

---

## 10. 前端模块设计

### 10.1 路由

| 路径 | 页面 | 认证 |
|------|------|------|
| `/login`, `/register`, `/forgot-password` | 认证页 | 否 |
| `/files` | 文件管理 | 是 |
| `/shares` | 分享管理 | 是 |
| `/s/{token}` | 公开分享访问 | 否 |
| `/teams` | 团队列表 | 是 |
| `/teams/{id}` | 团队详情 | 是 |
| `/teams/{id}/files` | 团队文件 | 是 |
| `/profile` | 个人中心 | 是 |
| `/admin/**` | 管理后台 | 是（ADMIN+） |

### 10.2 Pinia Store

| Store | 状态 |
|-------|------|
| useUserStore | token, userInfo, role |
| useFileStore | fileList, currentDir, breadcrumb, selectedFiles |
| useUploadStore | uploadQueue, uploadProgress |
| useShareStore | shareList |
| useTeamStore | teamList, currentTeam |
| useAdminStore | dashboard stats |

### 10.3 公共组件

| 组件 | 用途 |
|------|------|
| FileList.vue | 文件列表（列表/图标切换） |
| DirectoryTree.vue | 目录树 |
| UploadDialog.vue | 上传对话框 |
| TransferQueue.vue | 传输队列面板 |
| BreadcrumbNav.vue | 面包屑导航 |
| ShareDialog.vue | 创建分享弹窗 |

---

## 11. 包层级定义

### 11.1 总览

```
backend/src/main/java/com/cloud/backend/
├── annotation/          # 自定义标记注解（横切关注点）
├── aspect/              # AOP 切面实现（日志、权限拦截等）
├── authorization/       # 业务权限校验规则
├── bo/                  # 业务对象（多表组合查询结果）
├── config/              # 应用配置（装配 Bean、设定框架行为）
├── constant/            # 常量
├── controller/          # HTTP 控制器（请求入口）
│   └── admin/           # 管理后台控制器
├── dao/                 # 复杂 SQL 查询接口（多表 join）
├── dto/                 # 请求/响应数据传输对象（前后端传递）
│   └── admin/           # 管理后台 DTO
├── entity/              # 数据库实体映射（字段一一对应表）
├── enums/               # 枚举集中管理
├── event/               # 领域事件与监听器
├── exception/           # 异常体系
├── mapper/              # MyBatis Mapper（单表 CRUD）
├── security/            # Spring Security 框架集成
├── service/             # 业务服务
│   ├── file/            # 文件领域
│   │   └── impl/
│   ├── share/           # 分享领域
│   │   └── impl/
│   ├── system/          # 系统服务（认证、邮件、验证码等）
│   │   └── impl/
│   └── user/            # 用户领域
│       └── impl/
└── utils/               # 工具类
```

### 11.2 各包定义

| 包 | 放什么 | 不放什么 | 为什么存在 |
|----|--------|---------|-----------|
| `annotation/` | 自定义注解定义（如 `@Log`、`@RequireRole`），仅含注解声明 + `@Target`/`@Retention` | 注解的实现逻辑（放在 `aspect/`）；校验注解（放在 `validator/`） | 集中管理所有横切标记，让人一眼知道有哪些注解可用 |
| `aspect/` | AOP 切面类，含 `@Around`/`@Before`/`@After` 实现（如 `LogAspect` 拦截 `@Log` 写入操作日志） | 切面中调用的业务逻辑（放在 `service/`）；注解定义（放在 `annotation/`） | 将日志记录、权限拦截等横切逻辑从 Service 中剥离，消除重复 |
| `authorization/` | 业务权限校验规则类，返回 boolean（如 `AuthorizationPolicy.canManageFile(loginUser, targetUserId)`） | 框架安全配置（放在 `config/` 或 `security/`） | 将"什么角色能做什么"的规则与框架机制解耦，规则变化不影响基础设施 |
| `bo/` | 多表 join 查询的强类型结果对象（如 `AdminDashboardStatsBO`：含 userCount、fileCount、totalSize 等） | 单表实体（放在 `entity/`）；请求/响应 DTO（放在 `dto/`） | 跨表查询结果不属于任何单张表，用 `Map` 返回导致字段名无约束 |
| `config/` | `@Configuration` 注解的应用配置类，用于声明 `@Bean`、装配框架组件或初始化数据 | 业务逻辑（放在 `service/`） | 集中管理 Bean 定义，避免散落在各包中难以查找 |
| `constant/` | 静态常量类（如 `FileConstants.DEFAULT_QUOTA`） | 枚举（放在 `enums/`） | 避免魔术数字散落在代码中 |
| `controller/` | `@RestController` 控制器，处理 HTTP 请求映射，调用 Service，返回 DTO | 业务逻辑（放在 `service/`） | 职责单一：仅做请求解析 + 响应组装 |
| `dao/` | 复杂 SQL 查询接口（多表 join、统计查询等），加 `@Mapper` 注解 | 单表 CRUD（放在 `mapper/`） | 与 `mapper/` 分离，避免单表 Mapper 被复杂查询污染 |
| `dto/` | 请求参数类（如 `LoginRequest`）和响应类（如 `LoginResponse`），仅用于网络传输 | 业务逻辑；实体（放在 `entity/`） | 前后端契约，类型安全，与数据库表解耦 |
| `entity/` | 数据库表 → Java 类的一一映射，字段名与列名对应 | 非数据表 POJO（放在 `bo/`） | ORM 基础，MyBatis 结果映射的目标类型 |
| `enums/` | 所有枚举类型集中管理（`Role`、`FileStatus`、`OperationType` 等） | 常量值（放在 `constant/`） | 单点查找所有枚举，避免散落 |
| `event/` | 领域事件类（如 `UserRegisteredEvent`）和事件监听器（如 `UserEventListener`） | 事件中调用的业务逻辑（放在 `service/`） | 通过事件解耦，注册后发邮件、初始化空间等操作不侵入注册流程 |
| `exception/` | 自定义异常（`BusinessException`）、错误码枚举（`ErrorCode`）、全局异常处理器（`GlobalExceptionHandler`） | 业务判断逻辑（放在 `service/`） | 统一异常处理，避免每个 Controller 写 try-catch |
| `mapper/` | MyBatis Mapper 接口，一个实体对应一个 Mapper，仅含单表 CRUD | 多表 join/统计查询（放在 `dao/`） | 职责单一：单表操作不膨胀 |
| `security/` | Spring Security 集成代码：`LoginUser`（UserDetails 实现）、`JwtAuthenticationFilter`（OncePerRequestFilter）、`AuthenticationEntryPointImpl`、`AccessDeniedHandlerImpl` | 业务权限规则（放在 `authorization/`）；Bean 配置（放在 `config/`） | 隔离框架 SPI 实现，更换安全框架时只改此包 |
| `service/` | 业务接口 + 实现，按领域分子包（`file/`、`share/`、`system/`、`user/`） | 框架集成代码（放在 `security/`） | 业务逻辑集中层，领域内聚 |
| `utils/` | 无状态的静态工具方法（如 `IpUtil`、`EncryptUtil`） | 有状态服务（放在 `service/`） | 工具方法随处可用，无依赖 |

### 11.3 package-dependency 约束（不可违反）

```
controller → service, dto       # Controller 只能调 Service 和 DTO
service    → mapper, dao, bo    # Service 可调 Mapper、DAO、BO
service    → entity             # Service 可返回/接收 Entity（内部使用）
service    → authorization      # Service 可在操作前调用权限校验
service  → service              # 允许跨领域 Service 调用
mapper     → entity             # Mapper 只处理实体
dao        → bo                 # DAO 返回业务对象
dto        → entity ✗           # DTO 不能依赖 Entity（Entity 不能直接暴露给前端）
controller → entity ✗           # Controller 不能直接返回 Entity
controller → mapper ✗           # Controller 不能直接调 Mapper
```

---

## 12. 开放问题决策建议

| 问题 | 建议 | 理由 |
|------|------|------|
| 打包下载 | 异步任务 + WebSocket 通知 | 大文件不阻塞请求 |
| 缩略图 | Thumbnailator | 纯 Java，无外部依赖 |
| 文件搜索 | MVP 仅 LIKE，后续 ES | 实现简单，满足 MVP |
| WebSocket 集群 | MVP 单实例，后续 Redis Pub/Sub | 家庭使用单实例足够 |
| 团队配额 | 独立配额（t_team 表） | 空间共享不计入个人 |
