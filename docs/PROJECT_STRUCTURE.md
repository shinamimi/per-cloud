# Cloud 云盘 — 后端项目结构与文件职责说明

> 版本: v0.1
> 更新日期: 2026-07-29
> 说明: 记录后端全部源码文件的职责，作为新成员与 AI 助手的导航文档

---

## 整体预览（全量文件树 + 职责速查）

```text
backend/src/main/java/com/cloud/backend
├── annotation/
│   └── Log.java                            ### @Log 操作日志注解
├── aspect/
│   └── LogAspect.java                      ### 环绕切面拦截 @Log 方法并落库
├── authorization/
│   └── AuthorizationPolicy.java            ### 业务权限校验（角色判断/越权拦截）
├── bo/
│   └── AdminDashboardStatsBO.java          ### 仪表盘统计 BO（含配额使用率）
├── config/
│   ├── AuthenticationManagerConfig.java    ### 暴露 AuthenticationManager Bean
│   ├── FileCleanupTask.java                ### 定时清理（回收站/日志/分片）
│   ├── FileProperties.java                 ### 绑定 file.* 配置
│   ├── JwtProperties.java                  ### 绑定 jwt.* 配置
│   ├── MailProperties.java                 ### 绑定 mail.* 配置
│   ├── MinioConfig.java                    ### 双 MinioClient + 自动建桶
│   ├── MinioProperties.java                ### 绑定 minio.* 配置
│   ├── MyBatisTypeHandlerConfig.java       ### 全局枚举 TypeHandler 注册
│   ├── OpenAPIConfig.java                  ### Swagger/OpenAPI 文档
│   ├── PasswordEncoderConfig.java          ### 全局 BCryptPasswordEncoder
│   ├── RedisProperties.java                ### 绑定 redis.* 配置
│   ├── SecurityConfig.java                 ### 过滤器链 + 权限矩阵
│   ├── SuperAdminInitializer.java          ### 启动时幂等初始化超管
│   ├── TeamMemberRoleTypeHandler.java      ### TeamMemberRole 自定义 TypeHandler
│   └── WebSocketConfig.java                ### 注册 /ws/progress 通道
├── constant/
│   ├── FileConstants.java                  ### 文件大小/配额/分片常量
│   └── RedisConstants.java                 ### Redis Key 前缀常量
├── controller/
│   ├── AuthController.java                 ### 认证（登录/注册/验证码/重置/登出）
│   ├── FileController.java                 ### 个人文件 CRUD/分片上传/下载/回收站
│   ├── FriendController.java               ### 好友（请求/确认/删除）
│   ├── GuestShareController.java           ### 访客分享访问（公开）
│   ├── MetaController.java                 ### 字典接口 GET /api/meta/options
│   ├── ShareController.java                ### 我的分享管理
│   ├── TeamController.java                 ### 团队与成员管理
│   ├── TeamFileController.java             ### 团队文件管理 + 团队回收站
│   └── admin/
│       ├── AdminController.java            ### [已废弃] 占位类
│       ├── AdminAccountController.java     ### 管理员账号管理
│       ├── AdminDashboardController.java   ### 仪表盘统计
│       ├── AdminFileController.java        ### 全局文件管控
│       ├── AdminLogController.java         ### 操作日志查询
│       ├── AdminSettingsController.java    ### 系统配置中心
│       ├── AdminShareController.java       ### 分享治理
│       ├── AdminTeamController.java        ### 团队治理
│       └── AdminUserController.java        ### 后台用户管理
├── dao/
│   └── FileDao.java                        ### 文件复杂查询（分页搜索）
├── dto/
│   ├── AdminFileQuery.java                 ### 管理端文件查询条件
│   ├── FileQuery.java                      ### 文件搜索条件
│   ├── LoginRequest.java                   ### 登录请求体
│   ├── LoginResponse.java                  ### 登录响应体
│   ├── Page.java                           ### 分页结果包装
│   ├── PageRequest.java                    ### 分页请求参数
│   ├── RegisterRequest.java                ### 注册请求体
│   ├── ResetPasswordRequest.java           ### 重置密码请求体
│   ├── Result.java                         ### 统一响应体 {code,message,data}
│   ├── SendCodeRequest.java                ### 发送验证码请求体
│   ├── admin/                              ### 管理端 DTO（27 个）
│   │   ├── AdminFileResponse.java          ### 文件列表/详情响应
│   │   ├── AdminLogResponse.java           ### 日志列表响应
│   │   ├── AdminRecycleResponse.java       ### 全局回收站响应
│   │   ├── AdminResetPasswordRequest.java  ### 重置用户密码请求体
│   │   ├── AdminShareDownloadRequest.java  ### 分享下载开关请求体
│   │   ├── AdminShareResponse.java         ### 分享列表响应
│   │   ├── AdminTeamResponse.java          ### 团队列表响应
│   │   ├── AdminUploadLimitsRequest.java   ### 上传限制更新请求体
│   │   ├── AdminUserResponse.java          ### 用户/管理员列表响应
│   │   ├── BatchFileStatusRequest.java     ### 批量文件状态变更请求体
│   │   ├── CacheSettingsRequest.java       ### 缓存策略更新请求体
│   │   ├── CreateAdminRequest.java         ### 创建管理员请求体
│   │   ├── FileSettingsRequest.java        ### 文件管理配置更新
│   │   ├── FileStatusRequest.java          ### 单文件状态变更请求体
│   │   ├── LogFilterRequest.java           ### 日志筛选请求体
│   │   ├── LogItem.java                    ### 日志查询结果项
│   │   ├── LogSettingsRequest.java         ### 日志配置更新
│   │   ├── MailSettingsRequest.java        ### 邮件配置更新
│   │   ├── QuotaBatchRequest.java          ### 老用户配额批量调整
│   │   ├── QuotaBatchResponse.java         ### 批量配额调整结果
│   │   ├── QuotaRequest.java               ### 配额调整请求体
│   │   ├── RoleChangeRequest.java          ### 批量角色变更元素
│   │   ├── SessionSettingsRequest.java     ### 会话安全配置更新
│   │   ├── StatusRequest.java              ### 用户状态调整请求体
│   │   ├── StorageSettingsRequest.java     ### 存储限制更新
│   │   ├── SystemSettingsRequest.java      ### 系统功能开关更新
│   │   ├── TeamSettingsRequest.java        ### 团队默认值配置
│   │   └── UpdateRoleRequest.java          ### 管理员角色调整请求体
│   ├── file/                               ### 文件 DTO（18 个）
│   │   ├── AudioPlayResponse.java          ### 音频播放地址响应
│   │   ├── BatchDownloadRequest.java       ### 批量打包下载请求体
│   │   ├── BatchDownloadResponse.java      ### 打包任务状态响应
│   │   ├── DirectoryCreateRequest.java     ### 创建目录请求体
│   │   ├── FileCopyRequest.java            ### 复制文件请求体
│   │   ├── FileMoveRequest.java            ### 移动文件请求体
│   │   ├── FileNodeResponse.java           ### 文件列表节点响应
│   │   ├── FilePreviewResponse.java        ### 预览响应
│   │   ├── FileRenameRequest.java          ### 重命名请求体
│   │   ├── FileTreeResponse.java           ### 目录树节点响应
│   │   ├── RecycleBinResponse.java         ### 回收站记录响应
│   │   ├── SecUploadResponse.java          ### 秒传响应
│   │   ├── UploadInitRequest.java          ### 初始化上传请求体
│   │   ├── UploadInitResponse.java         ### 初始化响应
│   │   ├── UploadMergeRequest.java         ### 合并分片请求体
│   │   ├── UploadPolicyResponse.java       ### 上传策略响应
│   │   ├── UploadProgressResponse.java     ### 断点续传进度响应
│   │   └── UploadSecRequest.java           ### 秒传请求体
│   ├── friend/                             ### 好友 DTO（4 个）
│   │   ├── FriendRequestCreateRequest.java ### 发送好友请求请求体
│   │   ├── FriendRequestResponse.java      ### 好友请求列表项
│   │   ├── FriendSearchResponse.java       ### 搜索结果项（含关系）
│   │   └── FriendUserResponse.java         ### 好友列表项
│   ├── meta/                               ### 字典 DTO（2 个）
│   │   ├── MetaOptionsResponse.java        ### 字典响应体
│   │   └── OptionItem.java                 ### 字典单个选项
│   ├── share/                              ### 分享 DTO（7 个）
│   │   ├── GuestShareInfoResponse.java     ### 访客分享信息响应
│   │   ├── ShareAccessRequest.java         ### 访客下载/转存请求体
│   │   ├── ShareCreateRequest.java         ### 创建分享请求体
│   │   ├── ShareFileNodeResponse.java      ### 分享文件树节点响应
│   │   ├── ShareResponse.java              ### 我的分享列表项
│   │   ├── ShareUpdateRequest.java         ### 修改有效期请求体
│   │   └── ShareVerifyRequest.java         ### 提取码验证请求体
│   └── team/                               ### 团队 DTO（5 个）
│       ├── TeamCreateRequest.java          ### 创建团队请求体
│       ├── TeamInviteRequest.java          ### 邀请成员请求体
│       ├── TeamMemberResponse.java         ### 成员列表项
│       ├── TeamResponse.java               ### 团队详情/列表响应
│       └── TeamUpdateRequest.java          ### 更新团队信息请求体
├── entity/
│   ├── DisabledObject.java                 ### 对象级禁用记录
│   ├── File.java                           ### 文件/目录实体
│   ├── FileHash.java                       ### 秒传索引（引用计数）
│   ├── FriendRequest.java                  ### 好友请求实体
│   ├── Friendship.java                     ### 好友关系实体
│   ├── OperationLog.java                   ### 操作日志实体
│   ├── RecycleBin.java                     ### 回收站实体
│   ├── Setting.java                        ### 系统设置 key-value
│   ├── Share.java                          ### 分享记录实体
│   ├── ShareFile.java                      ### 分享快照实体
│   ├── Team.java                           ### 团队实体
│   ├── TeamMember.java                     ### 团队成员关系
│   └── User.java                           ### 用户实体（三来源配额）
├── enums/
│   ├── CaptchaType.java                    ### 验证码用途类型
│   ├── DisableScope.java                   ### 禁用范围 GLOBAL/USER
│   ├── ErrorCode.java                      ### 全局错误码（模块分区）
│   ├── FileStatus.java                     ### 文件状态
│   ├── FileType.java                       ### 文件类型 FILE/DIRECTORY
│   ├── FriendRequestStatus.java            ### 好友请求状态
│   ├── OperationType.java                  ### 操作日志类型
│   ├── Role.java                           ### 角色 USER/OPERATOR/ADMIN/SUPER_ADMIN
│   ├── ShareStatus.java                    ### 分享状态
│   ├── TargetType.java                     ### 日志目标类型
│   ├── TeamMemberRole.java                 ### 成员角色（按 value 存取）
│   ├── TeamStatus.java                     ### 团队状态
│   └── UserStatus.java                     ### 用户状态
├── event/
│   ├── UserEventListener.java              ### 注册事件监听（旁路日志）
│   └── UserRegisteredEvent.java            ### 用户注册成功事件
├── exception/
│   ├── BusinessException.java              ### 业务异常
│   └── GlobalExceptionHandler.java         ### 全局异常处理器（统一转 Result）
├── mapper/
│   ├── DisabledObjectMapper.java           ### 禁用记录 SQL（增删/命中校验）
│   ├── FileHashMapper.java                 ### 秒传索引 SQL
│   ├── FileMapper.java                     ### 文件全量 SQL
│   ├── FriendRequestMapper.java            ### 好友请求 SQL
│   ├── FriendshipMapper.java               ### 好友关系 SQL
│   ├── OperationLogMapper.java             ### 日志 SQL
│   ├── RecycleBinMapper.java               ### 回收站 SQL
│   ├── SettingMapper.java                  ### 设置 key-value SQL
│   ├── ShareFileMapper.java                ### 分享快照 SQL
│   ├── ShareMapper.java                    ### 分享 SQL（原子下载计数）
│   ├── TeamMapper.java                     ### 团队 SQL
│   ├── TeamMemberMapper.java               ### 成员 SQL
│   └── UserMapper.java                     ### 用户 SQL
├── security/
│   ├── AccessDeniedHandlerImpl.java        ### 已登录但无权限 → 403 JSON
│   ├── AuthenticationEntryPointImpl.java   ### 未登录/Token 无效 → 认证失败 JSON
│   ├── JwtAuthenticationFilter.java        ### JWT 验签 + 黑名单 → SecurityContext
│   ├── LoginUser.java                      ### UserDetails 当前用户封装
│   └── UserDetailsServiceImpl.java         ### 认证数据源
├── service/
│   ├── admin/
│   │   ├── AdminFileService.java           ### 接口：全局文件/回收站
│   │   ├── impl/AdminFileServiceImpl.java  ### 实现：禁用/恢复/彻底删除
│   │   ├── AdminSettingsService.java       ### 接口：配置中心
│   │   └── impl/AdminSettingsServiceImpl.java ### 实现：回落默认值/配额批量调整
│   ├── file/
│   │   ├── DownloadService.java            ### 接口：直链/打包下载
│   │   ├── impl/DownloadServiceImpl.java   ### 实现：预签名 + 异步 zip 打包
│   │   ├── FileHashService.java            ### 接口：秒传引用管理
│   │   ├── impl/FileHashServiceImpl.java   ### 实现：共享引用/归零删除
│   │   ├── FileService.java                ### 接口：文件 CRUD
│   │   ├── impl/FileServiceImpl.java       ### 实现：递归复制/回收站
│   │   ├── PreviewService.java             ### 接口：预览分发
│   │   ├── impl/PreviewServiceImpl.java    ### 实现：缩略图/文本直读
│   │   ├── RecycleBinService.java          ### 接口：回收站
│   │   ├── impl/RecycleBinServiceImpl.java ### 实现：递归恢复/清理
│   │   ├── SearchService.java              ### 接口：搜索
│   │   ├── impl/SearchServiceImpl.java     ### 实现：FileDao 分页搜索
│   │   ├── StorageService.java             ### 接口：对象存储抽象
│   │   ├── impl/StorageServiceImpl.java    ### 实现：MinIO SDK 封装
│   │   ├── UploadService.java              ### 接口：分片上传全流程
│   │   └── impl/UploadServiceImpl.java     ### 实现：断点续传/秒传/分布式锁
│   ├── friend/
│   │   ├── FriendService.java              ### 接口：好友管理
│   │   └── impl/FriendServiceImpl.java     ### 实现：双向确认/成对存储
│   ├── share/
│   │   ├── ShareService.java               ### 接口：分享/访客访问
│   │   └── impl/ShareServiceImpl.java      ### 实现：快照/限次验证/转存
│   ├── system/
│   │   ├── AuthService.java                ### 接口：登录/注册/重置
│   │   ├── impl/AuthServiceImpl.java       ### 实现：JWT 签发/锁定/验证码
│   │   ├── CaptchaService.java             ### 邮箱验证码（场景隔离）
│   │   ├── DashboardService.java           ### 接口：全局统计
│   │   ├── impl/DashboardServiceImpl.java  ### 实现：内存聚合统计
│   │   ├── EmailService.java               ### 邮件发送（配置回落链）
│   │   ├── JwtBlacklistService.java        ### 登出黑名单
│   │   ├── LoginAttemptService.java        ### 登录失败锁定
│   │   ├── MetaService.java                ### 接口：枚举选项
│   │   ├── impl/MetaServiceImpl.java       ### 实现：字典组装
│   │   ├── OperationLogService.java        ### 接口：日志写入/查询
│   │   └── impl/OperationLogServiceImpl.java ### 实现：开关控制/分页
│   ├── team/
│   │   ├── TeamFileService.java            ### 接口：团队文件/回收站
│   │   ├── TeamService.java                ### 接口：团队/成员/配额
│   │   ├── impl/TeamFileServiceImpl.java   ### 实现：团队目录树/回收站
│   │   └── impl/TeamServiceImpl.java       ### 实现：成员管理/权限链
│   └── user/
│       ├── UserService.java                ### 接口：用户/配额治理
│       └── impl/UserServiceImpl.java       ### 实现：三来源配额/密码重置
├── util/
│   └── ShareTokenGenerator.java            ### 分享短码生成（查重 10 次）
├── utils/
│   ├── FileUtil.java                       ### 扩展名/MIME/分类/去重
│   ├── IdUtil.java                         ### UUID/MinIO 对象路径
│   ├── IpUtil.java                         ### 客户端真实 IP
│   ├── JwtTokenUtil.java                   ### JWT 工具接口
│   └── JwtTokenUtilImpl.java               ### JWT 实现（jjwt）
└── websocket/
    └── ProgressWebSocketHandler.java       ### /ws/progress 广播进度
```

---

## 一、目录总览

```
backend/src/main
├── java/com/cloud/backend
│   ├── annotation/    — @Log 操作日志注解
│   ├── aspect/        — LogAspect 日志切面
│   ├── authorization/ — 业务权限校验工具
│   ├── bo/            — 业务对象（管理后台统计）
│   ├── config/        — 全部配置类（Security/MinIO/JWT/Mail/WebSocket/定时任务等）
│   ├── constant/      — 常量接口
│   ├── controller/    — 用户端控制器
│   │   └── admin/     — 管理端控制器
│   ├── dao/           — 复杂查询 DAO
│   ├── dto/           — 请求/响应 DTO（含 admin/file/friend/meta/share/team 子包）
│   ├── entity/        — 数据库实体
│   ├── enums/         — 枚举
│   ├── event/         — 领域事件（用户注册事件）
│   ├── exception/     — 业务异常 + 全局异常处理器
│   ├── mapper/        — MyBatis Mapper 接口（13 个）
│   ├── security/      — Spring Security 组件
│   ├── service/       — 服务接口与实现
│   │   ├── admin/     — 管理端（文件管控、系统设置）
│   │   ├── file/      — 文件（上传/下载/预览/搜索/回收站/秒传）
│   │   ├── friend/    — 好友
│   │   ├── share/     — 分享
│   │   ├── system/    — 系统（认证/验证码/邮件/日志/仪表盘/字典）
│   │   ├── team/      — 团队
│   │   └── user/      — 用户
│   ├── util/          — 单一工具（分享短码生成器）
│   ├── utils/         — 通用工具类
│   └── websocket/     — WebSocket 进度推送
└── resources/
    ├── application.yml / application-{local,dev,prod,test}.yml
    ├── logback-spring.xml
    └── mapper/        — MyBatis XML（14 个）
```

---

## 二、文件职责明细

### 2.1 `annotation/`

- `Log.java` — 定义 `@Log` 方法级注解，声明操作类型（OperationType）、目标类型（TargetType）以及支持 SpEL 表达式的 targetId/detail 字段；由 LogAspect 切面统一拦截落库，业务代码无需直接注入日志服务。

### 2.2 `aspect/`

- `LogAspect.java` — 环绕切面拦截所有标注 `@Log` 的方法：目标方法执行成功后从安全上下文取当前用户组装 OperationLog 落库（未登录跳过），并通过参数名绑定与 `#result` 引用解析 targetId/detail 的 SpEL 表达式。

### 2.3 `authorization/`

- `AuthorizationPolicy.java` — 集中管理业务权限校验的静态工具类：从 Security 上下文读取当前登录用户，按角色枚举 value 值（非 ordinal）提供 isAdmin/isSuperAdmin 判断，并在管理操作前用 canManageUser 拦截对管理员账号的越权操作。

### 2.4 `bo/`

- `AdminDashboardStatsBO.java` — 管理后台仪表盘统计业务对象，聚合用户数、文件数、总容量与总配额，构造器中统一计算配额使用率（含总配额为 0 的除零保护）。

### 2.5 `config/`

- `AuthenticationManagerConfig.java` — 将 Spring Security 的 AuthenticationManager 通过 AuthenticationConfiguration 暴露为可注入 Bean，登录服务直接调用 authenticate() 认证，不重复实现校验逻辑。
- `FileCleanupTask.java` — 定时清理任务：每日 03:00 物理清理回收站过期记录与打包下载产物，03:30 按保留天数清理操作/登录日志，04:00 清理 Redis 中元数据已失效的孤儿上传分片。
- `FileProperties.java` — 绑定 `file.*` 配置：落盘路径、分片大小、单文件大小与并发上限（普通/VIP 差异化）、回收站保留天数、打包/上传过期时间、文本预览大小上限等。
- `JwtProperties.java` — 绑定 `jwt.*` 配置：签名密钥、过期时间、请求头、前缀与签发者，供 JwtTokenUtil 签发与验签。
- `MailProperties.java` — 绑定 `mail.*` 的 SMTP 服务器地址、端口、账号、授权码与发件显示地址。
- `MinioConfig.java` — 根据 MinioProperties 构建两个 MinioClient Bean（内网 endpoint 客户端 + 用 public-url 签名的预签名客户端），并在 ApplicationReadyEvent 时按条件自动创建存储桶。
- `MinioProperties.java` — 绑定 `minio.*` 的 endpoint/access-key/secret-key/bucket/auto-create-bucket/public-url。
- `MyBatisTypeHandlerConfig.java` — 通过 ConfigurationCustomizer 向 MyBatis 全局注册枚举 TypeHandler：多数枚举用 EnumOrdinalTypeHandler 映射 TINYINT，TeamMemberRole 因自定义 value 注册专用处理器。
- `OpenAPIConfig.java` — 基于 SpringDoc 配置 OpenAPI/Swagger 文档的标题、描述、版本与许可证信息。
- `PasswordEncoderConfig.java` — 提供全局唯一的 BCryptPasswordEncoder Bean，确保注册/登录/重置密码编码方式一致（BCrypt 自带随机盐）。
- `RedisProperties.java` — 绑定 `redis.*` 连接参数，主要为支持环境变量注入。
- `SecurityConfig.java` — 组装 Spring Security 过滤器链：关闭 CSRF、启用 CORS、无状态会话，按路径前缀配置权限矩阵（白名单/管理端角色分级/其余需认证），挂载 JwtAuthenticationFilter，统一配置认证与授权异常出口。
- `SuperAdminInitializer.java` — ApplicationRunner 启动初始化：幂等地确保配置的超级管理员存在且可用，已存在仅在密码不匹配时重置，同名非超管则跳过告警。
- `TeamMemberRoleTypeHandler.java` — TeamMemberRole 枚举的自定义 MyBatis TypeHandler，按枚举 value 整数存取，解决 value 与声明顺序不一致的问题。
- `WebSocketConfig.java` — 开启 WebSocket 并把 ProgressWebSocketHandler 注册到统一进度通道 `/ws/progress`（setAllowedOrigins 放行前端跨源）。

### 2.6 `constant/`

- `FileConstants.java` — 文件模块常量：1024 进制容量换算、默认配额（10GB）、默认分片（10MB）、根目录父 ID、文件分类编号（图片/文档/视频/音频/压缩包/其他）。
- `RedisConstants.java` — Redis Key 前缀常量：上传元数据/分片/进行中任务/合并分布式锁、分享提取码错误计数/通过标记/下载去重标记等。

### 2.7 `controller/`（用户端）

- `AuthController.java` — 公开认证入口：登录（失败超限锁定并签发 JWT）、发送验证码、注册（受开放注册与邮箱验证开关约束）、忘记密码、重置密码、登出（Token 加入黑名单立即失效）。
- `FileController.java` — 个人/团队文件管理核心控制器：文件列表/目录树/建目录、分片上传全套（init/policy/chunk/merge/sec 秒传/progress）、下载与批量打包（302 预签名重定向）、重命名/移动/复制/删除（进回收站）、搜索、预览、音乐播放预留、回收站恢复/彻底删除。
- `FriendController.java` — 好友管理（双向确认）：好友列表、搜索用户、发送好友请求、待处理列表、接受/拒绝请求、删除好友；关系层供团队拉人、定向分享复用。
- `GuestShareController.java` — 访客分享访问（公开 permitAll）：分享信息查询、提取码验证（错误限次 5 次 Redis 计数）、分享文件树、预览、下载（计数+1 达限置 EXHAUSTED）、批量打包下载与任务查询、转存到个人空间。
- `MetaController.java` — 字典接口 `GET /api/meta/options`：管理后台登录后一次性拉取业务枚举，新增枚举组只改 MetaService。
- `ShareController.java` — 我的分享管理（需登录）：创建分享（单文件+文件夹快照锁定）、我的分享列表、修改有效期、取消分享、物理删除分享记录。
- `TeamController.java` — 用户端团队管理：创建团队、我的团队列表/详情、更新信息、解散团队（仅 OWNER）、邀请成员、成员列表、移除成员、退出团队。
- `TeamFileController.java` — 团队文件管理 + 团队回收站：列表/目录树/建目录、重命名/移动/复制/删除、下载/预览、回收站恢复/彻底删除。

### 2.8 `controller/admin/`（管理端）

- `AdminController.java` — 已废弃的空占位类（@Deprecated），说明管理端能力已拆分到以下五个专注控制器。
- `AdminAccountController.java` — 管理员账号管理：管理员列表（OPERATOR/ADMIN 分级展示）、创建/删除管理员、修改角色、候选用户列表、批量变更角色；服务层拦截删除自己/超管与授予超管角色。
- `AdminDashboardController.java` — 仪表盘统计：用户数、文件数、容量使用率等全局指标。
- `AdminFileController.java` — 全局文件管控（仅 ADMIN+，覆盖个人+团队文件）：全局列表筛选分页、详情、管理员下载/预览、禁用/启用（GLOBAL 全站或 USER 仅用户，支持批量）、删除进全局回收站、全局回收站列表/恢复/彻底删除。
- `AdminLogController.java` — 后台操作日志查询：按用户/操作类型/目标类型/时间范围等筛选审计日志。
- `AdminSettingsController.java` — 系统配置中心：全部配置分组查询（SMTP 密码脱敏）、upload/storage/session/cache/system/file/mail/log/team 各分组修改（null 恢复默认）、老用户配额批量调整（preview 仅看明细）、日志分页查询。
- `AdminShareController.java` — 分享治理：全部分享列表（补分享者昵称与文件名）、取消分享、切换下载开关、物理删除分享记录。
- `AdminTeamController.java` — 团队治理：团队列表（含成员数）、详情、调整配额、团队文件与回收站只读查看、物理清除回收站、强制解散团队（记录操作者 ID）。
- `AdminUserController.java` — 后台用户管理：用户列表、启用/禁用/锁定、调整配额（管理端赠送额度）、解锁并清零失败计数、重置密码。

### 2.9 `dao/`

- `FileDao.java` — 文件复杂查询 DAO 接口，XML 提供文件名 LIKE + 类型过滤的分页搜索（searchPage/count），用于文件搜索与音频列表。

### 2.10 `dto/`（根目录）

- `AdminFileQuery.java` — 管理端全局文件列表的查询条件（userId/username/teamId/category/status/sort + 分页）。
- `FileQuery.java` — 用户文件搜索的筛选条件（userId 必填、parentId/keyword/category/isDirectory/分页）。
- `LoginRequest.java` — 登录请求体（username + password + 可选图形验证码 captchaId/captchaCode）。
- `LoginResponse.java` — 登录成功响应（token、userId、username、role，供前端路由鉴权）。
- `Page.java` — 通用分页结果包装：records + total + 分页参数回显。
- `PageRequest.java` — 通用分页请求参数（page 从 1 起、默认 size=20，提供 getOffset() 换算 SQL 偏移量）。
- `RegisterRequest.java` — 注册请求体（username/password/email/nickname + 邮箱验证码，含密码强度校验注解）。
- `ResetPasswordRequest.java` — 找回密码最后一步请求体（email + 验证码 + 新密码）。
- `Result.java` — 全局统一响应体 `{code, message, data}`，提供 success/fail 静态工厂（code===200 成功）。
- `SendCodeRequest.java` — 发送邮箱验证码请求体（email + CaptchaType，类型拼入 Redis Key 隔离场景）。

### 2.11 `dto/admin/`（27 个）

- `AdminFileResponse.java` — 管理端文件列表/详情响应（含所属用户/团队名、禁用来源 scope）。
- `AdminLogResponse.java` — 后台操作日志列表响应（操作者/类型/目标/详情/IP/时间）。
- `AdminRecycleResponse.java` — 全局回收站记录响应（管理员删除的记录，含归属用户名/团队名）。
- `AdminResetPasswordRequest.java` — 管理员重置用户密码请求体（新密码 9 位起含字母数字）。
- `AdminShareDownloadRequest.java` — 分享下载开关更新 PUT /api/admin/shares/{id}/download 请求体（allowDownload）。
- `AdminShareResponse.java` — 管理端分享列表响应（分享者/文件名由服务层填充）。
- `AdminTeamResponse.java` — 后台团队列表响应（团队信息 + 状态 + 配额使用 + 成员数）。
- `AdminUploadLimitsRequest.java` — 上传限制更新请求体（普通/VIP 单文件大小与并发数上限，null 保持原值）。
- `AdminUserResponse.java` — 后台用户/管理员列表响应（基本信息 + 角色/状态 + 三来源配额拆分与 totalQuota 汇总 + usedSpace）。
- `BatchFileStatusRequest.java` — 管理端批量文件状态变更请求体（ids + FileStatus + DisableScope）。
- `CacheSettingsRequest.java` — 缓存策略配置更新（验证码/登录失败/黑名单/预览/下载链接的 TTL）。
- `CreateAdminRequest.java` — 创建管理员请求体（username/password/email/nickname + 目标角色）。
- `FileSettingsRequest.java` — 文件管理配置更新（回收站天数、分享默认/最长有效期、分享次数上限、默认提取码与下载策略）。
- `FileStatusRequest.java` — 管理端单文件状态变更请求体（FileStatus + DisableScope）。
- `LogFilterRequest.java` — 后台操作日志查询筛选（userId/operation/targetType/时间区间）。
- `LogItem.java` — 日志查询结果项（操作日志 join 用户表，含 username）。
- `LogSettingsRequest.java` — 日志配置更新（操作/登录日志保留天数）。
- `MailSettingsRequest.java` — 邮件服务配置更新（SMTP 主机/端口/加密/账号/密码/发件人/频率限制）。
- `QuotaBatchRequest.java` — 老用户配额批量调整（日期范围必填 + role/status 过滤 + 目标配额 + preview 幂等预览开关）。
- `QuotaBatchResponse.java` — 老用户配额批量调整结果（受影响数，preview 时返回明细）。
- `QuotaRequest.java` — 配额调整请求体（用户/团队的管理端赠送额度 adminBonusQuota）。
- `RoleChangeRequest.java` — 批量变更角色的单个元素（userId + newRole，不允许 SUPER_ADMIN）。
- `SessionSettingsRequest.java` — 会话安全配置更新（Token/验证码 TTL、登录失败锁定阈值与时长、重置密码 TTL）。
- `StatusRequest.java` — 用户状态调整请求体（正常/禁用/锁定/未激活）。
- `StorageSettingsRequest.java` — 存储限制更新（新用户/新 VIP 默认配额，只影响新注册用户）。
- `SystemSettingsRequest.java` — 系统功能开关更新（允许注册、游客分享、邮箱验证、验证码、操作日志开关）。
- `TeamSettingsRequest.java` — 团队默认值配置（每人创建上限、默认配额、回收站天数、最大成员数）。
- `UpdateRoleRequest.java` — 单个管理员角色调整请求体（不允许 SUPER_ADMIN）。

### 2.12 `dto/file/`（18 个）

- `AudioPlayResponse.java` — 音频播放地址响应（fileId/name + 直连 MinIO 的预签名 url）。
- `BatchDownloadRequest.java` — 批量打包下载请求体（fileIds，支持目录递归）。
- `BatchDownloadResponse.java` — 批量打包下载异步任务状态响应（taskId + status + 进度 + url）。
- `DirectoryCreateRequest.java` — 创建目录请求体（parentId/name/teamId）。
- `FileCopyRequest.java` — 复制文件请求体（targetParentId，仅同用户复制）。
- `FileMoveRequest.java` — 移动文件请求体（targetParentId，仅改 DB parentId）。
- `FileNodeResponse.java` — 文件列表节点响应（基本属性 + type/category + 上传者信息）。
- `FilePreviewResponse.java` — 文件预览响应（type: IMAGE/VIDEO/AUDIO/PDF/TEXT/UNSUPPORTED + 预签名 url + 缩略图 + 文本内容）。
- `FileRenameRequest.java` — 重命名请求体（新文件名）。
- `FileTreeResponse.java` — 目录树节点响应（id/name/isDirectory/children）。
- `RecycleBinResponse.java` — 回收站记录响应（id/fileId/originalName/type/size/deletedTime/expireTime）。
- `SecUploadResponse.java` — 秒传响应（instant=true 秒传完成返回 file，false 需继续分片上传）。
- `UploadInitRequest.java` — 初始化分片上传请求体（fileName/fileSize/fileHash/parentId/teamId）。
- `UploadInitResponse.java` — 初始化响应（uploadId + chunkSize + totalChunks，前端据此切分）。
- `UploadMergeRequest.java` — 合并分片请求体（仅 uploadId）。
- `UploadPolicyResponse.java` — 上传策略响应（单文件大小上限 maxSize、并发上限 maxConcurrent，VIP 差异化）。
- `UploadProgressResponse.java` — 断点续传进度响应（uploadId + 文件元信息 + uploadedChunks 已上传分片）。
- `UploadSecRequest.java` — 秒传请求体（fileHash/fileName/fileSize/parentId/teamId）。

### 2.13 `dto/friend/`（4 个）

- `FriendRequestCreateRequest.java` — 发送好友请求请求体（仅 toUserId）。
- `FriendRequestResponse.java` — 好友请求列表项（requestId + 发起方资料 + 状态）。
- `FriendSearchResponse.java` — 加好友搜索结果项（用户资料 + relation：SELF/FRIEND/PENDING_SENT/PENDING_RECEIVED/NONE）。
- `FriendUserResponse.java` — 好友列表项（好友 userId + 基本资料）。

### 2.14 `dto/meta/`（2 个）

- `MetaOptionsResponse.java` — 字典接口响应体，按 Map<组名, List<OptionItem>> 组织。
- `OptionItem.java` — 字典单个选项（value + label，UI 样式归前端维护）。

### 2.15 `dto/share/`（7 个）

- `GuestShareInfoResponse.java` — 访客获取分享信息响应（未验证提取码也返回 hasPassword 等，供前端弹密码框）。
- `ShareAccessRequest.java` — 访客批量下载/转存请求体（传分享快照节点 id 列表，防原文件 id 越权）。
- `ShareCreateRequest.java` — 创建分享请求体（fileId、有效期类型/天数、提取码、下载/转存策略）。
- `ShareFileNodeResponse.java` — 分享文件树节点响应（id 为快照节点 id，预览/下载/转存均用此 id）。
- `ShareResponse.java` — 我的分享列表项（含状态、token、下载策略与计数）。
- `ShareUpdateRequest.java` — 修改分享有效期请求体（仅 validType/validDays）。
- `ShareVerifyRequest.java` — 提取码验证请求体（仅 password）。

### 2.16 `dto/team/`（5 个）

- `TeamCreateRequest.java` — 创建团队请求体（name 必填、description/avatar 可选）。
- `TeamInviteRequest.java` — 邀请成员请求体（被邀请 userIds 列表，非强制好友）。
- `TeamMemberResponse.java` — 团队成员列表项（成员资料 + 角色 + 加入时间）。
- `TeamResponse.java` — 团队详情/列表共用响应（资料 + 配额 + 成员数 + myRole 供前端权限控制）。
- `TeamUpdateRequest.java` — 更新团队信息请求体（name/description/avatar，仅更新传入字段）。

### 2.17 `entity/`（13 个）

- `DisabledObject.java` — 对象级禁用记录实体（t_disabled_object），按内容 hash + 范围（GLOBAL/USER）禁用。
- `File.java` — 文件/目录实体（t_file），目录树模型，记录归属（用户/团队）、MinIO 对象路径、秒传 hash、状态与分类。
- `FileHash.java` — 秒传索引实体（t_file_hash），全局 SHA256 索引 + 引用计数 refCount，归零才删 MinIO 对象。
- `FriendRequest.java` — 好友请求实体（t_friend_request），双向确认状态机 PENDING/ACCEPTED/REJECTED。
- `Friendship.java` — 好友关系实体（t_friendship），成对存储（user_a_id < user_b_id）防重复。
- `OperationLog.java` — 操作日志实体（t_operation_log），记录关键操作用于审计。
- `RecycleBin.java` — 回收站实体（t_recycle_bin），逻辑删除记录与原文件信息，供恢复与到期物理清理。
- `Setting.java` — 系统设置实体（t_setting），key-value 存储管理员可配置项，无记录时回退配置文件默认值。
- `Share.java` — 分享记录实体（t_share），token、提取码、有效期、下载策略/计数与状态（NORMAL/EXPIRED/CANCELED/EXHAUSTED）。
- `ShareFile.java` — 分享快照实体（t_share_file），目录分享时锁定创建时刻的文件树，树结构自包含防原文件 id 越权。
- `Team.java` — 团队实体（t_team），资料、所有者、状态（正常/解散）与配额/已用空间。
- `TeamMember.java` — 团队成员关系实体（t_team_member），角色（MEMBER/ADMIN/OWNER，按 value 存储）。
- `User.java` — 用户实体（t_user），登录凭据、角色、三来源配额（基础/管理赠送/奖励）、已用空间与状态。

### 2.18 `enums/`（13 个）

- `CaptchaType.java` — 验证码用途类型（REGISTER/RESET_PASSWORD/LOGIN），拼入 Redis Key 防场景串用。
- `DisableScope.java` — 对象禁用范围（GLOBAL=1 全站 / USER=2 仅用户）。
- `ErrorCode.java` — 全局错误码枚举，按业务模块分区（认证 10100、文件 10200、分享 10300、团队 10400、好友 10600）。
- `FileStatus.java` — 文件状态（DELETED=0/NORMAL=1/DISABLED=2）。
- `FileType.java` — 文件类型（FILE=0/DIRECTORY=1），按 ordinal 存 TINYINT。
- `FriendRequestStatus.java` — 好友请求状态（PENDING/ACCEPTED/REJECTED），按名称存 VARCHAR。
- `OperationType.java` — 操作日志类型（LOGIN/REGISTER/UPLOAD_FILE/DELETE_FILE/TEAM_* 等）。
- `Role.java` — 用户角色（USER=0/OPERATOR=10/ADMIN=20/SUPER_ADMIN=100），value 越大权限越高。
- `ShareStatus.java` — 分享状态（NORMAL=0/EXPIRED=1/CANCELED=2/EXHAUSTED=3）。
- `TargetType.java` — 操作日志目标类型（USER/FILE/SHARE/TEAM）。
- `TeamMemberRole.java` — 团队成员角色（MEMBER=0/ADMIN=10/OWNER=20），按自定义 value 存取。
- `TeamStatus.java` — 团队状态（DISSOLVED=0/NORMAL=1）。
- `UserStatus.java` — 用户状态（DISABLED=0/NORMAL=1/LOCKED=2/INACTIVE=3）。

### 2.19 `event/`

- `UserEventListener.java` — 订阅用户注册事件（@EventListener），当前仅记录注册成功日志的旁路处理器。
- `UserRegisteredEvent.java` — 用户注册成功事件，承载 userId/username/email 不可变快照。

### 2.20 `exception/`

- `BusinessException.java` — 携带 ErrorCode 的运行时业务异常，由全局处理器统一转 Result。
- `GlobalExceptionHandler.java` — @RestControllerAdvice 全局异常处理器：业务异常、@Valid 参数校验、JSON 反序列化、上传超限等统一转 Result<T>，兜底 500 并 log.error 完整堆栈。

### 2.21 `mapper/`（13 个）

- `DisabledObjectMapper.java` — 对象级禁用表操作：增删、按 hash 查询、命中（全站/仅用户）校验。
- `FileHashMapper.java` — 秒传索引表操作：hash 命中查询、引用计数原子增减、索引删除。
- `FileMapper.java` — t_file 增删查改、分页、同名检查、递归状态更新、对象级禁用恢复等全量文件 SQL。
- `FriendRequestMapper.java` — 好友请求插入、双向最近记录查重、待处理列表、状态更新。
- `FriendshipMapper.java` — 好友关系（成对存储）：好友列表、成对判定、删除。
- `OperationLogMapper.java` — 日志写入、多维度过滤、管理端分页（JOIN 用户表）、过期批量删除。
- `RecycleBinMapper.java` — 个人/团队/全局（deleted_by=1）三类回收站的查询、级联删除、过期扫描。
- `SettingMapper.java` — key-value 配置的插入、按 key 查询、upsert 与删除。
- `ShareFileMapper.java` — 分享快照批量插入、按分享查询、防越权联合查询、级联删除。
- `ShareMapper.java` — 分享 CRUD、按 token 访问、原子下载计数（防并发超限）、同文件活跃分享数统计。
- `TeamMapper.java` — 团队 CRUD、用户所在团队 JOIN 查询、解散、配额/已用空间原子更新、同名检查。
- `TeamMemberMapper.java` — 成员查询、角色更新、退出状态更新、团队数统计。
- `UserMapper.java` — 用户 CRUD、账号/邮箱查询、原子调整已用空间、配额批量更新、关键字搜索。

### 2.22 `security/`

- `AccessDeniedHandlerImpl.java` — 已登录但权限不足（如普通用户访问管理接口）的拒绝处理器，返回统一 JSON 403。
- `AuthenticationEntryPointImpl.java` — 未登录/Token 无效的入口守卫，返回统一 JSON 认证失败响应。
- `JwtAuthenticationFilter.java` — 每次请求的 JWT 入口过滤器：验签 + 查黑名单后构建 SecurityContext，或直接返回统一 JSON 错误。
- `LoginUser.java` — 实现 UserDetails 的当前登录用户封装，将角色映射为 ROLE_ 权限字符串，用状态判断账号可用性。
- `UserDetailsServiceImpl.java` — 认证数据源，按用户名/邮箱加载用户并包装为 LoginUser。

### 2.23 `service/admin/`

- `AdminFileService.java` — 接口：全局文件列表、禁用/启用、全局回收站与恢复/彻底删除。
- `impl/AdminFileServiceImpl.java` — 实现：全局筛选列表、对象级禁用/启用、删除进全局回收站、递归恢复与彻底删除。
- `AdminSettingsService.java` — 接口：集中暴露上传/存储/会话/缓存/系统/文件/邮件/日志/团队全部配置项 getter 与 update*。
- `impl/AdminSettingsServiceImpl.java` — 实现：t_setting 有值优先否则回落 yml 默认值、SMTP 密码脱敏、老用户配额批量调整。

### 2.24 `service/file/`

- `DownloadService.java` — 接口：单文件预签名 URL 与批量打包异步下载（WebSocket 通知）。
- `impl/DownloadServiceImpl.java` — 实现：单文件直链（禁用拦截）与内存任务表驱动的异步 zip 打包及过期清理。
- `FileHashService.java` — 接口：秒传对象注册（命中共享引用）、引用 +1 与释放归零判定。
- `impl/FileHashServiceImpl.java` — 实现：全局 SHA256 命中即共享对象，并发注册回退复用，引用归零才删物理对象。
- `FileService.java` — 接口：个人空间文件 CRUD、分页、目录树、重命名/移动/复制与删除到回收站。
- `impl/FileServiceImpl.java` — 实现：文件 CRUD、递归复制（共享引用+1）、删除进回收站（释放配额）、同名唯一化。
- `PreviewService.java` — 接口：按类型返回预览响应（图片/视频/音频/PDF/文本）。
- `impl/PreviewServiceImpl.java` — 实现：按扩展名分发预览类型、生成/复用缩略图、直读小文本、拦截禁用文件。
- `RecycleBinService.java` — 接口：移入/恢复/立即删除/定时清理/单记录物理清理。
- `impl/RecycleBinServiceImpl.java` — 实现：递归恢复（配额校验+同名还原）、物理清理（秒传引用归零才删对象）、过期清理。
- `SearchService.java` — 接口：按文件名 + 分类过滤的分页搜索。
- `impl/SearchServiceImpl.java` — 实现：组装 FileQuery 并调用 FileDao 完成分页搜索。
- `StorageService.java` — 接口：对象存储抽象（upload/download/delete/预签名 URL/复制/存在判断），当前 MinIO 实现。
- `impl/StorageServiceImpl.java` — 实现：封装 MinIO SDK 的上传、下载、删除、预签名 URL（专用 client 保证签名 host）。
- `UploadService.java` — 接口：init/chunk/merge/sec（秒传）/progress 上传全流程。
- `impl/UploadServiceImpl.java` — 实现：自适应分片、Redis 断点续传、SHA-256 校验合并、分布式锁防并发、秒传共享。

### 2.25 `service/friend/`

- `FriendService.java` — 接口：好友列表、搜索、请求发送/接受/拒绝、删除与关系校验。
- `impl/FriendServiceImpl.java` — 实现：双向确认好友流程（成对存储）、好友/同团队关系复用校验。

### 2.26 `service/share/`

- `ShareService.java` — 接口：用户侧分享管理（创建/取消/删除）与访客访问（验证码、文件树、下载、预览、转存）契约。
- `impl/ShareServiceImpl.java` — 实现：目录快照锁定、提取码 Redis 限次验证、原子下载计数、批量打包与秒传转存。

### 2.27 `service/system/`

- `AuthService.java` — 接口：登录、注册、验证码发送、重置密码契约。
- `impl/AuthServiceImpl.java` — 实现：登录/注册签发 JWT、登录失败锁定、验证码场景隔离校验、重置密码。
- `CaptchaService.java` — 邮箱验证码服务：生成 6 位验证码存 Redis，支持场景隔离、冷却期防刷、一次性校验。
- `DashboardService.java` — 接口：后台全局统计指标获取。
- `impl/DashboardServiceImpl.java` — 实现：全表内存聚合统计用户数、文件数、总容量与总配额。
- `EmailService.java` — 邮件发送：按配置中心/yml 回落链动态构建 JavaMailSender，发送 HTML 验证码邮件。
- `JwtBlacklistService.java` — JWT 登出黑名单：将登出 Token 写入 Redis 黑名单立即失效（TTL 自动过期）。
- `LoginAttemptService.java` — 登录失败锁定：Redis 计数失败次数，达阈值临时锁定账号。
- `MetaService.java` — 接口：管理后台枚举选项组装。
- `impl/MetaServiceImpl.java` — 实现：角色/状态/分享状态/操作类型枚举组装为 value+label（角色做显示混淆）。
- `OperationLogService.java` — 接口：日志写入与多维度/分页查询。
- `impl/OperationLogServiceImpl.java` — 实现：写入受开关控制，支持按用户/条件/管理端分页查询。

### 2.28 `service/team/`

- `TeamFileService.java` — 接口：团队文件列表/树/CRUD/回收站及管理端操作。
- `TeamService.java` — 接口：团队 CRUD、成员管理（邀请/移除/退出）、配额与角色权限校验。
- `impl/TeamFileServiceImpl.java` — 实现：团队目录树、重命名/移动/复制、删除进团队回收站、恢复/彻底删除、下载预览。
- `impl/TeamServiceImpl.java` — 实现：建队、成员管理、解散、配额原子调整、requireMember/Admin/Owner 权限链。

### 2.29 `service/user/`

- `UserService.java` — 接口：用户 CRUD、三来源配额模型、密码管理与管理端治理操作。
- `impl/UserServiceImpl.java` — 实现：注册、配额计算（基础+赠送+奖励）、管理端状态/配额/角色治理、密码重置。

### 2.30 `util/`

- `ShareTokenGenerator.java` — 分享短码生成器：去混淆字符集生成 10 位 token 并查重（最多重试 10 次）避免碰撞。

### 2.31 `utils/`

- `FileUtil.java` — 文件处理：扩展名提取、MIME 映射、允许扩展名白名单、文件分类、大小格式化、同名唯一化。
- `IdUtil.java` — UUID 与 MinIO 对象路径生成（用户/分片/文件/缩略图/打包产物路径）。
- `IpUtil.java` — 从请求头链（X-Forwarded-For 等）提取客户端真实 IP。
- `JwtTokenUtil.java` — JWT 工具接口：签发、解析、校验与有效期取值契约。
- `JwtTokenUtilImpl.java` — JWT 工具实现（jjwt）：Base64 密钥 HMAC-SHA 签名、强制校验签发者、有效期实时取配置。

### 2.32 `websocket/`

- `ProgressWebSocketHandler.java` — 统一进度推送通道 `/ws/progress`：维护会话集合并向所有连接广播上传/打包进度 JSON。

---

## 三、资源文件

### 3.1 配置

- `application.yml` — 公共配置：应用名、默认 profile=local、优雅停机、MyBatis（下划线转驼峰、关闭缓存）、日志配置、super-admin 默认账号。
- `application-local.yml` — 本地开发：默认连 localhost 的 MySQL/Redis/MinIO，含默认弱密钥与 SMTP 占位。
- `application-dev.yml` — Docker Compose 集成环境：用容器名访问、SMTP 通过 Brevo 环境变量注入、含 STARTTLS。
- `application-prod.yml` — 生产：全部配置强制从环境变量注入，auto-create-bucket 关闭，JWT 密钥必填。
- `application-test.yml` — 测试环境配置。

### 3.2 日志

- `logback-spring.xml` — 按 profile 分流：dev/local 输出控制台 + project/error/spring/mybatis/application 多文件；prod 仅文件；按大小与时间滚动、总量上限控制。

### 3.3 MyBatis XML（14 个）

- `DisabledObjectMapper.xml` / `FileHashMapper.xml` / `FileMapper.xml` / `FriendRequestMapper.xml` / `FriendshipMapper.xml` / `OperationLogMapper.xml` / `RecycleBinMapper.xml` / `SettingMapper.xml` / `ShareFileMapper.xml` / `ShareMapper.xml` / `TeamMapper.xml` / `TeamMemberMapper.xml` / `UserMapper.xml` — 分别实现 `mapper/` 中同名接口的全部 SQL。
- `FileDao.xml` — 实现 FileDao 的复杂分页搜索 SQL（LIKE + 类型过滤）。

---

## 四、测试

- `CloudBackendApplicationTests.java` — Spring Boot 启动上下文测试，验证容器加载无配置/依赖错误。