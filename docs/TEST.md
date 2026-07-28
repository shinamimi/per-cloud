# Cloud 企业级云盘 — 测试设计文档

> 版本: v0.1
> 更新日期: 2026-07-28
> 状态: Draft

---

## 1. 测试层次

| 层次 | 框架 | 覆盖范围 |
|------|------|---------|
| 单元测试 | JUnit5 + Mockito | 各 Service/Controller/Mapper 的逻辑和异常路径 |
| 集成测试 | SpringBootTest + Testcontainers | 端到端流程（MySQL + Redis + MinIO） |
| 前端测试 | Vitest + Vue Test Utils | 组件渲染、交互事件、API 调用 |

---

## 2. 后端单元测试

### 2.1 M1 认证授权

| 测试类 | Mock 策略 | 关键测试场景 |
|--------|----------|-------------|
| AuthControllerTest | Mock UserService, CaptchaService, EmailService, RedisTemplate | 登录成功/失败/锁定, 注册, 验证码冷却 |
| JwtTokenUtilTest | 无 Mock（纯工具类） | Token 生成、解析、校验 |
| LoginAttemptServiceTest | Mock RedisTemplate | 失败计数、锁定阈值、过期解锁 |

### 2.2 M2 用户管理

| 测试类 | Mock 策略 | 关键测试场景 |
|--------|----------|-------------|
| UserControllerTest | Mock UserService, PasswordEncoder | 资料修改、密码修改（旧密码校验 → 新密码 BCrypt 加密） |

### 2.3 M3 文件管理

| 测试类 | Mock 策略 | 关键测试场景 |
|--------|----------|-------------|
| FileControllerTest | Mock FileService, UploadService, SearchService | 参数校验、路由分发 |
| UploadServiceTest | Mock FileService, StorageService, RedisTemplate | init/uploadChunk/merge/sec 全流程；分片缺失异常、合并后清理 |
| SearchServiceTest | Mock FileMapper | 模糊搜索、分页 |
| ThumbnailServiceTest | Mock StorageService | 缩略图生成 |
| PackageServiceTest | Mock FileService, StorageService | 打包任务创建、状态查询、过期清理 |

并发场景：
- 同一 uploadId 合并冲突（Redis 锁）
- 同名目录同时创建（业务层预检查）
- 配额临界值并发（原子 UPDATE）

### 2.4 M4 分享管理

| 测试类 | Mock 策略 | 关键测试场景 |
|--------|----------|-------------|
| ShareControllerTest | Mock ShareService | 创建/列表/取消分享 |
| GuestShareControllerTest | Mock ShareService, FileService | 公开访问、提取码校验、预览 |

### 2.5 M5 回收站

| 测试类 | Mock 策略 | 关键测试场景 |
|--------|----------|-------------|
| RecycleBinControllerTest | Mock FileService, RecycleBinService, StorageService | 删除后 t_file.status=DELETED + t_recycle_bin 存在 + MinIO 未被删除；恢复后 status=NORMAL + 记录移除；彻底删除 MinIO 对象 + 记录移除 |

### 2.6 M6 团队空间

| 测试类 | Mock 策略 | 关键测试场景 |
|--------|----------|-------------|
| TeamServiceTest | Mock TeamMapper, TeamMemberMapper, FileMapper | 创建/更新/解散、成员邀请/移除/退出、权限校验（OWNER 可解散，ADMIN 可邀请，MEMBER 不能移除） |
| TeamFileControllerTest | Mock TeamService, FileService | 团队文件列表、上传、删除 |

并发场景：
- 同一用户名同时被两个管理员邀请

### 2.7 M7 管理后台

| 测试类 | Mock 策略 | 关键测试场景 |
|--------|----------|-------------|
| AdminTeamControllerTest | Mock TeamService | 全局团队列表、强制解散 |

### 2.8 M8 操作审计

| 测试类 | Mock 策略 | 关键测试场景 |
|--------|----------|-------------|
| OperationLogServiceTest | Mock OperationLogMapper | 日志写入、条件查询（按操作类型/时间范围/用户 ID 过滤） |

### 2.9 M9 WebSocket + 定时任务

| 测试类 | Mock 策略 | 关键测试场景 |
|--------|----------|-------------|
| ProgressHandlerTest | Mock WebSocketSession | 连接建立、消息推送、断连清理 |
| WebSocketAuthInterceptorTest | Mock JwtTokenUtil | Token 校验、无 Token 拒绝 |
| UploadMergeConcurrencyTest | Mock FileService, StorageService, RedisTemplate | 同一 uploadId 被多次合并时的 Redis 锁防重入 |
| UploadCleanupTaskTest | Mock StorageService, RedisTemplate | 扫描过期 uploadId，删除 MinIO 临时分片和 Redis 元数据 |
| PackageServiceCleanupTest | Mock FileService, StorageService | 过期打包任务的文件和 Redis 清理 |
| RecycleBinCleanupTaskTest | Mock RecycleBinMapper, StorageService | 构造 expire_time < now() 的记录，断言清理逻辑物理删除 MinIO 对象后删除回收站记录 |

---

## 3. 集成测试

| 场景 | 说明 |
|------|------|
| 上传→下载 | 分片上传完整流程，验证 MinIO 对象和 DB 记录一致 |
| 秒传 | 相同 hash 文件上传，验证 copyObject 隔离性 |
| 上传过期清理 | 创建分片上传后等待超时，验证临时分片和 Redis 元数据被定时任务清理 |
| 打包过期清理 | 创建打包任务后等待超时，验证 zip 文件和 Redis 记录被清理 |
| 分享→访问 | 创建分享 → 提取码验证 → 预览 |
| 回收站 | 删除 → 回收站列表 → 恢复/彻底删除 |
| 回收站过期清理 | 创建回收站记录后触发 RecycleBinCleanupTask，验证过期记录的 MinIO 对象被物理删除、DB 记录被清理 |
| 团队 | 创建团队 → 邀请成员 → 团队文件操作 → 解散 |

集成测试使用 Testcontainers 启动真实 MySQL + Redis + MinIO。

---

## 4. 前端测试

| 层次 | 框架 | 测试内容 |
|------|------|---------|
| 单元测试 | Vitest + Vue Test Utils | Mock Axios + Pinia，测试组件渲染、交互事件、API 调用 |
| 组件 | - | FileList（列表/图标切换、选择）、DirectoryTree（目录展开/选择）、UploadDialog（文件选择、上传触发） |
| Store | - | fileStore（目录导航、列表加载）、uploadStore（进度更新、队列管理） |

---

## 5. 测试数据策略

- 单元测试使用 Mock 数据，不依赖数据库
- 集成测试每次运行前初始化测试数据，运行后清理
- 文件上传测试使用固定大小的小文件（1KB/1MB），避免资源浪费
