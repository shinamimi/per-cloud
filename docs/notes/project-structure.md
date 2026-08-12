# Cloud 云盘 — 项目结构全说明（学习笔记）

> 用途：逐一说明项目每个文件夹与文件的作用，方便快速定位代码、面试讲解、新人上手。
> 数据基于当前仓库真实结构与源码注释整理。

---

## 1. 目录总览

```
cloud/
├── .agents/              # AI 技能库（skills）与摘要，与本项目运行无关
├── backend/              # 后端 Spring Boot 工程（Java 21）
├── frontend/             # 前端 Vue 3 + TS 工程
├── sql/                  # 数据库建表 + 迁移脚本
├── docs/                 # 全部设计/规范文档 + 决策记录(ADR) + 学习笔记
├── docker/               # Docker Compose 环境变量
├── docker-compose.yml    # 基础设施编排（MySQL + Redis + MinIO）
├── docker-compose.prod.yml # 生产编排（MySQL/Redis/MinIO/Backend/Frontend，2C2G 适配）
├── start-dev.sh          # 一键启动脚本
├── .env                  # SMTP 等敏感环境变量（已 gitignore）
├── .gitignore
└── skills-lock.json      # AI 技能版本锁定文件
```

---

## 2. 根目录

| 文件/目录 | 作用 |
| --- | --- |
| `docker-compose.yml` | 编排开发环境：MySQL 8.4 + Redis 7.2 + MinIO（各带 healthcheck、Volume 持久化、共享 `cloud-network` 网络；`sql/` 映射进 MySQL 首次启动自动导入建表脚本） |
| `docker-compose.prod.yml` | 编排生产环境：MySQL + Redis + MinIO + Backend + Frontend(Nginx)；全服务内存限制、MySQL 只挂 `sql/init-full.sql`、MinIO 9000 公网暴露、`prod.env`/`prod-backend.env` 注入，适配 2C2G |
| `docker/.env` | Compose 用环境变量（MySQL/Redis/MinIO 账号密码、端口），仅本地开发用，可提交 |
| `docker/prod.env` | 生产基础设施变量（MySQL/MinIO 强密码、容器主机名），**已 gitignore**，上传服务器前替换 |
| `docker/prod-backend.env` | 生产后端变量（对应 `application-prod.yml` 全部 `${VAR}`，含 JWT 密钥、SMTP、MinIO 公网地址），**已 gitignore** |
| `.env` | 根目录敏感配置（SMTP 账号/密码/发件人、时区），**已被 gitignore**，仅供后端启动加载 |
| `start-dev.sh` | 一键启动：1) docker compose 起基础设施 → 2) `./mvnw spring-boot:run` 起后端并轮询等待就绪 → 3) `npm run dev` 起前端；Ctrl+C 全部停止 |
| `sql/` | 数据库脚本（见 §7） |
| `docs/` | 文档体系（见 §8） |
| `skills-lock.json` | opencode skills 的版本锁（hash 校验），非业务文件 |
| `.gitignore` | 忽略 target、node_modules、.env、logs、IDE 配置等 |

---

## 3. backend/ — 后端工程

### 3.1 构建与配置

| 文件/目录 | 作用 |
| --- | --- |
| `pom.xml` | Maven 构建：Spring Boot 4.0.7 parent、Java 21、依赖清单（Redis/Security/Validation/Web/WebSocket/AOP、SpringDoc、MyBatis、JJWT、MinIO、Thumbnailator、Mail、MySQL、Lombok、各 test starter）；Lombok 注解处理器配置 |
| `mvnw` / `mvnw.cmd` | Maven Wrapper（免安装 Maven 直接构建） |
| `.mvn/wrapper/` | Maven Wrapper 版本配置 |
| `HELP.md` | 官方生成的脚手架说明（可忽略） |
| `logs/` | 运行日志（application/error/project/spring/mybatis 五类，按日期归档） |
| `src/main/resources/application.yml` | 公共配置：应用名、profile 激活（local）、MyBatis 驼峰映射、logback 引用、超管初始账号 |
| `application-{local,dev,prod,test}.yml` | 四套环境配置：local(端口8081, 本地依赖)、dev(Docker Compose 服务名访问)、prod(生产环境变量)、test(测试) |
| `src/main/resources/logback-spring.xml` | 日志配置：按 profile 区分，日志分文件输出 |

### 3.2 后端包结构（`src/main/java/com/cloud/backend/`）

| 包 | 作用 | 关键类 |
| --- | --- | --- |
| `.` | 应用入口 | `CloudBackendApplication`（@SpringBootApplication + @ConfigurationPropertiesScan + @EnableScheduling） |
| `annotation/` | 自定义注解 | `Log`（操作日志标记，@Target(METHOD)+@Retention(RUNTIME)，属性支持 SpEL） |
| `aspect/` | AOP 切面 | `LogAspect`（@Around 拦截 @Log，方法成功后用反射+SpEL 组装 OperationLog 落库） |
| `authorization/` | 业务权限工具 | `AuthorizationPolicy`（从 SecurityContext 取当前用户、角色判断、管理操作前置校验） |
| `bo/` | 业务对象 | `AdminDashboardStatsBO`（管理端仪表盘统计：用户/文件/总量/配额/使用率） |
| `config/` | 配置类 | 见下方详解 |
| `constant/` | 常量 | `FileConstants`（单位/默认配额/分片大小/分类）、`RedisConstants`（Redis key 前缀设计） |
| `controller/` | Web 层（用户端） | `AuthController`、`FileController`、`ShareController`、`GuestShareController`、`TeamController`、`TeamFileController`、`FriendController`、`MetaController` |
| `controller/admin/` | Web 层（管理端） | 8 个 Admin* 控制器（账号/仪表盘/文件/日志/设置/分享/团队/用户） |
| `dao/` | 复杂查询数据访问 | `FileDao`（文件搜索分页 + 计数，对应 FileDao.xml） |
| `dto/` | 传输对象 | 通用（Result/Page/PageRequest/各请求响应）+ 分模块子包（admin/file/friend/meta/share/team） |
| `entity/` | 数据库实体 | 13 个实体（User/File/Share/FileHash/RecycleBin/Setting/OperationLog/Team/TeamMember/FriendRequest/Friendship/ShareFile/DisabledObject） |
| `enums/` | 枚举 | 14 个（ErrorCode/Role/UserStatus/FileStatus/FileType/ShareStatus/TeamStatus/TeamMemberRole/FriendRequestStatus/OperationType/TargetType/CaptchaType/DisableScope 等） |
| `event/` | 事件 | `UserRegisteredEvent`（注册事件）+ `UserEventListener`（监听并处理） |
| `exception/` | 异常体系 | `BusinessException`（业务异常）+ `GlobalExceptionHandler`（@RestControllerAdvice 统一兜底） |
| `mapper/` | MyBatis Mapper 接口 | 14 个（每个实体对应一个，注解或 XML 定义 SQL） |
| `security/` | 认证授权组件 | `JwtAuthenticationFilter`（OncePerRequestFilter 验签+黑名单+建上下文）、`LoginUser`（UserDetails 实现）、`UserDetailsServiceImpl`、`AuthenticationEntryPointImpl`、`AccessDeniedHandlerImpl` |
| `service/` | 业务层接口 | 按模块分包：user/system/file/share/team/friend/admin |
| `service/*/impl/` | 业务层实现 | 接口的实现类（如 `UploadServiceImpl`、`ShareServiceImpl`、`AuthServiceImpl`） |
| `util/` | 工具（特殊） | `ShareTokenGenerator`（10 位去混淆字符集短码生成，查重防冲突） |
| `utils/` | 通用工具 | `JwtTokenUtil`+`JwtTokenUtilImpl`（签发/校验）、`FileUtil`（扩展名/MIME/分类/大小格式）、`IdUtil`（UUID+对象路径）、`IpUtil`（代理头取 IP） |
| `websocket/` | WebSocket | `ProgressWebSocketHandler`（/ws/progress 统一进度通道，广播上传/打包进度） |

### 3.3 后端文件详解（逐个，含注解）

> 注解标注形式：**类注解** + 关键**方法注解**（括号内为该注解用途）。

#### 3.3.1 应用入口与事件

| 文件 | 注解 | 作用 |
| --- | --- | --- |
| `CloudBackendApplication` | `@SpringBootApplication`（组合配置/自动装配/组件扫描）、`@ConfigurationPropertiesScan`（自动注册 Properties）、`@EnableScheduling`（开启定时任务） | 后端启动入口，`main()` 调 `SpringApplication.run` |
| `event/UserRegisteredEvent` | —（普通类） | 注册成功事件载体（userId/username/email） |
| `event/UserEventListener` | `@Component`、`@EventListener`（监听事件） | 监听注册事件，处理注册后置逻辑 |

#### 3.3.2 注解与切面（自定义注解 + AOP）

| 文件 | 注解 | 作用 |
| --- | --- | --- |
| `annotation/Log` | `@Target(ElementType.METHOD)`、`@Retention(RUNTIME)`（自定义注解的元注解） | 操作日志标记注解，属性 operation/target/targetId/detail 支持 SpEL |
| `aspect/LogAspect` | `@Aspect`（声明切面）、`@Component`、`@Around("@annotation(...Log)")`（环绕通知） | 拦截所有 `@Log` 方法，成功后用反射+SpEL 组装 OperationLog 落库 |

#### 3.3.3 异常体系

| 文件 | 注解 | 作用 |
| --- | --- | --- |
| `exception/BusinessException` | —（`extends RuntimeException`） | 业务异常，携带 ErrorCode 枚举 |
| `exception/GlobalExceptionHandler` | `@RestControllerAdvice`（全局异常处理器）、`@ExceptionHandler`（6 个：BusinessException/参数校验/JSON 反序列化/上传超限/兜底500） | 统一异常→`Result<T>`，避免各层 try-catch |

#### 3.3.4 权限工具

| 文件 | 注解 | 作用 |
| --- | --- | --- |
| `authorization/AuthorizationPolicy` | —（静态工具类） | 从 SecurityContextHolder 取当前用户；角色判断（value 比较）；管理操作前置校验 canManageUser |

#### 3.3.5 config/ 配置类详解

| 配置类 | 注解 | 作用 |
| --- | --- | --- |
| `SecurityConfig` | `@Configuration`、`@Bean`（2 个：securityFilterChain、corsConfigurationSource） | 组装 Spring Security 过滤器链：关 CSRF、开 CORS、无状态会话、URL 权限矩阵（公开/ADMIN+/OPERATOR+/认证）、挂 JWT 过滤器 |
| `AuthenticationManagerConfig` | `@Configuration`、`@Bean` | 把 AuthenticationManager 暴露为可注入 Bean（登录认证用） |
| `PasswordEncoderConfig` | `@Configuration`、`@Bean` | 提供 BCryptPasswordEncoder（全项目统一密码编码） |
| `JwtProperties` | `@Data`（Lombok）、`@ConfigurationProperties(prefix="jwt")` | JWT 配置映射（secret/过期时间/header/prefix/issuer） |
| `FileProperties` | `@Data`、`@ConfigurationProperties(prefix="file")` | 文件配置：分片大小/单文件上限/并发数/回收站天数/预览大小等 |
| `MailProperties` | `@Data`、`@ConfigurationProperties(prefix="mail")` | SMTP 配置（host/port/username/password/from） |
| `MinioProperties` | `@Data`、`@ConfigurationProperties(prefix="minio")` | MinIO 配置（endpoint/密钥/桶/是否自动建桶） |
| `RedisProperties` | `@Data`、`@ConfigurationProperties(prefix="redis")` | Redis 配置（host/port/database/password/timeout） |
| `MinioConfig` | `@Configuration`、`@Bean`（minioClient）、`@EventListener(ApplicationReadyEvent)`（启动建桶）、`@ConditionalOnProperty`（自动建桶开关） | 创建 MinioClient；应用就绪后自动建桶 |
| `MyBatisTypeHandlerConfig` | `@Component`、`implements ConfigurationCustomizer`、`@Override customize` | 注册各枚举 TypeHandler（EnumOrdinalTypeHandler + 自定义） |
| `TeamMemberRoleTypeHandler` | —（`extends BaseTypeHandler<TeamMemberRole>`） | 自定义 TypeHandler：DB 存 value（0/10/20）非 ordinal |
| `OpenAPIConfig` | `@Configuration`、`@Bean` | SpringDoc 生成 Swagger 文档（/swagger-ui） |
| `SuperAdminInitializer` | `@Component`、`implements ApplicationRunner`、`@Override run` | 启动时确保配置的超管账号存在（幂等） |
| `FileCleanupTask` | `@Component`、`@Scheduled`（3 个 cron：03:00/03:30/04:00） | 定时清理：回收站过期记录、打包产物、操作日志、孤儿分片 |
| `WebSocketConfig` | `@Configuration`、`@EnableWebSocket`（开启 WebSocket）、`implements WebSocketConfigurer` | 注册 /ws/progress Handler，允许跨源 |

#### 3.3.6 controller/ 用户端控制器（含注解）

| 控制器 | 类注解 + 路由 | 关键接口（方法注解） | 作用 |
| --- | --- | --- | --- |
| `AuthController` | `@RestController` + `@RequestMapping("/api/auth")` | `@PostMapping` login/send-code/register/forgot-password/reset-password/logout；`@Valid @RequestBody` 入参校验 | 登录/注册/验证码/找回密码/登出 |
| `FileController` | `@RestController` + `@RequestMapping("/api/files")` | `@GetMapping` list/tree/search/preview/recycle-bin/audio/play；`@PostMapping` directory/upload/init/upload/chunk/upload/merge/upload/sec/download/batch/recycle-bin恢复；`@PutMapping` rename；`@DeleteMapping` 删除/回收站彻底删；`@PathVariable`/`@RequestParam`/`@Valid`；部分 `@Log` | 文件列表/目录树/分片上传/下载/重命名/移动/复制/删除/回收站 |
| `ShareController` | `@RestController` + `@RequestMapping("/api/shares")` | `@PostMapping` 创建；`@GetMapping` 列表；`@PutMapping` 更新有效期；`@DeleteMapping` 取消/删除记录 | 我的分享管理 |
| `GuestShareController` | `@RestController` + `@RequestMapping("/api/shares/access")` | `@GetMapping` info/files/preview/download/batch-task；`@PostMapping` verify/batch-download/save；`@PathVariable` | 访客访问分享（提取码/文件树/下载/转存） |
| `TeamController` | `@RestController` + `@RequestMapping("/api/teams")` | `@PostMapping` 创建；`@GetMapping` 列表/详情/成员；`@PutMapping` 更新；`@DeleteMapping` 解散/移除成员；`@PostMapping` 邀请/离开 | 团队管理（CRUD/成员） |
| `TeamFileController` | `@RestController` + `@RequestMapping("/api/teams/{teamId}/files")` | `@GetMapping` list/tree；`@PostMapping` directory；`@PutMapping` rename/move；`@PostMapping` copy/delete；`@DeleteMapping` 删除/回收站 | 团队空间文件管理（复用文件逻辑） |
| `FriendController` | `@RestController` + `@RequestMapping("/api/friends")` | `@GetMapping` 列表/搜索/请求列表；`@PostMapping` 发请求；`@PutMapping` 接受/拒绝；`@DeleteMapping` 删除好友 | 好友系统 |
| `MetaController` | `@RestController` + `@RequestMapping("/api/meta")` | `@GetMapping` options | 元数据/选项字典（枚举下拉等） |

#### 3.3.7 controller/admin/ 管理端控制器

| 控制器 | 类注解 + 路由 | 作用 |
| --- | --- | --- |
| `AdminController` | `@RestController` + `@RequestMapping("/api/admin")`（占位/根） | 管理端基础入口 |
| `AdminDashboardController` | `@RestController` + `/api/admin/dashboard`；`@GetMapping` stats | 仪表盘统计（用户/文件/总量/配额） |
| `AdminUserController` | `@RestController` + `/api/admin/users` | 用户管理：状态/配额/解锁/重置密码 |
| `AdminFileController` | `@RestController` + `/api/admin/files` | 全局文件管控：详情/下载/预览/禁用/批量删除/全局回收站 |
| `AdminShareController` | `@RestController` + `/api/admin/shares` | 全局分享管控：取消/允许下载/删除记录 |
| `AdminTeamController` | `@RestController` + `/api/admin/teams` | 全局团队管控：配额/文件/回收站/解散 |
| `AdminLogController` | `@RestController` + `/api/admin/logs` | 操作日志查询（过滤/分页） |
| `AdminAccountController` | `@RestController` + `/api/admin/admins` | 管理员账号管理：创建/删除/改角色/批量改角色（`@AuthenticationPrincipal LoginUser`） |
| `AdminSettingsController` | `@RestController` + `/api/admin/settings` | 系统配置中心：upload/storage/session/cache/system/file/mail/log/team 分组更新 |

#### 3.3.8 dao/ + mapper/（数据访问层）

| 文件 | 注解 | 作用 |
| --- | --- | --- |
| `dao/FileDao` | —（对应 FileDao.xml） | 文件复杂搜索分页 + 计数 |
| `mapper/UserMapper` | `@Mapper`、`@Param`（多参数绑定） | t_user CRUD、账号/邮箱查询、used_space 原子增减、配额筛选 |
| `mapper/FileMapper` | `@Mapper`、`@Param` | t_file 增删改查、分页/树、团队文件、批量禁用、哈希查询 |
| `mapper/ShareMapper` | `@Mapper`、`@Param` | 分享 CRUD、按 token 查、原子下载计数（防超限）、活跃分享数 |
| `mapper/ShareFileMapper` | `@Mapper`、`@Param` | 分享文件快照（批量插入/按 shareId 查/删除） |
| `mapper/FileHashMapper` | `@Mapper` | 秒传索引（hash 查/插/删） |
| `mapper/RecycleBinMapper` | `@Mapper` | 回收站记录（按用户/团队/过期时间查） |
| `mapper/OperationLogMapper` | `@Mapper`、`@Param` | 操作日志（分页 join 用户名、按时间清理） |
| `mapper/TeamMapper` | `@Mapper` | 团队 CRUD、配额/已用空间原子更新 |
| `mapper/TeamMemberMapper` | `@Mapper`、`@Param` | 团队成员（按团队/用户查、角色/状态更新） |
| `mapper/FriendRequestMapper` | `@Mapper`、`@Param` | 好友请求（最新/待处理/状态更新） |
| `mapper/FriendshipMapper` | `@Mapper` | 好友关系（按用户/双边查、删除） |
| `mapper/SettingMapper` | `@Mapper` | 系统配置项（key 查/插/删/全查） |
| `mapper/DisabledObjectMapper` | `@Mapper`、`@Param` | 禁用对象（hash+scope+user，countBlocked 校验） |
| `resources/mapper/*.xml`（14 个） | —（XML 定义 SQL，与 Mapper 接口一一对应） | 具体 SQL 实现（含动态 SQL/联表） |

#### 3.3.9 entity/ 实体类（数据库映射）

> 注解：全部使用 `@Data`（Lombok，自动生成 getter/setter/toString）。

| 实体 | 对应表 | 关键字段 |
| --- | --- | --- |
| `User` | t_user | id/username/password/email/role/quota/usedSpace/status/isVip |
| `File` | t_file | userId/teamId/parentId/name/path/size/fileHash/isDirectory/category/objectName/status |
| `Share` | t_share | userId/fileId/shareToken/accessPassword/status/expireTime/maxDownload/downloadCount/allowDownload/allowSave |
| `ShareFile` | t_share_file | shareId/fileId/parentId/name（分享文件快照） |
| `FileHash` | t_file_hash | fileHash/objectName/size/refCount（秒传共享索引） |
| `RecycleBin` | t_recycle_bin | userId/fileId/objectName/fileHash/parentId/size/deletedTime/expireTime |
| `OperationLog` | t_operation_log | userId/operation/targetType/targetId/detail/ip/userAgent |
| `Team` | t_team | name/ownerId/quota/usedSpace/status |
| `TeamMember` | t_team_member | teamId/userId/role/status |
| `FriendRequest` | t_friend_request | fromUserId/toUserId/status |
| `Friendship` | t_friendship | userAId/userBId |
| `Setting` | t_setting | settingKey/settingValue/description（配置中心） |
| `DisabledObject` | t_disabled_object | fileHash/scope/userId/reason（禁用对象） |

#### 3.3.10 dto/ 传输对象（请求/响应）

> 注解：请求类 `@Data` + Bean Validation（`@NotBlank`/`@NotNull`/`@Size`/`@Email` 等，配合 `@Valid` 在 Controller 校验）；响应类 `@Data` 只做序列化。

| 子包/文件 | 作用 |
| --- | --- |
| `Result<T>` | 全局统一响应 `{code,message,data}`（静态工厂 success/fail） |
| `Page<T>` / `PageRequest` | 通用分页结果 / 分页入参 |
| `LoginRequest/RegisterRequest/ResetPasswordRequest/SendCodeRequest` | 认证请求体（含校验注解） |
| `LoginResponse` | 登录响应（token/userId/username/role） |
| `FileQuery` / `AdminFileQuery` | 文件搜索/管理端筛选条件 |
| `dto/file/`（18 个） | 上传（UploadInit/UploadSec/UploadMerge/UploadProgress/UploadPolicy）、下载（BatchDownload）、目录/重命名/移动/复制请求、FileNode/FileTree/FilePreview/RecycleBin/AudioPlay 响应 |
| `dto/share/`（8 个） | 分享创建/更新/校验请求、分享/访客/文件快照响应 |
| `dto/team/`（5 个） | 团队创建/更新/邀请请求、团队/成员响应 |
| `dto/friend/`（4 个） | 好友请求创建、搜索/请求/用户响应 |
| `dto/meta/`（2 个） | 元数据选项响应（MetaOptionsResponse/OptionItem） |
| `dto/admin/`（24 个） | 管理端全部请求/响应（配额/角色/状态/设置分组/日志筛选等） |

#### 3.3.11 service/ 业务层接口 + impl 实现

> 通用注解：接口无注解；实现类 `@Service` + 方法 `@Override`；事务方法 `@Transactional`；审计方法 `@Log`。

| 接口（包） | 实现类 | 核心能力（方法摘选） |
| --- | --- | --- |
| `UserService` | `UserServiceImpl` | 注册、账号/邮箱查询、密码 BCrypt 更新、管理端：创建管理员/改状态/配额/解锁/删管理员/重置密码 |
| `AuthService` | `AuthServiceImpl` | 登录（AuthenticationManager 认证 + 失败锁定 + 签发 JWT + 审计）、注册自动登录、发验证码、找回/重置密码 |
| `CaptchaService` | （无 impl，直接 `@Service` 类） | 验证码生成/校验/冷却（Redis String + TTL + 一次性） |
| `EmailService` | （无 impl，直接 `@Service` 类） | 发验证码/HTML 邮件（动态 SMTP 配置） |
| `LoginAttemptService` | （无 impl，直接 `@Service` 类） | 登录失败计数 + 阈值锁定（Redis INCR + TTL） |
| `JwtBlacklistService` | （无 impl，直接 `@Service` 类） | Token 黑名单（登出失效，TTL=剩余有效期） |
| `OperationLogService` | `OperationLogServiceImpl` | 审计日志落库/查询/过滤/分页（手动埋点 + @Log 切面共用） |
| `MetaService` | `MetaServiceImpl` | 元数据选项（枚举字典，供前端下拉） |
| `DashboardService` | `DashboardServiceImpl` | 管理端统计（用户/文件/总量/配额） |
| `FileService` | `FileServiceImpl` | 文件领域：分页/树/目录/重命名/移动/复制/删除到回收站/音频列表/唯一名解析 |
| `UploadService` | `UploadServiceImpl` | 分片上传全流程：init（自适应分片+Redis 元数据）/chunk（幂等+进度）/merge（合并+SHA256 校验+秒传注册+配额扣减）/sec（秒传）/policy |
| `DownloadService` | `DownloadServiceImpl` | 单文件预签名直链、批量打包（线程池+WS 进度）、打包产物清理、分享/访客打包 |
| `StorageService` | `StorageServiceImpl` | 对象存储抽象（upload/download/delete/预签名 URL/copy/exists/list/bucket）；record ObjectInfo |
| `PreviewService` | `PreviewServiceImpl` | 文件预览（文本/图片/音视频分类返回地址，含管理员通道） |
| `SearchService` | `SearchServiceImpl` | 文件名+分类搜索分页 |
| `FileHashService` | `FileHashServiceImpl` | 秒传索引注册/引用计数增减 |
| `RecycleBinService` | `RecycleBinServiceImpl` | 回收站：恢复/彻底删除/过期清理/团队回收站 |
| `ShareService` | `ShareServiceImpl` | 分享创建/列表/有效期/取消/删除、密码校验、文件快照、下载 URL、管理端取消/改下载/删除 |
| `TeamService` | `TeamServiceImpl` | 团队 CRUD/成员管理/配额校验/成员角色校验（requireMember/Admin/Owner） |
| `TeamFileService` | `TeamFileServiceImpl` | 团队文件：列表/树/目录/重命名/移动/复制/删除/下载/预览/回收站 + 管理端 |
| `FriendService` | `FriendServiceImpl` | 好友列表/搜索/请求收发/接受拒绝/删除/isFriendOrTeamMate |
| `AdminSettingsService` | `AdminSettingsServiceImpl` | 配置中心：读写全部配置分组（上传/存储/会话/缓存/系统/文件/邮件/日志/团队），供全项目取值 |
| `AdminFileService` | `AdminFileServiceImpl` | 管理端文件管控：详情/下载/禁用/全局回收站/恢复/删除 |

#### 3.3.12 security/ 认证授权组件

| 文件 | 注解 | 作用 |
| --- | --- | --- |
| `JwtAuthenticationFilter` | `@Component`、`extends OncePerRequestFilter`、`@Override doFilterInternal` | 每个请求验签→查黑名单→查用户→建 SecurityContext；失败直接返回 JSON |
| `LoginUser` | `@Getter`（Lombok）、`implements UserDetails` | 当前登录用户封装（userId/username/role/status），getAuthorities 映射 ROLE_ 前缀 |
| `UserDetailsServiceImpl` | `@Service`、`implements UserDetailsService`、`@Override loadUserByUsername` | 登录时按账号查用户构建 LoginUser |
| `AuthenticationEntryPointImpl` | `@Component`、`implements AuthenticationEntryPoint`、`@Override commence` | 未认证（401）统一 JSON 响应 |
| `AccessDeniedHandlerImpl` | `@Component`、`implements AccessDeniedHandler`、`@Override handle` | 无权限（403）统一 JSON 响应 |

#### 3.3.13 websocket/ + 工具类

| 文件 | 注解 | 作用 |
| --- | --- | --- |
| `websocket/ProgressWebSocketHandler` | `@Component`、`extends TextWebSocketHandler`、`@Override`（连接建立/关闭/错误/文本） | /ws/progress 统一进度通道，CopyOnWriteArraySet 管理会话，broadcast 广播消息 |
| `util/ShareTokenGenerator` | —（final 工具类 + 静态方法） | 10 位去混淆字符集短码（排除 O/0/I/l/1），查重最多重试 10 次 |
| `utils/JwtTokenUtil` + `JwtTokenUtilImpl` | `@Service`（Impl）+ 接口 | JWT 签发/校验/解析（HMAC 签名，issuer 校验），过期时间动态取自配置 |
| `utils/FileUtil` | —（静态工具） | 扩展名提取、MIME 映射、类型分类、大小格式化、白名单 |
| `utils/IdUtil` | —（静态工具） | UUID + MinIO 对象路径生成（user/{id}/{uuid}/{name} 等） |
| `utils/IpUtil` | —（静态工具） | 从 X-Forwarded-For/X-Real-IP 等代理头取客户端 IP |

#### 3.3.14 enums/ 枚举 + bo/ + constant/

| 文件 | 作用 |
| --- | --- |
| `enums/ErrorCode` | 全项目错误码（code+message，200/10000+） |
| `enums/Role` | 用户角色（USER=0/OPERATOR=10/ADMIN=20/SUPER_ADMIN=100） |
| `enums/UserStatus` | 用户状态（DISABLED/NORMAL/LOCKED/INACTIVE） |
| `enums/FileStatus` | 文件状态（DELETED/NORMAL/DISABLED） |
| `enums/FileType` | 文件/目录类型 |
| `enums/ShareStatus` | 分享状态（NORMAL/EXPIRED/CANCELED/EXHAUSTED） |
| `enums/TeamStatus` | 团队状态（NORMAL/DISSOLVED） |
| `enums/TeamMemberRole` | 团队角色（MEMBER/ADMIN/OWNER，value 0/10/20） |
| `enums/FriendRequestStatus` | 好友请求状态（PENDING/ACCEPTED/REJECTED） |
| `enums/OperationType` | 审计操作类型（LOGIN/UPLOAD_FILE/DELETE_FILE 等） |
| `enums/TargetType` | 审计目标类型（USER/FILE/SHARE/TEAM） |
| `enums/CaptchaType` | 验证码场景（REGISTER/LOGIN/RESET_PASSWORD） |
| `enums/DisableScope` | 禁用粒度（GLOBAL/USER，对应 ADR-012 B+C） |
| `bo/AdminDashboardStatsBO` | 仪表盘统计聚合对象（record 风格 DTO） |
| `constant/FileConstants` | 文件常量：单位换算/默认配额/分片大小/分类编号/根目录 id |
| `constant/RedisConstants` | Redis key 前缀设计：upload:meta/upload:chunks/upload:uploading/lock:merge/share:pwd-fail 等 |

### 3.4 注解总体使用与功能（全量汇总）

> 按用途分类，统计数字来自 `grep` 全量扫描（含注释中的引用，实际生效数量以此为参考）。

#### 3.4.1 Spring 注册 / 装配

| 注解 | 项目使用量 | 功能 | 本项目落点 |
| --- | --- | --- | --- |
| `@Component` | 11 | 注册为 Spring Bean（通用组件） | security 过滤器、事件监听、WebSocket Handler、定时任务、配置自定义等 |
| `@Service` | 24 | 注册业务层 Bean | 所有 ServiceImpl |
| `@RestController` | 16 | 组合注解 = `@Controller` + `@ResponseBody`，注册 Web Bean 并直接返回 JSON | 全部 17 个控制器 |
| `@Configuration` | 7 | 声明配置类（含 @Bean 方法） | SecurityConfig/MinioConfig/OpenAPIConfig 等 |
| `@Bean` | 6 | 在配置类中显式声明 Bean | SecurityFilterChain、CorsConfigurationSource、MinioClient、AuthenticationManager、PasswordEncoder、OpenAPI |
| `@ConfigurationProperties` | 7 | 绑定 yml 前缀配置为属性类（配合 @ConfigurationPropertiesScan） | JwtProperties/FileProperties/MailProperties/MinioProperties/RedisProperties |
| `@ConfigurationPropertiesScan` | 2 | 扫描并注册所有 @ConfigurationProperties 类 | CloudBackendApplication 类级 |
| `@EnableConfigurationProperties` | 1 | 显式启用配置属性绑定（备选方案） | — |
| `@Value` | 9 | 注入单个配置值 | SuperAdminInitializer 等 |
| `@Autowired`/`@Resource` | — | 字段/构造器注入 | 项目统一用构造器注入，未用字段注入 |

#### 3.4.2 组件扫描与启动（自动配置）

| 注解 | 使用量 | 功能 | 落点 |
| --- | --- | --- | --- |
| `@SpringBootApplication` | 2 | 组合：`@Configuration` + `@EnableAutoConfiguration` + `@ComponentScan` | CloudBackendApplication |
| `@EnableAutoConfiguration` | 1 | 开启自动配置（starter 依赖生效） | 被 @SpringBootApplication 组合 |
| `@ComponentScan` | 1 | 包扫描注册组件 | 被 @SpringBootApplication 组合 |
| `@EnableScheduling` | 2 | 开启 @Scheduled 定时任务 | CloudBackendApplication |
| `@EnableWebSocket` | 1 | 开启 WebSocket 支持 | WebSocketConfig |
| `@ConditionalOnProperty` | 2 | 按配置项条件注册 Bean | MinioConfig 自动建桶开关 |
| `@EventListener` | 2 | 监听 Spring 应用事件 | ApplicationReadyEvent（建桶）、UserRegisteredEvent |
| `@PostConstruct` | 1 | 初始化回调（Bean 创建后执行） | 注释中提到（MinioConfig 弃用说明） |

#### 3.4.3 Web / MVC

| 注解 | 使用量 | 功能 | 落点 |
| --- | --- | --- | --- |
| `@RequestMapping` | 16 | 类级路由前缀 | 全部控制器类级 |
| `@GetMapping` | 46 | GET 路由 | 查询接口 |
| `@PostMapping` | 31 | POST 路由 | 写操作/上传 |
| `@PutMapping` | 25 | PUT 路由 | 更新操作 |
| `@DeleteMapping` | 17 | DELETE 路由 | 删除操作 |
| `@PathVariable` | 79 | 路径参数绑定（如 /{id}） | 大量接口 |
| `@RequestParam` | 22 | 查询参数绑定（分页/筛选/上传分片） | FileController/TeamFileController 等 |
| `@RequestBody` | 48 | 请求体反序列化为 DTO | 全部 POST/PUT 写接口 |
| `@RequestHeader` | 1 | 请求头参数绑定 | AuthController logout 取 Authorization |
| `@ResponseBody` | 1 | 返回值序列化 JSON | 被 @RestController 组合 |
| `@AuthenticationPrincipal` | 3 | 注入当前登录用户（LoginUser） | AdminAccountController/AdminTeamController |

#### 3.4.4 参数校验（Bean Validation）

| 注解 | 使用量 | 功能 | 落点 |
| --- | --- | --- | --- |
| `@Valid` | 26 | 触发 DTO 校验（配合 @RequestBody） | 全部带校验的写接口 |
| `@NotBlank` | 18 | 字符串非空校验 | DTO 请求字段 |
| `@NotNull` | 15 | 非空校验 | DTO 请求字段 |
| `@NotEmpty` | 2 | 集合/字符串非空 | DTO 请求字段 |
| `@Size` | 8 | 长度范围校验 | 用户名/密码等 |
| `@Email` | 3 | 邮箱格式校验 | 注册/找回密码 |
| `@Pattern` | 3 | 正则校验 | 密码/格式规则 |
| `@Positive` | 2 | 正数校验 | 分页/数量 |
| 校验失败处理 | — | `@ExceptionHandler(MethodArgumentNotValidException)` 统一响应 | GlobalExceptionHandler |

#### 3.4.5 AOP / 自定义注解

| 注解 | 使用量 | 功能 | 落点 |
| --- | --- | --- | --- |
| `@Aspect` | 1 | 声明切面类 | LogAspect |
| `@Around` | 1 | 环绕通知（`@annotation(...)` 切点） | LogAspect 拦截 @Log |
| `@interface` | 1 | 声明自定义注解 | annotation/Log |
| `@Target` | 2 | 自定义注解可用位置（METHOD） | annotation/Log |
| `@Retention` | 2 | 自定义注解生命周期（RUNTIME） | annotation/Log |
| `@Log` | 19 | 标记审计方法，切面自动记录操作日志（SpEL 支持 #result/#参数） | File/Share/TeamFile/User 模块关键写操作 |

#### 3.4.6 事务

| 注解 | 使用量 | 功能 | 落点 |
| --- | --- | --- | --- |
| `@Transactional` | 23 | 声明式事务（异常自动回滚） | FileServiceImpl/ShareServiceImpl/TeamServiceImpl/TeamFileServiceImpl/AdminFileServiceImpl/FriendServiceImpl 的多步写操作 |

#### 3.4.7 异常处理

| 注解 | 使用量 | 功能 | 落点 |
| --- | --- | --- | --- |
| `@RestControllerAdvice` | 2 | 全局异常处理器（返回 JSON） | GlobalExceptionHandler |
| `@ControllerAdvice` | 1 | 全局异常/模型处理（@ResponseBody 需另加） | 注释对比说明 |
| `@ExceptionHandler` | 6 | 绑定异常类型处理 | BusinessException/参数校验/反序列化/超限/兜底 |

#### 3.4.8 定时任务

| 注解 | 使用量 | 功能 | 落点 |
| --- | --- | --- | --- |
| `@Scheduled` | 3 | 定时执行（cron 表达式） | FileCleanupTask（03:00/03:30/04:00 三条清理线） |

#### 3.4.9 数据层（MyBatis）

| 注解 | 使用量 | 功能 | 落点 |
| --- | --- | --- | --- |
| `@Mapper` | 14 | 标记 MyBatis Mapper 接口（被扫描生成代理） | 全部 14 个 Mapper 接口 |
| `@Param` | 100 | 多参数命名绑定（SQL 中引用参数名） | Mapper 接口方法参数 |

#### 3.4.10 Lombok（编译期代码生成）

| 注解 | 使用量 | 功能 | 落点 |
| --- | --- | --- | --- |
| `@Data` | 85 | 组合 = @Getter + @Setter + @ToString + @EqualsAndHashCode + @RequiredArgsConstructor | 全部 entity、DTO 响应类 |
| `@Getter` | 15 | 只生成 getter | LoginUser 等 |
| `@AllArgsConstructor` | 2 | 全参构造器 | DTO 等 |
| 其他 Lombok | — | @Builder/@NoArgsConstructor 等未使用 | — |

#### 3.4.11 其他/内置

| 注解 | 使用量 | 功能 | 落点 |
| --- | --- | --- | --- |
| `@Override` | 243 | 覆写父类/接口方法（编译器校验） | 全部实现类 |
| `@Deprecated` | 1 | 标记废弃 | — |
| `@SuppressWarnings` | 1 | 抑制编译器警告 | LogAspect 泛型强转 |

#### 3.4.12 注解在请求链路中的协作（全景示例）

```
HTTP 请求
  ↓ @Valid @RequestBody → 校验 DTO（@NotBlank/@Size/@Email...）
  ↓ @RequestMapping + @GetMapping/@PostMapping → 路由到 Controller 方法
  ↓ @PathVariable/@RequestParam/@AuthenticationPrincipal → 参数绑定
  ↓ (自定义 @Log + @Around) → 业务成功后 AOP 记录审计日志
  ↓ @Service 方法内 @Transactional → 多步写操作事务包裹
  ↓ 抛异常 → @RestControllerAdvice + @ExceptionHandler → 统一 Result 返回
```

---

## 4. frontend/ — 前端工程（Vue 3 + TS + Element Plus）

### 4.1 工程配置

| 文件/目录 | 作用 |
| --- | --- |
| `package.json` | 依赖：vue3、vue-router4、pinia、axios、element-plus、typescript、vite、vue-tsc；scripts: dev/build/preview |
| `vite.config.ts` | Vite 配置：端口 5173、`@`→`src` 别名、/api 与 /ws 代理到 8081 |
| `tsconfig.json` / `tsconfig.tsbuildinfo` | TypeScript 配置 / 构建缓存 |
| `index.html` | 单页入口（挂载 #app，加载 main.ts） |
| `.env.development` | 开发环境变量（API 基础地址、WS 地址、标题） |
| `env.d.ts` | .vue 模块类型声明 + import.meta.env 类型扩展 |

### 4.2 src/ 源码

| 目录 | 作用 | 关键文件 |
| --- | --- | --- |
| `main.ts` | 应用入口：挂载 Pinia、Router、Element Plus（中文包）、全局图标 | — |
| `App.vue` | 根组件（按 meta.layout 选布局） | — |
| `api/` | 后端接口封装（axios 调用，按模块分文件） | auth/file/share/team/friend/meta + admin/ 子目录 |
| `api/admin/` | 管理端接口封装 | admin/user/file/share/team/settings/dashboard |
| `components/` | 复用组件 | common/（Transfer、UserPreviewTable）、file/（目录树、文件列表、上传、传输队列、预览、移动复制、面包屑）、share/（创建分享）、team/（加成员、团队移动复制） |
| `layout/` | 布局组件 | `AuthLayout.vue`（认证页居中卡片）、`MainLayout.vue`（侧边栏+顶栏+内容区） |
| `permissions/` | 权限控制 | `role.ts`（角色等级）、`admin-operations.ts`、`file-operations.ts`（操作权限判定） |
| `router/` | 路由 | `index.ts`：全页面路由 + 导航守卫（未登录跳 /login，已登录跳 /files）；Hash 模式 |
| `stores/` | Pinia 状态 | user（登录态/localStorage 持久化）、file、upload（上传队列与进度）、meta（系统元数据） |
| `types/` | TS 类型定义 | api/auth/user/file/share/friend/team/admin/meta 各模块类型 |
| `utils/` | 工具 | `request.ts`（axios 封装：自动解包 Result、注入 Token、401 跳登录、统一错误提示）、`ws.ts`（WebSocket 单例管理）、`upload.ts`、`download.ts`、`format.ts`、`permission.ts` |
| `views/` | 页面 | 见下方详解 |

### 4.3 views/ 页面详解

| 页面 | 路由 | 作用 |
| --- | --- | --- |
| `login/LoginView.vue` | /login | 登录 |
| `register/RegisterView.vue` | /register | 注册（含邮箱验证码） |
| `forgot/ForgotPasswordView.vue` | /forgot-password | 找回密码 |
| `welcome/WelcomeView.vue` | / | 欢迎/落地页 |
| `files/FileView.vue` | /files | 文件管理主页面（列表、目录树、上传、预览、分享、移动复制、回收站入口） |
| `recycle/RecycleBinView.vue` | /recycle | 回收站（恢复/彻底删除） |
| `share/ShareManageView.vue` | /shares | 我的分享管理 |
| `share/GuestShareView.vue` | /s/:token | 访客分享访问（提取码、下载/转存） |
| `friends/FriendsView.vue` | /friends | 好友列表与好友请求 |
| `teams/TeamsView.vue` | /teams | 团队列表/创建/成员管理 |
| `teams/TeamFilesView.vue` | /teams/:id/files | 团队空间文件 |
| `teams/TeamRecycleView.vue` | /teams/:id/recycle | 团队回收站 |
| `admin/AdminDashboardView.vue` | /admin | 管理端仪表盘（统计） |
| `admin/AdminUserView.vue` | /admin/users | 用户管理（角色/配额/状态） |
| `admin/AdminFileView.vue` | /admin/files | 全局文件管控 |
| `admin/AdminShareView.vue` | /admin/shares | 全局分享管控 |
| `admin/AdminTeamView.vue` | /admin/teams | 全局团队管控 |
| `admin/AdminAdminView.vue` | /admin/admins | 管理员账号管理 |
| `admin/SystemConfigCenterView.vue` | /admin/settings | 系统配置中心（20+ 项动态配置） |

---

## 5. backend/src/test

| 文件 | 作用 |
| --- | --- |
| `CloudBackendApplicationTests.java` | 空启动测试（验证上下文加载），当前测试覆盖待补（见 docs/TEST.md） |

---

## 6. 数据库脚本（sql/）

| 文件 | 作用 |
| --- | --- |
| `schema.sql` | 基础建表：t_user、t_file、t_share、t_recycle_bin、t_operation_log（Compose 首次启动自动导入） |
| `migration-file-module.sql` | 文件模块迁移（分片/秒传/存储相关表结构演进） |
| `migration-share-module.sql` | 分享模块迁移 |
| `migration-team-friend.sql` | 团队 + 好友模块迁移（t_team/t_team_member/t_friend_request/t_friendship） |
| `migration-admin-file-control.sql` | 管理端文件管控迁移（t_disabled_object 等） |

---

## 7. docs/ — 文档体系

### 7.1 核心文档
| 文档 | 作用 |
| --- | --- |
| `PRD.md` | 产品需求文档（MVP 功能范围、用户画像、核心目标） |
| `HLD.md` | 概要设计（模块划分 M1-M10、依赖矩阵、协作场景、安全设计、部署架构、开放问题） |
| `DDD.md` | 领域驱动设计（领域模型/聚合/术语） |
| `API.md` | API 接口文档 |
| `DATABASE.md` | 数据库设计（表结构、字段、索引） |
| `TEST.md` | 测试计划/策略 |
| `CODING_STANDARDS.md` | 编码规范（含 §1.6/§2.7 注释规范：中文、全覆盖、不引文档编号） |

### 7.2 模块设计
| 文档 | 作用 |
| --- | --- |
| `file-module.md` | 文件模块（上传/下载/秒传/回收站/预览） |
| `share-module.md` | 分享模块（含 §5.1 下载次数=全局共享 N 次） |
| `team-module.md` | 团队模块 |
| `friend-system.md` | 好友系统 |
| `system-config-center.md` | 系统配置中心（两级权限 OPERATOR/ADMIN） |
| `admin-file-management.md` | 管理端文件管控（禁用粒度 B+C、秒传拦截） |
| `admin-role-hierarchy.md` | 管理端角色层级 |
| `admin-user-management.md` | 管理端用户管理 |
| `frontend-standard.md` | 前端规范 |
| `component-transfer.md` | 组件迁移说明（前端结构演进） |
| `ai-code-quality.md` | AI 生成代码质量分析 |

### 7.3 ADR（架构决策记录）
`docs/adr/001-013`：MinIO 桶、预签名 URL、秒传、回收站、角色层级、枚举命名、配额模型、前端边界、设置权限、好友、团队文件、管理文件管控、分享 token。

### 7.4 其他
| 目录/文件 | 作用 |
| --- | --- |
| `bugs/` | 问题修复记录（BUG_FIXES_1~5） |
| `notes/` | 学习笔记（谷粒商城架构、谷粒×云盘对比、实操学习指南、本文件） |

---

## 8. 各技术栈在结构中的对应（速查）

| 技术 | 位置 |
| --- | --- |
| Java 21 | backend 全部源码（switch 表达式/instanceof 模式匹配/record） |
| Spring Boot | config/ + CloudBackendApplication + resources/application*.yml |
| Spring Security | config/SecurityConfig + security/ |
| MyBatis | mapper/ + resources/mapper/*.xml + config/MyBatisTypeHandlerConfig |
| MySQL | sql/ + docker-compose.yml + sql/init-full.sql（生产全量建表） |
| Redis | constant/RedisConstants + 各 Service 的 StringRedisTemplate 使用 |
| MinIO | config/MinioConfig + service/file/StorageService(Impl) + utils/IdUtil 对象路径 |
| Vue 3 | frontend/src（main.ts、views/、components/） |
| TypeScript | frontend 全部 .ts + .vue `<script setup lang="ts">` |
| Element Plus | main.ts 全局挂载 + 各页面 `<el-*>` 组件 |
| WebSocket | config/WebSocketConfig + websocket/ProgressWebSocketHandler + frontend/src/utils/ws.ts |
| Docker Compose | docker-compose.yml（dev）+ docker-compose.prod.yml（生产，适配 2C2G） |
| 部署 | backend/Dockerfile + frontend/Dockerfile + frontend/nginx.conf + docker/prod.env + docker/prod-backend.env |
| 部署文档 | docs/DEPLOYMENT.md |

---

## 关联
- 实操指南（如何用这些文件学技术）：`docs/notes/cloud-learning-guide.md`
- 模块边界：`docs/HLD.md`
