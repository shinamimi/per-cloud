# Cloud 企业级云盘 — 详细设计文档

> 版本: v0.1
> 更新日期: 2026-07-28
> 状态: Draft

---

## 1. 文档说明

本文档在 HLD 的模块划分基础上，对每个模块进行类级别设计。每个模块包含：服务接口、DTO 结构、核心算法、状态机、并发策略和异常体系。

不包含：源码级实现、getter/setter、框架注解、Mapper SQL。

---

## 2. 通用约定

### 2.1 响应格式

所有 API 返回统一结构：

```
Result<T>:
  code: int    # 200=成功
  message: string
  data: T

PageResponse<T>:
  records: List<T>
  total: long
  page: int
  size: int
```

### 2.2 错误码体系

| 码值 | 名称 | 说明 |
|------|------|------|
| 200 | SUCCESS | 操作成功 |
| 400 | BAD_REQUEST | 请求参数错误 |
| 401 | UNAUTHORIZED | 未登录或登录已过期 |
| 403 | FORBIDDEN | 权限不足 |
| 404 | NOT_FOUND | 请求资源不存在 |
| 500 | INTERNAL_SERVER_ERROR | 服务器内部错误 |
| 10001 | USER_NOT_FOUND | 用户不存在 |
| 10002 | USER_ALREADY_EXISTS | 用户已存在 |
| 10003 | WRONG_PASSWORD | 密码错误 |
| 10004 | EMAIL_ALREADY_EXISTS | 邮箱已被注册 |
| 10005 | CAPTCHA_INVALID | 验证码错误或已过期 |
| 10006 | CAPTCHA_COOLDOWN | 发送过于频繁 |
| 20001 | FILE_NOT_FOUND | 文件不存在 |
| 20002 | FILE_UPLOAD_FAILED | 文件上传失败 |
| 20003 | FILE_DOWNLOAD_FAILED | 文件下载失败 |
| 30001 | SHARE_NOT_FOUND | 分享不存在或已过期 |
| 30002 | SHARE_EXPIRED | 分享已过期 |
| 40001 | INVALID_TOKEN | Token 无效 |
| 40002 | TOKEN_EXPIRED | Token 已过期 |
| 40003 | LOGIN_LOCKED | 账号已锁定 |
| 40004 | ACCOUNT_DISABLED | 账号已被禁用 |
| 40005 | WRONG_CREDENTIALS | 用户名或密码错误 |
| 50001 | MINIO_ERROR | MinIO 存储异常 |

### 2.3 认证上下文

Controller 层通过 `LoginUser` 获取当前用户信息：

```
LoginUser:
  userId: Long
  username: String
  role: Role
```

---

## 3. 模块 M10 — 基础设施

### 3.1 新增配置

| 配置类 | 用途 |
|--------|------|
| WebSocketConfig | 注册 `/ws/progress` 端点，配置 Auth 拦截器 |

### 3.2 新增常量

```
RedisConstants:
  UPLOAD_PROGRESS_PREFIX = "upload:progress:"
  UPLOAD_META_PREFIX = "upload:meta:"
  PACKAGE_TASK_PREFIX = "package:task:"
```

### 3.3 新增工具类

| 类 | 方法 | 说明 |
|----|------|------|
| ThumbnailUtil | generateThumbnail(sourcePath, thumbPath, width) | 缩略图生成 |

---

## 4. 模块 M1 — 认证授权（已有，不变）

### 4.1 类清单

| 类 | 说明 |
|----|------|
| AuthController | 6 个公开接口（login, register, logout, sendCode, forgotPassword, resetPassword） |
| JwtTokenUtil | Token 生成/解析/校验 |
| JwtBlacklistService | Token 登出黑名单 |
| LoginAttemptService | 登录失败计数 + Redis 锁定（5 次失败锁定 15 分钟） |
| CaptchaService | 验证码生成/校验 |
| EmailService | 邮件发送 |
| JwtAuthenticationFilter | 请求拦截解析 Token |
| LoginUser | 认证用户上下文 |
| SecurityConfig | Spring Security 配置 |

---

## 5. 模块 M2 — 用户管理

### 5.1 包结构

```
controller/UserController.java
dto/UserUpdateRequest.java
dto/PasswordUpdateRequest.java
dto/UserProfileResponse.java
dto/QuotaResponse.java
```

### 5.2 Service 依赖

UserController 依赖：UserService, PasswordEncoder

### 5.3 DTO

```
UserUpdateRequest:
  nickname: String (可选, max 50)
  avatar: String (可选, max 255)

PasswordUpdateRequest:
  oldPassword: String
  newPassword: String (6-32 位)

UserProfileResponse:
  id: Long
  username: String
  email: String
  nickname: String
  avatar: String
  role: Integer
  quota: Long
  usedSpace: Long
  status: Integer
  createdAt: LocalDateTime

QuotaResponse:
  quota: Long
  usedSpace: Long
  usagePercent: Double
```

### 5.4 接口

| 方法 | 路径 | 输入 | 输出 | 流程 |
|------|------|------|------|------|
| GET | /api/users/me | LoginUser | UserProfileResponse | 调用 UserService.findById → 脱敏返回（不返回密码） |
| PUT | /api/users/me | UserUpdateRequest | - | 更新昵称/头像 |
| PUT | /api/users/me/password | PasswordUpdateRequest | - | 校验旧密码 → BCrypt 加密新密码 → 更新 |
| GET | /api/users/me/quota | LoginUser | QuotaResponse | 查询配额和已用空间 |

### 5.5 错误码

| 码值 | 场景 |
|------|------|
| WRONG_PASSWORD (10003) | 旧密码不匹配 |

---

## 6. 模块 M3 — 文件管理

### 6.1 包结构

```
controller/FileController.java
dto/request/:
  DirectoryCreateRequest.java
  UploadInitRequest.java
  UploadMergeRequest.java
  UploadSecRequest.java
  FileRenameRequest.java
  FileMoveRequest.java
  FileCopyRequest.java
  FileSearchRequest.java
  BatchDownloadRequest.java
dto/response/:
  FileNodeResponse.java
  FileTreeResponse.java
  UploadInitResponse.java
  UploadMergeResponse.java
  FilePreviewResponse.java
  UploadProgressResponse.java
service/:
  UploadService.java
  SearchService.java
  ThumbnailService.java
  PackageService.java
service/impl/:
  UploadServiceImpl.java
  SearchServiceImpl.java
  ThumbnailServiceImpl.java
  PackageServiceImpl.java
```

### 6.2 Service 接口

**UploadService** — 分片上传生命周期管理

| 方法 | 输入 | 输出 | 说明 |
|------|------|------|------|
| initUpload | userId, UploadInitRequest | UploadInitResponse | 生成 uploadId（UUID），记录文件元信息到 Redis，返回分片配置 |
| uploadChunk | userId, uploadId, chunkNumber, MultipartFile | - | 写入 MinIO 临时目录，更新 Redis 已上传分片集合 |
| mergeChunks | userId, uploadId, UploadMergeRequest | File | 校验所有分片就绪 → 创建 t_file 记录 → MinIO composeObject 合并 → 更新 objectName → 清理临时分片和 Redis |
| secUpload | userId, UploadSecRequest | File | 按 file_hash 匹配 → 校验配额 → 创建 t_file 记录 → copyObject 隔离存储 → 更新 objectName |
| getUploadedChunks | userId, uploadId | List\<Integer\> | 从 Redis 查询已上传的分片编号列表，用于断点续传 |
| cleanupUpload | uploadId | - | 删除 MinIO 临时分片 + Redis 元数据 |
| cleanupExpiredUploads | - | - | 定时任务，扫描 Redis 中超 2 小时未更新的 uploadId 并清理 |

**SearchService** — 文件搜索

| 方法 | 输入 | 输出 |
|------|------|------|
| searchFiles | userId, keyword, parentId, teamId, PageRequest | PageResponse\<File\> |

按文件名 LIKE 模糊匹配，支持按父目录和团队范围限定。

**ThumbnailService** — 缩略图生成 + EXIF 提取

| 方法 | 输入 | 输出 | 说明 |
|------|------|------|------|
| generateThumbnail | userId, File | String | 生成图片缩略图，返回 MinIO objectName |
| getThumbnailUrl | userId, fileId | String | 获取缩略图下载 URL |
| extractExif | fileId | ExifInfo (可选) | 提取 EXIF 信息（拍摄时间、设备、GPS 等），失败时不阻断上传流程 |

**PackageService** — 打包下载

| 方法 | 输入 | 输出 | 说明 |
|------|------|------|------|
| createPackageTask | userId, fileIds | String | 创建打包任务，返回 taskId，Redis TTL=1 小时 |
| getTaskStatus | taskId | PackageTaskStatus | 查询任务状态 |
| getPackageDownloadUrl | taskId | String | 仅 completed 状态可用 |
| cleanupExpiredTasks | - | - | 定时清理过期打包文件和 Redis 记录 |

### 6.3 DTO

```
DirectoryCreateRequest:
  parentId: Long
  name: String (max 255)
  teamId: Long (可选)

UploadInitRequest:
  fileName: String (max 255)
  fileSize: Long
  fileHash: String (SHA256, max 64)
  parentId: Long
  teamId: Long (可选)

UploadInitResponse:
  uploadId: String
  chunkSize: int (默认 5MB)
  totalChunks: int

UploadProgressResponse:
  uploadId: String
  fileName: String
  fileSize: Long
  mimeType: String
  uploadedChunks: List<Integer> (从 1 开始)
  parentId: Long
  teamId: Long (可选)

UploadMergeRequest:
  uploadId: String
  fileName: String
  parentId: Long
  teamId: Long (可选)

UploadSecRequest:
  fileHash: String (SHA256, max 64)
  fileName: String (max 255)
  fileSize: Long
  parentId: Long
  teamId: Long (可选)

FileRenameRequest:
  name: String (max 255)

FileMoveRequest:
  targetParentId: Long

FileCopyRequest:
  targetParentId: Long

FileSearchRequest:
  keyword: String (必填)
  parentId: Long (可选)
  teamId: Long (可选)
  page: int (默认 1)
  size: int (默认 20, max 100)

BatchDownloadRequest:
  fileIds: List<Long>

FileNodeResponse:
  id: Long
  name: String
  parentId: Long
  size: Long
  mimeType: String
  extension: String
  isDirectory: boolean
  createdAt: LocalDateTime
  updatedAt: LocalDateTime

FileTreeResponse:
  id: Long
  name: String
  isDirectory: boolean
  children: List<FileTreeResponse>

FilePreviewResponse:
  type: PreviewType (IMAGE / TEXT / UNSUPPORTED)
  url: String     # 图片预览
  content: String # 文本预览

ExifInfo:
  cameraModel: String (可选)
  dateTimeOriginal: LocalDateTime (可选)
  latitude: Double (可选)
  longitude: Double (可选)
  width: int (可选)
  height: int (可选)
```

### 6.4 核心算法

#### 分片上传流程

```
1. 前端计算 SHA256 → 调用秒传
   a. Hash 命中 → copyObject 隔离存储, 完成
   b. 未命中 → 进入分片上传

2. 初始化 (initUpload)
   → 生成 UUID (uploadId)
   → Redis: upload:meta:{uploadId} = totalChunks, TTL=2h
   → Redis: upload:progress:{uploadId} = empty Set
   → 返回 uploadId, chunkSize, totalChunks

3. 上传分片 (uploadChunk)
   → MinIO: uploads/{userId}/{uploadId}/chunk_{seq}
   → Redis: SADD upload:progress:{uploadId} {chunkNumber}
   → 通过 WebSocket 推送进度

4. 合并 (mergeChunks)
   → 校验所有分片存在
   → 创建 t_file 记录获取 fileId
   → MinIO composeObject → files/{userId}/{fileId}/{fileName}
   → 更新 file.objectName
   → 删除临时分片 + Redis 元数据
```

#### 秒传流程

```
1. 按 fileHash 查询 t_file 中 NORMAL 状态的文件
2. 未命中 → 返回 hash_not_found，前端转入分片上传
3. 命中 → 校验配额 → 创建 t_file 记录获取 fileId
4. → MinIO copyObject: from 已有文件路径 → to files/{userId}/{fileId}/{fileName}
5. → 更新 file.objectName
6. → 原子更新 used_space: UPDATE t_user SET used_space = used_space + #{size} WHERE id = #{userId}
7. → 返回完整的文件记录
```

#### 目录树构建

使用 Map 邻接表一次扫描构建树，避免递归 O(n²)：

```
1. 查询所有目录: listDirectories(userId, teamId)
2. 遍历目录列表，按 parentId 分组到 Map<parentId, List<FileTreeResponse>>
3. 遍历每一组，为每个节点挂载其 children（通过 id 从 Map 查找）
4. 返回 parentId=0 的根目录列表
```

#### 文件预览分发

按 mimeType 类型分发预览策略：
- IMAGE（image/*）：生成/返回缩略图 URL
- TEXT（text/*）：读取文件内容（限 1MB 以内），返回纯文本
- UNSUPPORTED：返回不支持预览

### 6.5 并发策略

| 场景 | 策略 |
|------|------|
| 分片上传 | 每个 uploadId 独立，Redis 记录已上传分片序号，多分片同时上传不冲突 |
| 合并冲突 | 可选 Redis 分布式锁 `lock:merge:{uploadId}`，防同一 uploadId 被多次合并 |
| 目录重名 | MySQL 不支持部分索引。业务层先 SELECT COUNT 检查，再 INSERT。并发下极小概率重复，MVP 可接受 |
| 配额检查 | 上传前检查 used_space + fileSize ≤ quota；更新使用原子操作 `UPDATE t_user SET used_space = used_space + #{size}` |
| 团队配额 | 同理，原子操作 `UPDATE t_team SET used_space = used_space + #{size}` |

### 6.6 定时任务

| 任务 | 频率 | 职责 |
|------|------|------|
| UploadCleanupTask | 每小时 | 扫描 Redis 中超 2 小时未更新的 uploadId，删除 MinIO 临时分片和 Redis 元数据 |
| RecycleBinCleanupTask | 每天 | 扫描 t_recycle_bin 中 expire_time < now() 的记录，物理删除对应 MinIO 对象后删除回收站记录 |

### 6.7 Mapper 扩展

FileMapper 新增方法（仅签名，不含 SQL）：

| 方法 | 用途 |
|------|------|
| findByUserIdAndParentId(userId, parentId, pageable) | 按用户+父目录分页查询 |
| findByTeamIdAndParentId(teamId, parentId, status) | 按团队+父目录查询 |
| findDescendants(parentId, userId) | 递归查询子文件（用于删除/打包） |
| findByHash(fileHash, status) | 按 hash 查询（秒传） |
| search(userId, keyword, parentId, teamId) | 模糊搜索 |
| listDirectories(userId, teamId) | 列出所有目录 |

---

## 7. 模块 M4 — 分享管理

### 7.1 包结构

```
controller/ShareController.java
controller/GuestShareController.java
dto/:
  ShareCreateRequest.java
  ShareCreateResponse.java
  ShareVerifyRequest.java
  ShareListResponse.java
  ShareAccessResponse.java
```

### 7.2 DTO

```
ShareCreateRequest:
  fileId: Long
  expireTime: LocalDateTime (可选, null=永久)
  accessPassword: String (可选, max 6)
  teamId: Long (可选)

ShareCreateResponse:
  shareToken: String (UUID)
  shareUrl: String
  expireTime: LocalDateTime

ShareListResponse:
  id: Long
  fileName: String
  fileSize: Long
  shareToken: String
  shareUrl: String
  expireTime: LocalDateTime
  status: Integer (0-正常 1-已过期 2-已取消)
  downloadCount: Integer
  createdAt: LocalDateTime

ShareVerifyRequest:
  password: String

ShareAccessResponse:
  shareToken: String
  fileName: String
  fileId: Long
  fileSize: Long
  hasPassword: boolean
  verified: boolean
```

### 7.3 状态机

```
NORMAL ──(过期)──→ EXPIRED
NORMAL ──(取消)──→ CANCELED
```

过期检查：访问时 `expire_time < now()` 视为过期。

### 7.4 接口

| 方法 | 路径 | 认证 | 流程 |
|------|------|------|------|
| POST | /api/shares | 登录 | 校验文件存在 → 生成 UUID Token → 写 t_share |
| GET | /api/shares | 登录 | 按 userId 查询分享列表 |
| DELETE | /api/shares/{id} | 登录 | 校验创建者 → 更新 status=CANCELED |
| GET | /api/shares/access/{token} | 公开 | 校验 Token 有效 + 未过期 → 返回文件信息 |
| POST | /api/shares/access/{token}/verify | 公开 | 校验提取码 |
| GET | /api/shares/access/{token}/file/{fileId}/preview | 公开 | 校验分享有效性 → 委托 FileController.preview |
| GET | /api/shares/access/{token}/file/{fileId}/download | 公开 | 校验分享有效性 → 委托 StorageService 下载 → 自增 t_share.download_count |

---

## 8. 模块 M5 — 回收站

### 8.1 包结构

```
controller/RecycleBinController.java
dto/RecycleBinListResponse.java
```

### 8.2 设计原则

- 文件"删除" = 逻辑删除（标记 `t_file.status=DELETED` + 写入 `t_recycle_bin`）
- 不物理删除 MinIO 对象
- 恢复：还原 `t_file.status=NORMAL` + 删除回收站记录
- 彻底删除：物理删除 MinIO 对象 + 删除回收站记录

### 8.3 DTO

```
RecycleBinListResponse:
  id: Long
  originalName: String
  mimeType: String
  size: Long
  deletedTime: LocalDateTime
  expireTime: LocalDateTime
```

### 8.4 接口

| 方法 | 路径 | 流程 |
|------|------|------|
| GET | /api/recycle-bin | 按 userId 查询回收站列表 |
| POST | /api/recycle-bin/{id}/restore | 查询回收站记录 → 还原 t_file.status=NORMAL → 删除回收站记录 |
| DELETE | /api/recycle-bin/{id} | 物理删除 MinIO 对象 → 删除回收站记录 |

### 8.5 核心逻辑

删除（FileController 内联调用）：
1. 写入 `t_recycle_bin` 记录（含 objectName, expireTime=30 天）
2. 标记 `t_file.status=DELETED`

恢复：
1. 校验原父目录是否存在，不存在则恢复到根目录（parentId=0）
2. 恢复 `t_file` 记录为 NORMAL
3. 删除 `t_recycle_bin` 记录

彻底删除：
1. 调用 `StorageService.delete(objectName)`
2. 删除 `t_recycle_bin` 记录

---

## 9. 模块 M6 — 团队空间

### 9.1 包结构

```
controller/TeamController.java
controller/TeamFileController.java
entity/Team.java
entity/TeamMember.java
enums/TeamMemberRole.java (MEMBER=0, ADMIN=10, OWNER=20)
enums/TeamStatus.java (DISSOLVED=0, NORMAL=1)
mapper/TeamMapper.java
mapper/TeamMemberMapper.java
service/TeamService.java
service/impl/TeamServiceImpl.java
dto/:
  TeamCreateRequest.java
  TeamUpdateRequest.java
  TeamListResponse.java
  TeamDetailResponse.java
  TeamMemberResponse.java
  TeamInviteRequest.java
```

### 9.2 实体

```
Team:
  id: Long
  name: String (max 64)
  ownerId: Long
  avatar: String (可选)
  description: String (可选, max 512)
  status: TeamStatus
  quota: Long (默认 10GB)
  usedSpace: Long
  createdAt: LocalDateTime
  updatedAt: LocalDateTime

TeamMember:
  id: Long
  teamId: Long
  userId: Long
  role: TeamMemberRole (MEMBER/ADMIN/OWNER)
  status: Integer (0-已退出 1-正常)
  joinedAt: LocalDateTime
```

### 9.3 Service 接口

| 方法 | 输入 | 输出 | 说明 |
|------|------|------|------|
| createTeam | userId, TeamCreateRequest | Team | |
| updateTeam | userId, teamId, TeamUpdateRequest | Team | 校验 OWNER/ADMIN 权限 |
| dissolveTeam | userId, teamId | - | 仅 OWNER 可操作 |
| listMyTeams | userId | List\<TeamListResponse\> | 含我在内的所有团队 |
| getTeamDetail | userId, teamId | TeamDetailResponse | |
| inviteMember | userId, teamId, TeamInviteRequest | - | 按用户名邀请，OWNER/ADMIN 可操作 |
| removeMember | userId, teamId, targetUserId | - | 不能移除 OWNER |
| listMembers | teamId | List\<TeamMemberResponse\> | |
| leaveTeam | userId, teamId | - | OWNER 不能退出 |
| isOwner | userId, teamId | boolean | |
| isAdminOrOwner | userId, teamId | boolean | |
| isMember | userId, teamId | boolean | |

### 9.4 权限矩阵

| 操作 | OWNER | ADMIN | MEMBER |
|------|-------|-------|--------|
| 编辑团队信息 | Y | Y | - |
| 解散团队 | Y | - | - |
| 邀请/移除成员 | Y | Y | - |
| 上传/修改团队文件 | Y | Y | Y |
| 删除团队文件 | Y | Y | Y¹ |
> ¹ MEMBER 仅可删除自己上传的文件，不可删除他人上传的文件。
| 退出团队 | - | Y | Y |

### 9.5 团队文件

TeamFileController 复用 FileController 的文件操作逻辑，所有接口增加 teamId 参数：
- teamId 不为 null 时，user_id 校验改为 team_member 校验
- 文件记录的 team_id 字段设为对应团队 ID
- 团队文件 MinIO 路径：`files/team/{teamId}/{fileId}/{fileName}`

### 9.6 核心权限校验

权限校验方法 `checkAdminOrOwner(userId, teamId)`：
1. 查询 `t_team_member` 获取成员角色
2. 成员不存在或 status≠1 → 抛出 TEAM_NOT_FOUND
3. 角色为 MEMBER → 抛出 FORBIDDEN

---

## 10. 模块 M7 — 管理后台

### 10.1 新增类

```
controller/admin/AdminTeamController.java
  listTeams()        → 全局团队列表
  dissolveTeam(id)   → 强制解散团队
```

### 10.2 日志查询增强

OperationLogService 新增过滤查询：

```
LogFilterRequest:
  userId: Long (可选)
  operation: String (可选)
  targetType: String (可选)
  startTime: LocalDateTime (可选)
  endTime: LocalDateTime (可选)

listWithFilter(request, pageRequest) → PageResponse<OperationLog>
```

### 10.3 权限层级

```
SUPER_ADMIN → 全部管理功能（含管理员管理）
ADMIN       → 大部分管理功能（不含管理员管理）
OPERATOR    → 只读操作（可选，当前未启用）
```

---

## 11. 模块 M8 — 操作审计（已有，变化极小）

### 11.1 现有实现

OperationLogService 提供 log(listByUserId/listAll) 方法。

### 11.2 调用方式

各 Controller 在关键操作完成后直接注入并调用 `operationLogService.log(...)`，不引入 BaseController 继承体系。

### 11.3 新增枚举值

```
OperationType: 新增 TEAM_CREATE, TEAM_DISSOLVE, TEAM_INVITE, TEAM_REMOVE, TEAM_LEAVE
TargetType: 新增 TEAM
```

---

## 12. 模块 M9 — WebSocket 通信

### 12.1 包结构

```
config/WebSocketConfig.java
handler/ProgressHandler.java
interceptor/WebSocketAuthInterceptor.java
dto/ProgressMessage.java
```

### 12.2 设计原则

- 每个用户建立一条 WebSocket 连接
- 消息体内 `taskId` 区分不同任务（uploadId / packageTaskId）
- 避免并发上传时建立多条连接

### 12.3 数据结构

```
ProgressHandler:
  userSessions: Map<Long, Map<String, WebSocketSession>>
    # 外层 key = userId, 内层 key = taskId

ProgressMessage:
  type: String          # upload_progress / package_progress
  taskId: String
  current: int
  total: int
  percentage: int (0-100)
  status: String        # processing / completed / failed
  errorMessage: String
```

### 12.4 认证

`WebSocketAuthInterceptor` 从 query 参数获取 token，校验 JWT 有效性后存入 session attributes。

### 12.5 流程

```
1. 客户端连接 /ws/progress?token=xxx
2. 拦截器校验 Token → 存入 userId
3. Handler.afterConnectionEstablished → 初始化 userSessions[userId]
4. 客户端 subscribe(taskId) → userSessions[userId][taskId] = session
5. 服务端 sendProgress(userId, taskId, message) → 查找 session 并发送
6. 连接断开 → 清理 userSessions[userId]
```

---

## 13. 前端模块设计

### 13.1 项目结构

```
src/
├── api/             # API 调用层
├── components/      # 通用组件
├── composables/     # 组合式函数
├── layout/          # 布局
├── router/          # 路由
├── stores/          # Pinia Store
├── types/           # TypeScript 类型
├── utils/           # 工具类
└── views/           # 页面
```

### 13.2 组件清单

| 组件 | 职责 |
|------|------|
| FileList.vue | 文件列表（列表/图标视图切换） |
| DirectoryTree.vue | 目录树 |
| UploadDialog.vue | 上传对话框 |
| TransferQueue.vue | 传输队列面板 |
| BreadcrumbNav.vue | 面包屑导航 |
| ShareDialog.vue | 创建分享弹窗 |

### 13.3 状态管理 (Pinia)

| Store | 状态 |
|-------|------|
| userStore | token, userInfo, role |
| fileStore | fileList, currentDir, breadcrumb, selectedFiles |
| uploadStore | uploadQueue, uploadProgress |
| shareStore | shareList |
| teamStore | teamList, currentTeam |
| adminStore | dashboard stats |

### 13.4 分片上传前端流程

```
1. 在 Web Worker 中计算文件 SHA256（不阻塞主线程）
2. 尝试秒传（POST /upload/sec）
   a. 成功 → 返回
   b. hash_not_found → 继续
3. 检查 localStorage 是否有未完成的 uploadId（断点续传）
   a. 有 → 调用 GET /upload/progress/{uploadId} 获取已上传分片列表
   b. 无 → 调用 POST /upload/init 初始化
4. 通过 WebSocket 订阅进度（subscribe(taskId)）
5. 遍历所有分片，跳过已上传的，并发上传剩余分片
6. 所有分片上传完成 → 调用 POST /upload/merge
7. 关闭 WebSocket 连接，清除 localStorage
```

---

## 14. 附录：开放问题决策建议

| 问题 | 建议 | 理由 |
|------|------|------|
| 打包下载 | 异步任务模式 | 大文件打包可能耗时，异步不阻塞请求，WebSocket 通知完成 |
| 缩略图生成 | 使用 Thumbnailator | 纯 Java 实现，无需外部依赖 |
| 文件搜索 | MVP 仅文件名 LIKE | 实现简单，后续可引入 ES |
| 团队配额 | 独立配额（t_team.quota） | 共享空间不计入个人配额 |
| WebSocket 集群 | MVP 不做集群 | 单实例足够家庭使用，后续引入 Redis Pub/Sub |
