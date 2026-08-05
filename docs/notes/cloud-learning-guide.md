# 通过 Cloud 项目学技术 — 实操学习指南（学习笔记）

> 前置：后端 `./mvnw spring-boot:run` 启动，端口 **8081**，Redis 6379，Swagger: http://localhost:8081/swagger-ui/index.html
> 登录接口返回 `data.token`，后续请求带 `Authorization: Bearer <token>`。
> 约定：`文件:方法():行号` 表示断点位置；"看 X 变化"指用 redis-cli 或数据库查询验证。

---

## 1. Java 基础与进阶

### 1.1 泛型 —— `Result<T>` / `Page<T>`
- **读** `dto/Result.java`（3 个泛型静态工厂 `success/fail`）；`dto/Page.java`（`Page<T>` 泛型类）
- **观察**：`FileController.list():97` 返回 `Result<Page<FileNodeResponse>>`，一层泛型套一层——看懂为什么 Controller 层不需要强转
- **动手**：把 `Result.success(T data)` 改成非泛型（返回 `Result` 裸类型），看编译警告与调用处如何失去类型检查

### 1.2 枚举 —— `ErrorCode`
- **读** `enums/ErrorCode.java`：枚举带 `code/message` 字段 + 构造器 + getter（不只是常量）
- **操作**：断点 `GlobalExceptionHandler.handleBusinessException():33`，调 `POST /api/auth/login` 传错误密码 → 观察 `e.getErrorCode()` 完整枚举对象

### 1.3 异常链
- **追踪一次完整异常链路**：断点①`AuthServiceImpl.login():83`（抛 `BusinessException(ErrorCode.LOGIN_LOCKED)`）→ 断点②`GlobalExceptionHandler`:33 → 看最终响应体 `{code:xxx, message:"..."}`
- **理解点**：业务异常在 Service 抛、在 @RestControllerAdvice 统一兜底，Controller 全程无 try-catch

### 1.4 反射 + 注解（自定义注解）
- **读** `annotation/Log.java` + `aspect/LogAspect.java`（用反射拿方法注解 + SpEL 解析 `#result.id`）
- **操作**：断点 `LogAspect.around():44`，触发任意 `@Log` 接口（如 `DELETE /api/files/{id}` 删文件）→ 观察 `Method.getAnnotation(Log.class)` 如何拿到注解属性

---

## 2. Spring

### 2.1 IoC/DI —— 构造器注入
- **读** `AuthServiceImpl` 构造器（注入 8 个依赖）——为什么用构造器注入而非 @Autowired 字段
- **操作**：断点 `AuthServiceImpl` 构造器，启动项目 → 观察 Spring 按依赖顺序逐个实例化 Bean

### 2.2 Bean 生命周期 + 初始化回调
- **读** `config/SuperAdminInitializer.java`（`CommandLineRunner`）
- **操作**：断点 `run()`，启动时观察"应用就绪后自动创建超管"这一钩子时机；对照 `SecurityConfig` 的 `@Bean SecurityFilterChain`

### 2.3 配置绑定 —— @ConfigurationProperties
- **读** `config/FileProperties.java`、`config/JwtProperties.java`（前缀绑定 application.yml）
- **操作**：断点 `FileProperties` 构造/Setter，看 `file.upload-expire-hours` 等属性如何注入；改 yml 值重启验证

### 2.4 AOP —— @Log 切面
- **断点** `LogAspect.around():44`，执行 `POST /api/files/upload/merge` → 看 `joinPoint.proceed()` 先执行业务、后写 `t_operation_log`
- **实验**：删掉方法上 `@Log` 注解 → 验证切面不再介入（理解"切入点由注解驱动"）
- **进阶**：`LogAspect.evaluateSpel():82` 断点，看 SpEL `#result.id` 如何从返回值取 ID

### 2.5 Spring Boot 自动配置
- **读** `pom.xml` 依赖 + `config/MinioConfig.java`（`@ConditionalOnProperty` 之类，如无则对照 Spring Boot 自动配置机制）
- **实验**：把 `MinioProperties` 前缀改错 → 启动报"无法绑定属性"，理解配置校验

---

## 3. Web + Security 认证链路（核心实验）

### 实验 3.1 完整登录链路
断点按顺序打：
1. `AuthController.login():40`（HTTP 入口，`IpUtil.getClientIp` 拿 IP）
2. `AuthServiceImpl.login():80`（`userService.findByAccount`）
3. `UserDetailsServiceImpl`（Spring Security 查用户 → 密码比对）
4. `JwtTokenUtilImpl.generateToken()`（签发 JWT）
5. `OperationLogService.log()`（写登录审计日志）

**触发**：`curl -X POST http://localhost:8081/api/auth/login -H "Content-Type: application/json" -d '{"username":"root","password":"root123456"}'`
**观察**：请求 → Filter → Controller → Service → 返回 `LoginResponse(token, userId, role)`

### 实验 3.2 带 Token 访问受保护接口
1. 拿 3.1 的 token
2. `curl http://localhost:8081/api/files -H "Authorization: Bearer <token>"`
3. **断点** `JwtAuthenticationFilter.doFilterInternal():47`：看 `authHeader` 取值 → 验签 → `SecurityContextHolder.getContext().setAuthentication():77`
4. **再验证**：不带 token 请求 → `SecurityConfig:74` 的 `/api/**` 规则拦下 → `AuthenticationEntryPointImpl` 返回 401

### 实验 3.3 Redis 黑名单登出
1. 断点 `JwtBlacklistService.blacklistToken()`
2. `POST /api/auth/logout` 带 token → 观察 token 写入 Redis（`redis-cli keys '*blacklist*'` 看 key，`ttl` 看剩余时长）
3. 再用同一 token 访问 `/api/files` → `JwtAuthenticationFilter:64` 命中黑名单返回"Token 已注销"

### 实验 3.4 登录锁定（Redis 计数）
1. `redis-cli keys 'login:*'` 清场
2. 断点 `LoginAttemptService.loginFailed():39`
3. 连续错密码 5 次 → 观察 `attempts>=threshold:48` 写 `login:lock:{username}` → 第 6 次正确密码也被 `AuthServiceImpl.login():83` 拒绝（LOCKED）
4. `redis-cli TTL login:lock:root` 看锁定时长（默认 30 分钟）；等过期或用 `loginSucceeded` 解锁路径观察自动解锁

### 实验 3.5 Spring Session / 会话
- 本项目的会话方案：**JWT 无状态**（`SecurityConfig:57` STATELESS），不是 SpringSession。对比理解"为什么单体 JWT 无需 Session"
- **读** `security/LoginUser.java`（实现 UserDetails）+ `UserDetailsServiceImpl`

---

## 4. Database（MyBatis + 事务 + 原子操作）

### 实验 4.1 一次带参数的 SQL 查询
1. 断点 `FileServiceImpl.listByUserAndParent():93`，访问 `GET /api/files?parentId=0`
2. 看 `fileMapper.findByUserIdAndParentId(userId, parentId)` 如何被 MyBatis 解析
3. 打开 MyBatis 日志（logback 已配 DEBUG 则可见 SQL），或断点 mapper 代理类观察参数绑定

### 实验 4.2 EXPLAIN 索引优化
```sql
EXPLAIN SELECT * FROM t_file WHERE user_id = ? AND parent_id = ? AND status = ?;
```
- 对照 `FileMapper` 里该查询的 WHERE，看是否走索引（key 列）；若 `type=ALL` 全表扫，分析该加什么联合索引

### 实验 4.3 @Transactional 回滚
- **断点** `FileServiceImpl.deleteToRecycle()`（删除文件 + 写回收站 + 扣配额多步操作）
- **实验**：临时在方法中途 `throw new RuntimeException()` → 观察整个事务回滚（t_file、t_recycle_bin、used_space 均未变化）
- **进阶**：读该方法的 `@Transactional` 注解，思考"调用方与本类自调用"是否事务失效（`this.xxx()` 与注入代理的区别）

### 实验 4.4 数据库原子操作防超限（重点）
- **读** `mapper/ShareMapper.java:25`（`UPDATE ... WHERE status=NORMAL AND ...` 原子下载计数）
- **理解**：为什么用"受影响行数"判断而非"先查后改"（并发安全）；对照 `RedisConstants` 里分享下载去重 key 的配合

---

## 5. Cache（Redis 在认证/上传/限流中的真实用法）

### 实验 5.1 验证码（String + TTL + 一次性）
1. `POST /api/auth/send-code`（邮箱随便填）→ 断点 `CaptchaService.generateAndStore():37`
2. `redis-cli GET "captcha:REGISTER:xxx@mail.com"` 看 6 位码；`TTL` 看 5 分钟
3. 再调一次 → `isOnCooldown():75` 命中冷却返回 CAPTCHA_COOLDOWN

### 实验 5.2 上传元数据（Hash + Set + 多 key TTL）
1. 前端上传一个大文件，或直接调 `POST /api/files/upload/init`（body: fileName/fileSize/fileHash/parentId）
2. 断点 `UploadServiceImpl.init():160` → `redis-cli HGETALL upload:meta:{uploadId}` 看元数据
3. 上传分片后 `redis-cli SMEMBERS upload:chunks:{uploadId}` 看已传分片序号 → 断点 `uploadChunk():225` 看断点续传幂等（重复传同一分片直接 return）
4. `redis-cli SMEMBERS upload:uploading:{userId}` 看并发任务数限制

### 实验 5.3 合并分布式锁（setIfAbsent）
- **断点** `UploadServiceImpl.merge():290`：`redis.opsForValue().setIfAbsent(lockKey,...)` 拿锁
- **理解**：为什么并发 merge 同一 uploadId 时只有 1 个成功；`finally` 里 `redis.delete(MERGE_LOCK)` 保证释放

### 实验 5.4 分享下载去重
- **读** `RedisConstants.SHARE_DOWNLOAD_DEDUP_PREFIX:26` + `ShareServiceImpl` 调用处：60 秒内同 IP 重复下载不重复计数（防刷）

---

## 6. Storage（MinIO + 分片/秒传/预签名）

### 实验 6.1 一次完整分片上传（跟踪到底）
按序断点：
1. `FileController.uploadInit():127` → `UploadServiceImpl.init():114`：自适应分片（`fileSize <= smallFileThreshold ? 1 片 : chunkSize`）、写 Redis 元数据
2. `UploadServiceImpl.uploadChunk():212`：分片写 MinIO（对象名 `IdUtil.uploadChunkObject`），`storageService.upload():231`
3. `UploadServiceImpl.merge():276`：`SequenceInputStream` 合并分片 + `DigestInputStream` 边传边算 SHA-256（`:327`）→ 校验失败抛 UPLOAD_INVALID
4. `FileHashService.register()`：注册秒传索引（共享对象引用）

**观察**：`mc ls`（MinIO 客户端）或 MinIO 控制台，看 `files/{userId}/{fileId}/{name}` 对象生成

### 实验 6.2 秒传命中
1. 上传文件 A 完成后，`redis-cli`/库查 `file_hash` 表的 hash
2. 再调 `POST /api/files/upload/sec`（body 带同一 hash）→ 断点 `UploadServiceImpl.sec():399` → 断点 `FileHashServiceImpl.shareRef()` 引用计数 +1，返回 `hit` 不实际传输

### 实验 6.3 预签名下载
- 断点 `StorageServiceImpl.generateDownloadUrl()`（或 `MinioConfig` 依赖注入点），访问 `GET /api/files/{id}/download`
- **观察**：响应 302 且 `Location` 是带签名参数（X-Amz-Signature 等）的 MinIO URL，文件流不经过后端 → 理解"签名直链节省服务器带宽"

### 实验 6.4 文件隔离
- `StorageServiceImpl` 与 `IdUtil.fileObject(userId,fileId,name)`：对象路径按 userId 隔离（`files/{userId}/`），团队文件 `files/team/{teamId}/` → 看 HLD.md §4

---

## 7. 并发与异步（本项目落点）

### 实验 7.1 批量打包（线程池 + WebSocket 进度）
1. `POST /api/files/download/batch`（body: fileIds 列表）→ 断点 `DownloadServiceImpl.createBatchTask():104`
2. 断点 `packExecutor.execute(() -> pack(task)):126`：观察任务丢进固定线程池（`newFixedThreadPool(2)`，`:74`）
3. 断点 `pack():190`：逐文件 `writeZipEntry` → 上传 MinIO → 广播 DONE
4. 断点 `ProgressWebSocketHandler.broadcast():50`：观察 `{type:"download", status:"PACKING", done, total}` 消息结构
- **理解**：`ConcurrentHashMap<String, BatchTask> tasks`（`:61`）内存任务表 vs 分布式任务区别

### 实验 7.2 进程内 vs 分布式并发
- `DownloadServiceImpl` 是**单实例内存态**（重启丢任务），对照 `UploadServiceImpl` 用 Redis 存状态（重启可恢复）——对比两种异步方案

---

## 8. 定时任务

- **读** `config/FileCleanupTask.java`（`@Scheduled` cron）
- **操作**：断点 `FileCleanupTask` 各清理方法；把 cron 改成每 1 分钟（`0 */1 * * * ?`）观察"回收站过期记录物理清理 + 打包产物清理 + 孤儿分片清理"三条线各做什么

---

## 9. Testing（补课落点）

### 建议第一个单测目标：`LoginAttemptService`
- 用 Mockito mock `StringRedisTemplate` + `AdminSettingsService`
- 场景：5 次失败后 `isLocked()` 返回 true；成功后清 key
- **写法参考**：Spring Boot Test 自动配置，无容器用纯 Mockito 即可

### 建议第一个集成测试目标：注册→登录→访问文件列表
- `@SpringBootTest` + Testcontainers（MySQL+Redis+MinIO 容器），或本地依赖齐全时直连

---

## 10. 定时/延迟队列补课（云盘无 MQ）

### 实验：用 Redis ZSet 模拟"回收站 30 天自动清空"延迟队列
1. 建 key `queue:recycle:cleanup`，score=到期时间戳，member=记录id
2. 定时任务 `ZRANGEBYSCORE ... -inf now` 取到期项 → 清理 → `ZREM`
3. 对照 `docs/notes/gulimall-cloud-compare.md` 中 RabbitMQ 延迟队列+死信的思路，理解"为什么谷粒用 MQ，云盘用 cron+TTL"

---

## 11. 建议顺序（由浅入深，每步 30-60 分钟）
1. **3.1-3.5 认证链路**（覆盖 Java 异常/泛型、Spring IoC/Filter、Security/JWT、Redis 黑名单/锁定）
2. **6.1-6.4 上传链路**（覆盖 Web/Multipart、MinIO、Redis 元数据、分片合并算法、SHA-256）
3. **4.1-4.4 数据库**（MyBatis、EXPLAIN、事务、原子操作防超限）
4. **7.1-7.2 异步打包**（线程池 + WS + 内存任务表）
5. **2.1-2.5 Spring 内部**（构造器注入、生命周期、配置绑定、AOP）
6. **8 定时任务** + **10 延迟队列补课**
7. **9 补测试** → 部署 → 微服务演进（可选，先吃透单体）

## 12. Java 特性 × Spring 框架运用对照（全面）

> 核心认知：Spring 本身是"用 Java 特性搭起来的框架"。看懂下面这张表，等于同时复习 Java 语言特性 + 理解 Spring 的设计哲学。
> 每个条目都标了"Spring 框架内部在用它做什么"和"本项目代码里如何体现"。

### 12.1 泛型

| 运用点 | Spring 框架内部做什么 | 本项目落点 |
| --- | --- | --- |
| 泛型容器 | `ApplicationContext.getBean(Class<T>)` 返回带类型的 Bean；`ResolvableType` 解析泛型签名 | `dto/Result<T>`、`dto/Page<T>`：Controller 返回 `Result<Page<FileNodeResponse>>`（`FileController.list():97`） |
| 泛型通配符 | `List<@NonNull ? extends GrantedAuthority>` 权限集合 | `LoginUser.getAuthorities():42` 返回 `Collection<? extends GrantedAuthority>` |
| 泛型方法 | `RestTemplate.exchange(url, method, body, Class<T>)` 反序列化泛型 | `Result.success(T data)` / `Result.fail(...)` 静态泛型方法（`Result.java:29-59`） |
| 泛型擦除 | Jackson 反序列化 `List<T>` 需 `TypeReference` 记录泛型 | 无直接落点，理解"运行时擦除"即可；WebSocket 广播用 `Map<String,Object>` 规避泛型（`ProgressWebSocketHandler:50`） |
| 泛型上限 | `? extends T` 只读安全 | `Result.fail()` 返回 `Result<Void>` 表示无数据（`GlobalExceptionHandler:33`） |

### 12.2 枚举

| 运用点 | Spring 框架内部做什么 | 本项目落点 |
| --- | --- | --- |
| 配置绑定 | `@ConfigurationProperties` 把 yml 字符串转枚举（`spring.profiles` 等） | 5 个 `@ConfigurationProperties` 类（`FileProperties`/`JwtProperties`/`MinioProperties`/`MailProperties`/`RedisProperties`） |
| 请求参数转换 | `Converter<String, T>` 把 URL 参数转枚举（`@RequestParam`） | DTO 里枚举字段反序列化：`LogFilterRequest.operation`（`OperationType`）、`AdminShareResponse.status`（`ShareStatus`） |
| 权限模型 | `GrantedAuthority` 用字符串"ROLE_"约定 | `LoginUser:43` 用 `switch (role)` 把 `Role` 映射为 `ROLE_ADMIN` 等；`SecurityConfig:65` 的 `hasAnyRole("ADMIN","SUPER_ADMIN")` |
| 持久化映射 | MyBatis `TypeHandler` 枚举↔数据库 | `MyBatisTypeHandlerConfig`（`EnumOrdinalTypeHandler` 按 ordinal 存 TINYINT）+ `TeamMemberRoleTypeHandler`（自定义 value 0/10/20） |
| 状态机 | 枚举驱动业务流转 | `FileStatus`/`ShareStatus`/`UserStatus` 各状态；`FileCleanupTask` 按 status 清理 |
| 错误码 | 枚举统一错误语义 | `enums/ErrorCode.java`（code+message），`GlobalExceptionHandler` 读取 |

### 12.3 注解（Annotation）—— Spring 的灵魂

| 运用点 | Spring 框架内部做什么 | 本项目落点 |
| --- | --- | --- |
| 元注解 | `@Component` 是元注解，`@Service`/`@Controller`/`@Repository` 都是它的派生（@Component 组合） | 全部 Service 用 `@Service`，Controller 用 `@RestController` |
| 装配注解 | 按类型/名称注入 | 构造器注入（无注解）+ `@ConfigurationProperties` 自动装配 |
| 配置注解 | `@Configuration` + `@Bean` 显式声明 Bean | `SecurityConfig`、`MinioConfig`、`PasswordEncoderConfig` 等 6 个 `@Bean` |
| 条件注解 | `@Conditional*` 按条件注册 Bean | `MinioConfig:50` `@ConditionalOnProperty` 控制自动建桶开关 |
| AOP 注解 | `@Aspect`/`@Around`/`@Pointcut` 织入 | `LogAspect` + 自定义 `@Log` 注解（`annotation/Log.java`） |
| 校验注解 | `@Valid` + Bean Validation 校验 DTO | `@Valid @RequestBody`（`FileController` 全部写接口）+ `GlobalExceptionHandler:39` 处理 |
| 事件注解 | `@EventListener` 监听 Spring 事件 | `MinioConfig:49` 监听 `ApplicationReadyEvent`；`UserEventListener` 监听 `UserRegisteredEvent` |
| 元注解组合 | `@RestController = @Controller + @ResponseBody` | 全部控制器只用 `@RestController`（理解它"包含"了什么） |
| 事务注解 | `@Transactional` 声明式事务（AOP 织入） | `FileServiceImpl.deleteToRecycle` 等多步写操作 |
| 生命周期注解 | `@PostConstruct`/`@PreDestroy`（本项目改用事件替代） | `MinioConfig` 注释里解释了"为什么不用 @PostConstruct" |

### 12.4 反射（Reflection）

| 运用点 | Spring 框架内部做什么 | 本项目落点 |
| --- | --- | --- |
| 类扫描 | `ClassPathScanningCandidateComponentProvider` 扫描 `@Component` 注册 Bean | `CloudBackendApplication`（`@SpringBootApplication` 触发组件扫描） |
| 方法调用 | 通过反射调用 Controller 方法并做参数绑定 | `LogAspect:46` `MethodSignature.getMethod()` + `getAnnotation(Log.class)` |
| 动态代理 | CGLIB/JDK 代理生成代理对象（事务、AOP、Security） | `@Transactional`/`@Log` 生效的原理 |
| 泛型解析 | 反射读取泛型签名 | `LogAspect.evaluateSpel():82` 用 `signature.getParameterNames()` 拿参数名绑定 SpEL |
| 注入字段 | `Field.setAccessible(true)` 反射注入 | Lombok `@Getter`/`@Data` 本身就是"编译期反射"的替代 |

### 12.5 接口与多态（面向对象核心）

| 运用点 | Spring 框架内部做什么 | 本项目落点 |
| --- | --- | --- |
| 接口驱动开发 | Spring 面向接口编程，Bean 按接口注入 | 每个领域一个接口 + 实现：`FileService`/`UploadService`/`ShareService` 等 9 组接口-实现 |
| 策略模式 | 同一接口多个实现，按条件选择 | `StorageService` 抽象（可换 MinIO→OSS→S3），`StorageServiceImpl` 为当前实现 |
| 模板方法 | 抽象基类定义骨架，子类覆写 | `StorageService.ObjectInfo`（record）封装对象元信息 |
| 适配器 | 让不兼容接口协作 | `TeamMemberRoleTypeHandler extends BaseTypeHandler` 适配 MyBatis 与枚举 |
| 组合优于继承 | Spring 大量用组合（构造器注入） | `AuthServiceImpl` 注入 8 个依赖组合出登录/注册/验证码能力 |

### 12.6 函数式接口与 Lambda（Java 8+）

| 运用点 | Spring 框架内部做什么 | 本项目落点 |
| --- | --- | --- |
| 回调/事件 | 函数式接口作为回调参数 | `FileCleanupTask` 定时方法；WebSocket 会话回调 |
| 流式 API | Spring Data 的 `Function` 风格 | `FileController.recycleBin():303` `.stream().map(...).toList()` |
| 方法引用 | `::` 简洁调用 | `FileService` 内部 `Collectors.groupingBy(File::getParentId)`（`DownloadServiceImpl:257`） |
| 线程池任务 | `Runnable`/`Callable` 提交 | `packExecutor.execute(() -> pack(task))`（`DownloadServiceImpl:126`） |
| 配置 lambda | Security 配置的链式 lambda | `SecurityConfig` 全部 `auth ->` / `cors ->` / `session ->` 写法 |

### 12.7 异常处理

| 运用点 | Spring 框架内部做什么 | 本项目落点 |
| --- | --- | --- |
| 统一异常处理 | `@ExceptionHandler` + `@RestControllerAdvice` 拦截 | `GlobalExceptionHandler`（6 个处理器，见 §1.3） |
| 运行时异常体系 | `NestedRuntimeException`/`NestedCheckedException` 包装底层异常 | `BusinessException extends RuntimeException` 统一业务异常 |
| 事务回滚 | 异常触发 `@Transactional` 回滚 | `FileServiceImpl.deleteToRecycle`（§4.3 实验） |
| 异常转换 | `@Repository` 将 SQL 异常转 `DataAccessException` | MyBatis 通过 starter 自动完成 |

### 12.8 Java 21 新特性（本项目已用）

| 特性 | 本项目落点 |
| --- | --- |
| switch 表达式 + 箭头语法 | `LoginUser:43`、`AuthServiceImpl:195` |
| instanceof 模式匹配 | `AuthorizationPolicy:35`（`principal instanceof LoginUser loginUser`） |
| record | `StorageService.ObjectInfo`、`RecycleBinServiceImpl` 等 4 处 |
| 文本块 / Sealed / 虚拟线程 | 未用（可作为学习补充） |

---

## 13. 建议顺序（由浅入深，每步 30-60 分钟）
1. **3.1-3.5 认证链路**（覆盖 Java 异常/泛型、Spring IoC/Filter、Security/JWT、Redis 黑名单/锁定）
2. **6.1-6.4 上传链路**（覆盖 Web/Multipart、MinIO、Redis 元数据、分片合并算法、SHA-256）
3. **4.1-4.4 数据库**（MyBatis、EXPLAIN、事务、原子操作防超限）
4. **7.1-7.2 异步打包**（线程池 + WS + 内存任务表）
5. **2.1-2.5 Spring 内部**（构造器注入、生命周期、配置绑定、AOP）
6. **8 定时任务** + **10 延迟队列补课**
7. **12.1-12.8 Java×Spring 对照**（边做边回看，理解"Spring 为何这样用 Java"）
8. **9 补测试** → 部署 → 微服务演进（可选，先吃透单体）

## 关联
- 架构：`docs/HLD.md`；谷粒对照：`docs/notes/gulimall-cloud-compare.md`；决策：`docs/adr/`
