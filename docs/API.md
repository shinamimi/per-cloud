# Cloud 企业级云盘 — API 接口文档

> 版本: v0.1
> 更新日期: 2026-07-28
> 状态: Draft

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

- `code = 200` 表示成功，其他值表示失败
- 分页响应包含 `records`, `total`, `page`, `size`

### 1.2 认证方式

所有需要登录的接口在 HTTP Header 中携带：

```
Authorization: Bearer <JWT Token>
```

### 1.3 错误码

| 范围 | 模块 | 错误码示例 |
|------|------|-----------|
| 10000-10099 | 通用 | BAD_REQUEST, UNAUTHORIZED, FORBIDDEN, NOT_FOUND, INTERNAL_ERROR |
| 10100-10199 | 认证 | LOGIN_LOCKED, CAPTCHA_INVALID, CAPTCHA_COOLDOWN |
| 10200-10299 | 文件 | FILE_NAME_DUPLICATE, FILE_QUOTA_EXCEEDED, FILE_NOT_FOUND, UPLOAD_INVALID, UPLOAD_CHUNK_MISSING, UPLOAD_MERGE_FAILED |
| 10300-10399 | 分享 | SHARE_EXPIRED, SHARE_PASSWORD_REQUIRED, SHARE_PASSWORD_INVALID |
| 10400-10499 | 团队 | TEAM_NAME_DUPLICATE, TEAM_NOT_FOUND, TEAM_MEMBER_EXISTS, TEAM_OWNER_CANNOT_LEAVE, TEAM_QUOTA_EXCEEDED |

---

## 2. 认证（M1）

所有接口公开，无需认证。

| 方法 | 路径 | 输入 | 输出 | 说明 |
|------|------|------|------|------|
| POST | `/api/auth/login` | LoginRequest | LoginResponse | 登录，返回 JWT Token |
| POST | `/api/auth/register` | RegisterRequest | - | 邮箱验证码注册 |
| POST | `/api/auth/logout` | - | - | 登出，Token 加入黑名单 |
| POST | `/api/auth/send-code` | SendCodeRequest | - | 发送邮箱验证码 |
| POST | `/api/auth/forgot-password` | ForgotPasswordRequest | - | 忘记密码 |
| POST | `/api/auth/reset-password` | ResetPasswordRequest | - | 重置密码 |

### 输入/输出结构

```
LoginRequest:
  username: string
  password: string
  captchaId: string (可选)
  captchaCode: string (可选)

LoginResponse:
  token: string           # JWT Token
  tokenType: string       # "Bearer"
  expiresIn: long         # 过期时间（秒）
  userInfo: UserProfile   # 基本用户信息

RegisterRequest:
  username: string        # 3-32 位
  password: string        # 6-32 位
  email: string
  code: string            # 邮箱验证码

SendCodeRequest:
  email: string

ForgotPasswordRequest:
  email: string
  code: string

ResetPasswordRequest:
  email: string
  code: string
  newPassword: string     # 6-32 位

UserProfile:
  id: long
  username: string
  email: string
  nickname: string
  avatar: string
  role: int
  quota: long
  usedSpace: long
  status: int
  createdAt: datetime
```

---

## 3. 用户管理（M2）

需要登录。

| 方法 | 路径 | 输入 | 输出 | 说明 |
|------|------|------|------|------|
| GET | `/api/users/me` | - | UserProfileResponse | 获取个人信息 |
| PUT | `/api/users/me` | UserUpdateRequest | - | 修改个人资料 |
| PUT | `/api/users/me/password` | PasswordUpdateRequest | - | 修改密码 |
| GET | `/api/users/me/quota` | - | QuotaResponse | 获取空间使用情况 |

### 输入/输出结构

```
UserUpdateRequest:
  nickname: string (可选, max 50)
  avatar: string (可选, max 255)

PasswordUpdateRequest:
  oldPassword: string
  newPassword: string (6-32 位)

UserProfileResponse:
  id: long
  username: string
  email: string
  nickname: string
  avatar: string
  role: int
  quota: long
  usedSpace: long
  status: int
  createdAt: datetime

QuotaResponse:
  quota: long                # 总配额（字节）
  usedSpace: long            # 已用空间（字节）
  usagePercent: double
```

---

## 4. 文件管理（M3）

需要登录。

### 4.1 文件操作

| 方法 | 路径 | 输入 | 输出 | 说明 |
|------|------|------|------|------|
| GET | `/api/files` | page, size, parentId, teamId? | PageResponse\<FileNode\> | 文件列表 |
| GET | `/api/files/tree` | parentId?, teamId? | List\<FileTree\> | 目录树 |
| POST | `/api/files/directory` | DirectoryCreateRequest | - | 创建目录 |
| PUT | `/api/files/{id}/rename` | FileRenameRequest | - | 重命名 |
| POST | `/api/files/{id}/move` | FileMoveRequest | - | 移动 |
| POST | `/api/files/{id}/copy` | FileCopyRequest | FileNode | 复制 |
| DELETE | `/api/files/{id}` | - | - | 移入回收站 |
| GET | `/api/files/search` | keyword, parentId?, teamId?, page, size | PageResponse\<FileNode\> | 搜索 |
| GET | `/api/files/{id}/preview` | - | FilePreviewResponse | 预览 |

### 4.2 分片上传

| 方法 | 路径 | 输入 | 输出 | 说明 |
|------|------|------|------|------|
| POST | `/api/files/upload/init` | UploadInitRequest | UploadInitResponse | 初始化上传 |
| POST | `/api/files/upload/chunk` | FormData (MultipartFile + uploadId + chunkNumber + totalChunks) | - | 上传分片 |
| POST | `/api/files/upload/merge` | UploadMergeRequest | FileNode | 合并分片 |
| POST | `/api/files/upload/sec` | UploadSecRequest | FileNode | 秒传 |
| GET | `/api/files/upload/progress/{uploadId}` | - | UploadProgressResponse | 查询已上传分片 |

### 4.3 下载

| 方法 | 路径 | 输入 | 输出 | 说明 |
|------|------|------|------|------|
| GET | `/api/files/download/{id}` | - | Blob | 下载单文件 |
| POST | `/api/files/download/batch` | BatchDownloadRequest | - | 批量打包下载（异步） |

### 4.4 输入/输出结构

```
DirectoryCreateRequest:
  parentId: long
  name: string (max 255)
  teamId: long (可选)

UploadInitRequest:
  fileName: string (max 255)
  fileSize: long
  fileHash: string (max 64, SHA256)
  parentId: long
  teamId: long (可选)

UploadInitResponse:
  uploadId: string
  chunkSize: int (默认 5MB)
  totalChunks: int

UploadProgressResponse:
  uploadId: string
  fileName: string
  fileSize: long
  mimeType: string
  uploadedChunks: int[] (从 1 开始)
  parentId: long
  teamId: long (可选)

UploadMergeRequest:
  uploadId: string
  fileName: string
  parentId: long
  teamId: long (可选)

UploadSecRequest:
  fileHash: string (max 64, SHA256)
  fileName: string (max 255)
  fileSize: long
  parentId: long
  teamId: long (可选)

FileRenameRequest:
  name: string (max 255)

FileMoveRequest:
  targetParentId: long

FileCopyRequest:
  targetParentId: long

FileSearchRequest:
  keyword: string (必填)
  parentId: long (可选)
  teamId: long (可选)
  page: int (默认 1)
  size: int (默认 20, max 100)

BatchDownloadRequest:
  fileIds: long[]

FileNode:
  id: long
  name: string
  parentId: long
  size: long
  mimeType: string
  extension: string
  isDirectory: boolean
  createdAt: datetime
  updatedAt: datetime

FileTree:
  id: long
  name: string
  isDirectory: boolean
  children: FileTree[]

FilePreviewResponse:
  type: string (IMAGE / TEXT / UNSUPPORTED)
  url: string (图片预览)
  content: string (文本预览)
```

---

## 5. 分享管理（M4）

### 5.1 用户端接口（需要登录）

| 方法 | 路径 | 输入 | 输出 | 说明 |
|------|------|------|------|------|
| POST | `/api/shares` | ShareCreateRequest | ShareCreateResponse | 创建分享 |
| GET | `/api/shares` | - | List\<ShareListItem\> | 我的分享列表 |
| DELETE | `/api/shares/{id}` | - | - | 取消分享 |

### 5.2 公开接口（无需认证）

| 方法 | 路径 | 输入 | 输出 | 说明 |
|------|------|------|------|------|
| GET | `/api/shares/access/{token}` | - | ShareAccessResponse | 获取分享文件信息 |
| POST | `/api/shares/access/{token}/verify` | ShareVerifyRequest | - | 验证提取码 |
| GET | `/api/shares/access/{token}/file/{fileId}/preview` | - | FilePreviewResponse | 分享内预览 |

### 5.3 输入/输出结构

```
ShareCreateRequest:
  fileId: long
  expireTime: datetime (可选，null 表示永久)
  accessPassword: string (可选, max 6 位)
  teamId: long (可选)

ShareCreateResponse:
  shareToken: string
  shareUrl: string
  expireTime: datetime

ShareListItem:
  id: long
  fileName: string
  fileSize: long
  shareToken: string
  shareUrl: string
  expireTime: datetime
  status: int (0-正常 1-已过期 2-已取消)
  downloadCount: int
  createdAt: datetime

ShareVerifyRequest:
  password: string

ShareAccessResponse:
  shareToken: string
  fileName: string
  fileId: long
  fileSize: long
  hasPassword: boolean
  verified: boolean
```

---

## 6. 回收站（M5）

需要登录。

| 方法 | 路径 | 输入 | 输出 | 说明 |
|------|------|------|------|------|
| GET | `/api/recycle-bin` | - | List\<RecycleBinItem\> | 回收站列表 |
| POST | `/api/recycle-bin/{id}/restore` | - | - | 恢复文件 |
| DELETE | `/api/recycle-bin/{id}` | - | - | 彻底删除 |

### 输入/输出结构

```
RecycleBinItem:
  id: long
  originalName: string
  mimeType: string
  size: long
  deletedTime: datetime
  expireTime: datetime
```

---

## 7. 团队空间（M6）

需要登录。

### 7.1 团队管理

| 方法 | 路径 | 输入 | 输出 | 说明 |
|------|------|------|------|------|
| POST | `/api/teams` | TeamCreateRequest | TeamInfo | 创建团队 |
| GET | `/api/teams` | - | List\<TeamListItem\> | 我的团队列表 |
| GET | `/api/teams/{id}` | - | TeamDetail | 团队详情 |
| PUT | `/api/teams/{id}` | TeamUpdateRequest | - | 更新团队信息 |
| DELETE | `/api/teams/{id}` | - | - | 解散团队 |
| POST | `/api/teams/{id}/members` | TeamInviteRequest | - | 邀请成员 |
| DELETE | `/api/teams/{id}/members/{userId}` | - | - | 移除成员 |
| GET | `/api/teams/{id}/members` | - | List\<TeamMemberInfo\> | 成员列表 |
| POST | `/api/teams/{id}/leave` | - | - | 退出团队 |

### 7.2 团队文件

| 方法 | 路径 | 输入 | 输出 | 说明 |
|------|------|------|------|------|
| GET | `/api/teams/{teamId}/files` | parentId, page, size | PageResponse\<FileNode\> | 团队文件列表 |
| POST | `/api/teams/{teamId}/files/directory` | DirectoryCreateRequest | - | 创建团队目录 |

> 团队文件的上传/下载/移动/复制/删除复用 `/api/files` 接口，添加 `teamId` 参数。

### 7.3 输入/输出结构

```
TeamCreateRequest:
  name: string (max 64)
  description: string (可选, max 512)

TeamUpdateRequest:
  name: string (可选, max 64)
  avatar: string (可选, max 255)
  description: string (可选, max 512)

TeamInviteRequest:
  username: string

TeamListItem:
  id: long
  name: string
  avatar: string
  description: string
  memberCount: int
  myRole: int

TeamDetail:
  id: long
  name: string
  avatar: string
  description: string
  ownerId: long
  ownerName: string
  memberCount: int
  quota: long
  usedSpace: long
  myRole: int
  createdAt: datetime

TeamMemberInfo:
  id: long
  userId: long
  username: string
  nickname: string
  avatar: string
  role: int
  joinedAt: datetime
```

---

## 8. 管理后台（M7）

需要 ADMIN 及以上角色。

### 8.1 仪表盘

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/dashboard/stats` | 系统统计 |

### 8.2 用户管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/users` | 用户列表 |
| PUT | `/api/admin/users/{id}/status` | 修改用户状态 |
| PUT | `/api/admin/users/{id}/quota` | 修改配额 |
| PUT | `/api/admin/users/{id}/unlock` | 解锁用户 |

### 8.3 文件管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/files` | 文件列表 |
| DELETE | `/api/admin/files/{id}` | 删除文件 |

### 8.4 分享管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/shares` | 分享列表 |
| POST | `/api/admin/shares/{id}/cancel` | 取消分享 |

### 8.5 审计日志

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/logs` | 操作日志（支持过滤） |

### 8.6 管理员管理（限 SUPER_ADMIN）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/admins` | 管理员列表 |
| POST | `/api/admin/admins` | 创建管理员 |
| DELETE | `/api/admin/admins/{id}` | 删除管理员 |
| PUT | `/api/admin/admins/{id}/role` | 修改管理员角色 |

### 8.7 团队管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/teams` | 全局团队列表 |
| DELETE | `/api/admin/teams/{id}` | 强制解散团队 |

---

## 9. WebSocket 通信（M9）

### 9.1 端点

| 路径 | 说明 | 认证方式 |
|------|------|---------|
| `/ws/progress` | 统一进度推送端点 | Token 参数（`?token=xxx`） |

### 9.2 消息格式

```json
{
  "type": "upload_progress",
  "taskId": "xxx",
  "current": 5,
  "total": 20,
  "percentage": 25,
  "status": "processing",
  "errorMessage": null
}
```

### 9.3 通信模型

- 每个用户建立一条 WebSocket 连接
- 消息体内 `taskId` 区分不同任务（uploadId / packageTaskId）
- 服务端主动推送进度消息，客户端无需轮询

---

## 10. MinIO 对象路径

| 用途 | 路径格式 |
|------|---------|
| 分片临时存储 | `uploads/{userId}/{uploadId}/chunk_{seq}` |
| 个人文件 | `files/{userId}/{fileId}/{fileName}` |
| 团队文件 | `files/team/{teamId}/{fileId}/{fileName}` |
| 缩略图 | `thumbnails/{userId}/{fileId}.jpg` |
| 打包下载临时文件 | `packages/{taskId}.zip` |
