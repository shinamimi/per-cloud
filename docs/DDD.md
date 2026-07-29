# Cloud 企业级云盘 — 详细设计文档

> 版本: v0.1
> 更新日期: 2026-07-28
> 状态: Draft
> 基于: PRD v0.1, HLD v0.1

---

## 1. 文档说明

本文档在 HLD 的模块划分基础上，对每个模块进行类级别设计，包括：包结构、类/接口方法签名、DTO 定义、错误码、状态机、数据库操作接口、关键算法、并发处理、测试策略。

每个模块设计为可独立编译、独立测试，通过 Mock 外部依赖（其他模块的 Service 接口 / 存储层）来实现解耦。

---

## 2. 通用约定

### 2.1 响应格式

所有 API 统一返回 `Result<T>`：

```java
public class Result<T> {
    private int code;       // 200=成功, 其他=失败
    private String message;
    private T data;
}
```

### 2.2 错误码体系

```java
// 通用 (10000-10099)
SUCCESS(200, "success"),
BAD_REQUEST(10000, "参数错误"),
UNAUTHORIZED(10001, "未登录"),
FORBIDDEN(10002, "无权限"),
NOT_FOUND(10003, "资源不存在"),
METHOD_NOT_ALLOWED(10004, "请求方法不支持"),
INTERNAL_ERROR(10500, "服务器内部错误"),

// 认证 (10100-10199)
LOGIN_LOCKED(10100, "账号已锁定"),
CAPTCHA_INVALID(10101, "验证码错误"),
CAPTCHA_COOLDOWN(10102, "验证码发送过于频繁"),

// 文件 (10200-10299)
FILE_NAME_DUPLICATE(10200, "文件名已存在"),
FILE_QUOTA_EXCEEDED(10201, "空间配额不足"),
FILE_NOT_FOUND(10202, "文件不存在"),
FILE_HASH_MATCH(10203, "文件已存在(秒传)"),
UPLOAD_INVALID(10204, "上传参数错误"),
UPLOAD_CHUNK_MISSING(10205, "分片缺失"),
UPLOAD_MERGE_FAILED(10206, "分片合并失败"),

// 分享 (10300-10399)
SHARE_EXPIRED(10300, "分享已过期"),
SHARE_PASSWORD_REQUIRED(10301, "需要提取码"),
SHARE_PASSWORD_INVALID(10302, "提取码错误"),

// 团队 (10400-10499)
TEAM_NAME_DUPLICATE(10400, "团队名已存在"),
TEAM_NOT_FOUND(10401, "团队不存在"),
TEAM_MEMBER_EXISTS(10402, "成员已在团队中"),
TEAM_OWNER_CANNOT_LEAVE(10403, "所有者不能退出团队"),
TEAM_QUOTA_EXCEEDED(10404, "团队空间配额不足");
```

### 2.3 分页请求/响应

```java
public class PageRequest {
    private int page = 1;
    @Max(100)
    private int size = 20;
}

public class PageResponse<T> {
    private List<T> records;
    private long total;
    private int page;
    private int size;
}
```

### 2.4 认证上下文

```java
// 已有实现: security/LoginUser.java
// Controller 中通过 @AuthenticationPrincipal LoginUser loginUser 获取当前用户
public class LoginUser implements UserDetails {
    private Long userId;
    private String username;
    private Role role;
    // ...
}
```

---

## 3. 模块 M10 — 基础设施（新增部分）

### 3.1 新增全局配置

```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(uploadProgressHandler(), "/ws/upload/progress/{uploadId}")
                .setAllowedOrigins("*");
        registry.addHandler(packageProgressHandler(), "/ws/package/progress/{taskId}")
                .setAllowedOrigins("*");
    }
}
```

### 3.2 新增常量

```java
public class RedisConstants {
    public static final String UPLOAD_PROGRESS_PREFIX = "upload:progress:";
    public static final String UPLOAD_META_PREFIX = "upload:meta:";
    public static final String PACKAGE_TASK_PREFIX = "package:task:";
}
```

### 3.3 新增工具类

```java
// 缩略图生成
public class ThumbnailUtil {
    public static String generateThumbnail(String sourcePath, String thumbPath, int width);
    // 返回生成的缩略图 MinIO objectName
}
```

### 3.4 测试策略

基础设施模块无需独立测试，配置变更在集成测试中验证。

---

## 4. 模块 M1 — 认证授权（已有，不变）

### 4.1 现有完整类清单

| 类 | 路径 | 说明 |
|---|------|------|
| AuthController | `controller/AuthController.java` | 6 个公开接口 |
| JwtTokenUtil | `utils/JwtTokenUtil.java` | Token 生成/解析/校验 |
| JwtBlacklistService | `service/JwtBlacklistService.java` | Token 登出黑名单 |
| LoginAttemptService | `service/LoginAttemptService.java` | 登录失败计数 + 锁定 |
| CaptchaService | `service/CaptchaService.java` | 验证码生成/校验 |
| EmailService | `service/EmailService.java` | 邮件发送 |
| JwtAuthenticationFilter | `security/JwtAuthenticationFilter.java` | 请求拦截解析 Token |
| LoginUser | `security/LoginUser.java` | 认证用户上下文 |
| UserDetailsServiceImpl | `security/UserDetailsServiceImpl.java` | 登录时加载用户 |
| SecurityConfig | `config/SecurityConfig.java` | 安全配置 |

### 4.2 测试策略

- `AuthController`: Mock UserService / CaptchaService / EmailService，测试正常登录/注册/验证码场景
- `JwtTokenUtil`: 纯工具类，白盒测试 Token 生成和校验
- `LoginAttemptService`: 测试 Redis 失败计数和锁定逻辑

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

### 5.2 新增 Controller

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    /** 获取个人信息 */
    @GetMapping("/me")
    public Result<UserProfileResponse> getProfile(@AuthenticationPrincipal LoginUser loginUser) {
        User user = userService.findById(loginUser.getUserId());
        // 返回脱敏信息（不返回密码）
    }

    /** 修改个人资料（昵称、头像） */
    @PutMapping("/me")
    public Result<Void> updateProfile(@AuthenticationPrincipal LoginUser loginUser,
                                      @Valid @RequestBody UserUpdateRequest request);

    /** 修改密码 */
    @PutMapping("/me/password")
    public Result<Void> updatePassword(@AuthenticationPrincipal LoginUser loginUser,
                                       @Valid @RequestBody PasswordUpdateRequest request);

    /** 空间使用情况 */
    @GetMapping("/me/quota")
    public Result<QuotaResponse> getQuota(@AuthenticationPrincipal LoginUser loginUser);
}
```

### 5.3 DTO 定义

```java
@Data
public class UserUpdateRequest {
    @Size(max = 50)
    private String nickname;
    @Size(max = 255)
    private String avatar;
}

@Data
public class PasswordUpdateRequest {
    @NotBlank
    private String oldPassword;
    @NotBlank @Size(min = 6, max = 32)
    private String newPassword;
}

@Data
public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    private String nickname;
    private String avatar;
    private Integer role;
    private Long quota;
    private Long usedSpace;
    private Integer status;
    private LocalDateTime createdAt;
}

@Data
public class QuotaResponse {
    private Long quota;      // 总配额（字节）
    private Long usedSpace;  // 已用空间（字节）
    private Double usagePercent;
}
```

### 5.4 错误码

| 错误码 | 场景 |
|--------|------|
| PASSWORD_INVALID | 旧密码不匹配 |
| USER_NOT_FOUND | (通用) 用户不存在 |

### 5.5 测试策略

- Mock UserService，测试 profile 读写、密码修改
- 密码修改需验证旧密码正确性后再更新

---

## 6. 模块 M3 — 文件管理

### 6.1 包结构

```
controller/FileController.java
dto/request/
├── DirectoryCreateRequest.java
├── UploadInitRequest.java
├── UploadChunkRequest.java    // MultipartFile
├── UploadMergeRequest.java
├── UploadSecRequest.java
├── FileRenameRequest.java
├── FileMoveRequest.java
├── FileCopyRequest.java
├── FileSearchRequest.java
└── BatchDownloadRequest.java
dto/response/
├── FileNodeResponse.java      // 文件列表项
├── FileTreeResponse.java      // 目录树节点
├── UploadInitResponse.java
├── UploadMergeResponse.java
└── FilePreviewResponse.java
service/UploadService.java         // 新增：分片上传逻辑
service/impl/UploadServiceImpl.java
service/SearchService.java         // 新增：文件搜索
service/impl/SearchServiceImpl.java
service/ThumbnailService.java      // 新增：缩略图生成
service/impl/ThumbnailServiceImpl.java
service/PackageService.java        // 新增：打包下载
service/impl/PackageServiceImpl.java
```

### 6.2 新增 Service 接口

```java
public interface UploadService {
    /** 初始化分片上传，返回 uploadId */
    UploadInitResponse initUpload(Long userId, UploadInitRequest request);

    /** 上传一个分片，写入 MinIO 临时目录 */
    void uploadChunk(Long userId, String uploadId, int chunkNumber, MultipartFile file);

    /** 合并分片，写入 t_file + MinIO 正式路径 */
    File mergeChunks(Long userId, String uploadId, UploadMergeRequest request);

    /** 通过 file_hash 秒传 */
    File secUpload(Long userId, UploadSecRequest request);

    /** 清理指定上传的分片临时数据 */
    void cleanupUpload(String uploadId);

    /** 清理过期未完成的上传（定时任务调用，扫描 Redis 中超过 2 小时未更新的 uploadId） */
    void cleanupExpiredUploads();
}

public interface SearchService {
    PageResponse<File> searchFiles(Long userId, String keyword, Long parentId, PageRequest page);
}

public interface ThumbnailService {
    /** 生成图片缩略图，返回缩略图 objectName */
    String generateThumbnail(Long userId, File file);

    /** 获取缩略图下载 URL */
    String getThumbnailUrl(Long userId, Long fileId);
}

public interface PackageService {
    /** 设置任务过期时间（创建时写入 Redis，TTL=1 小时 */
    String createPackageTask(Long userId, List<Long> fileIds);

    /** 查询打包任务状态 */
    PackageTaskStatus getTaskStatus(String taskId);

    /** 获取打包文件下载 URL（仅 completed 状态可用） */
    String getPackageDownloadUrl(String taskId);

    /** 清理过期打包任务及 MinIO 中的 zip 文件 */
    void cleanupExpiredTasks();
}
```

### 6.3 DTO 定义

```java
// ==== 请求 ====

@Data
public class DirectoryCreateRequest {
    @NotNull
    private Long parentId;
    @NotBlank @Size(max = 255)
    private String name;
    private Long teamId; // 可选，团队空间目录
}

@Data
public class UploadInitRequest {
    @NotBlank @Size(max = 255)
    private String fileName;
    private Long fileSize;
    @Size(max = 64)
    private String fileHash;  // SHA256
    @NotNull
    private Long parentId;
    private Long teamId; // 可选
}

@Data
public class UploadInitResponse {
    private String uploadId;
    private int chunkSize;     // 默认 5MB
    private int totalChunks;
    private boolean uploaded;  // 秒传命中时 true
}

@Data
public class UploadMergeRequest {
    @NotBlank
    private String uploadId;
    @NotBlank
    private String fileName;
    @NotNull
    private Long parentId;
    private Long teamId;
}

@Data
public class UploadSecRequest {
    @NotBlank @Size(max = 64)
    private String fileHash;  // SHA256
    @NotBlank @Size(max = 255)
    private String fileName;
    private Long fileSize;
    @NotNull
    private Long parentId;
    private Long teamId;
}

@Data
public class FileRenameRequest {
    @NotBlank @Size(max = 255)
    private String name;
}

@Data
public class FileMoveRequest {
    @NotNull
    private Long targetParentId;
}

@Data
public class FileCopyRequest {
    @NotNull
    private Long targetParentId;
}

@Data
public class FileSearchRequest {
    @NotBlank
    private String keyword;
    private Long parentId; // 可选，限定搜索范围
    private Long teamId;
    private int page = 1;
    private int size = 20;
}

@Data
public class BatchDownloadRequest {
    @NotEmpty
    private List<Long> fileIds;
}

// ==== 响应 ====

@Data
public class FileNodeResponse {
    private Long id;
    private String name;
    private Long parentId;
    private Long size;
    private String mimeType;
    private String extension;
    private boolean isDirectory;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

@Data
public class FileTreeResponse {
    private Long id;
    private String name;
    private boolean isDirectory;
    private List<FileTreeResponse> children;
}
```

### 6.4 分片上传算法

```java
// UploadServiceImpl.mergeChunks() 核心逻辑

public File mergeChunks(Long userId, String uploadId, UploadMergeRequest request) {
    // 1. 从 Redis 获取总分片数
    int totalChunks = redisTemplate.opsForValue()
            .get(UPLOAD_META_PREFIX + uploadId);

    // 2. 校验所有分片已上传
    for (int i = 1; i <= totalChunks; i++) {
        String objectName = "uploads/" + userId + "/" + uploadId + "/chunk_" + i;
        if (!storageService.objectExists(objectName)) {
            throw new BusinessException(UPLOAD_CHUNK_MISSING, "分片 " + i + " 缺失");
        }
    }

    // 3. 写入 t_file 获取 fileId
    File file = new File();
    // ... 填充属性
    fileService.save(file);

    // 4. 在 MinIO 中合并分片
    // 注意：composeObject 成功后若后续 DB 更新失败，会产生"对象已存在但 file.object_name 为空"的数据不一致
    // MVP 阶段接受此不一致性，后续通过定时扫描修复（不引入分布式事务）
    // 方案：使用 MinIO composeObject API 合并
    String targetObject = "files/" + userId + "/" + file.getId() + "/" + request.getFileName();
    List<ComposeSource> sources = new ArrayList<>();
    for (int i = 1; i <= totalChunks; i++) {
        sources.add(ComposeSource.builder()
                .bucket(minioProperties.getBucket())
                .object("uploads/" + userId + "/" + uploadId + "/chunk_" + i)
                .build());
    }
    minioClient.composeObject(ComposeObjectArgs.builder()
            .bucket(minioProperties.getBucket())
            .object(targetObject)
            .sources(sources)
            .build());

    // 5. 更新 file.objectName
    file.setObjectName(targetObject);
    fileService.update(file);

    // 6. 清理临时分片和 Redis 状态
    for (int i = 1; i <= totalChunks; i++) {
        storageService.delete("uploads/" + userId + "/" + uploadId + "/chunk_" + i);
    }
    redisTemplate.delete(UPLOAD_META_PREFIX + uploadId);

    return file;
}
```

### 6.5 秒传算法

```java
public File secUpload(Long userId, UploadSecRequest request) {
    // 1. 查询是否存在相同 hash 的文件
    // Mapper: File findByHash(@Param("fileHash") String fileHash, @Param("status") Integer status);
    File existing = fileMapper.findByHash(request.getFileHash(), FileStatus.NORMAL.getValue());
    if (existing == null) {
        // 无匹配，返回需要分片上传
        throw new BusinessException(UPLOAD_INVALID, "hash_not_found");
    }

    // 2. 校验配额（与分片上传相同的校验）
    validateQuota(userId, request.getFileSize());

    // 校验配额（个人或团队）
    if (request.getTeamId() != null) {
        validateTeamQuota(request.getTeamId(), request.getFileSize());
    } else {
        validateQuota(userId, request.getFileSize());
    }

    // 3. 复制已有对象到当前用户目录（隔离存储，防止误删影响他人）
    String targetObject = "files/" + userId + "/" + IdUtil.generateId() + "/" + request.getFileName();
    storageService.copyObject(existing.getObjectName(), targetObject);

    File file = new File();
    file.setUserId(userId);
    file.setParentId(request.getParentId());
    file.setName(request.getFileName());
    file.setSize(existing.getSize());
    file.setMimeType(existing.getMimeType());
    file.setExtension(FileUtil.getExtension(request.getFileName()));
    file.setFileHash(request.getFileHash());
    file.setObjectName(targetObject);
    file.setIsDirectory(false);
    file.setStatus(FileStatus.NORMAL);
    if (request.getTeamId() != null) {
        file.setTeamId(request.getTeamId());
    }
    fileService.save(file);

    return file;
}
```

### 6.6 目录树构建算法

```java
// FileController.getTree()

public Result<List<FileTreeResponse>> getTree(@AuthenticationPrincipal LoginUser loginUser,
                                              @RequestParam(required = false) Long parentId,
                                              @RequestParam(required = false) Long teamId) {
    Long userId = teamId != null ? null : loginUser.getUserId();
    List<File> allDirs = fileMapper.listDirectories(userId, teamId);
    // 用 Map 邻接表一次扫描构建树，避免递归 O(n²)
    Map<Long, List<FileTreeResponse>> parentMap = new HashMap<>();
    List<FileTreeResponse> roots = new ArrayList<>();
    for (File dir : allDirs) {
        FileTreeResponse node = new FileTreeResponse();
        node.setId(dir.getId());
        node.setName(dir.getName());
        node.setDirectory(true);
        node.setChildren(new ArrayList<>());
        parentMap.computeIfAbsent(dir.getParentId(), k -> new ArrayList<>()).add(node);
    }
    for (Map.Entry<Long, List<FileTreeResponse>> entry : parentMap.entrySet()) {
        Long pid = entry.getKey();
        for (FileTreeResponse child : entry.getValue()) {
            List<FileTreeResponse> siblings = parentMap.get(child.getId());
            if (siblings != null) {
                child.setChildren(siblings);
            }
        }
        if (pid.equals(parentId != null ? parentId : 0L)) {
            roots.addAll(parentMap.get(pid));
        }
    }
    return Result.success(roots);
}
```

### 6.7 文件预览逻辑

```java
// 根据 mimeType 分发预览策略
public Result<FilePreviewResponse> preview(Long fileId, LoginUser loginUser) {
    File file = fileService.findById(fileId);
    // 校验权限（个人文件/团队文件）

    FilePreviewResponse resp = new FilePreviewResponse();
    resp.setType(detectPreviewType(file.getMimeType()));

    switch (resp.getType()) {
        case IMAGE:
            // 生成缩略图 URL
            resp.setUrl(thumbnailService.getThumbnailUrl(loginUser.getUserId(), fileId));
            break;
        case TEXT:
            // 读取文件内容（限 1MB 以内）
            InputStream in = storageService.download(file.getObjectName());
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            resp.setContent(content);
            break;
        case UNSUPPORTED:
            return Result.fail("不支持预览该文件类型");
    }
    return Result.success(resp);
}

enum PreviewType { IMAGE, TEXT, UNSUPPORTED }
```

### 6.8 并发处理

- **分片上传并发**: 每个 uploadId 独立，Redis 记录已上传分片序号。多个分片同时上传不冲突
- **合并时的并发安全**: 合并前检查所有分片就绪。可加 Redis 分布式锁 `lock:merge:{uploadId}`，防止同一 uploadId 被多次合并
- **目录重名检查**: 在 `(user_id, parent_id, name, team_id)` 上建唯一索引（`is_directory=1` 的部分索引），由数据库保证不重名，无需业务层加锁
- **配额检查**: 上传/秒传前检查 `used_space + fileSize <= quota`，更新配额使用 `UPDATE t_user SET used_space = used_space + #{size} WHERE id = #{userId}` 原子操作
- **团队配额校验**: 团队文件上传时校验 `t_team.used_space + fileSize <= t_team.quota`

### 6.9 定时任务

```java
@Component
public class UploadCleanupTask {
    @Scheduled(fixedRate = 3600000) // 每小时执行一次
    public void cleanExpiredUploads() {
        // 扫描 Redis 中超过 2 小时未更新的 UPLOAD_META_PREFIX 记录
        // 删除 MinIO 中的临时分片
        // 清理 Redis 中的元数据
    }
}
```

### 6.10 文件列表 Mapper 扩展

```java
// FileMapper 需要新增的方法

/** 按用户和父目录列出文件（含分页） */
Page<File> findByUserIdAndParentId(Long userId, Long parentId, Pageable pageable);

/** 按团队和父目录列出文件 */
List<File> findByTeamIdAndParentId(@Param("teamId") Long teamId,
                                   @Param("parentId") Long parentId,
                                   @Param("status") Integer status);

/** 递归查询目录下所有子文件（用于递归删除/打包） */
List<File> findDescendants(@Param("parentId") Long parentId,
                           @Param("userId") Long userId);

/** 按 hash 查询文件 */
File findByHash(@Param("fileHash") String fileHash, @Param("status") Integer status);

/** 搜索文件 */
List<File> search(@Param("userId") Long userId,
                  @Param("keyword") String keyword,
                  @Param("parentId") Long parentId,
                  @Param("teamId") Long teamId);

/** 列出所有目录 */
List<File> listDirectories(@Param("userId") Long userId, @Param("teamId") Long teamId);
```

### 6.11 测试策略

| 测试类 | 策略 |
|--------|------|
| `FileControllerTest` | Mock FileService / UploadService / SearchService，测试参数校验和路由 |
| `UploadServiceTest` | Mock FileService / StorageService / RedisTemplate，测试 init/uploadChunk/merge/sec 全流程 |
| `SearchServiceTest` | Mock FileMapper，测试模糊搜索 |
| `ThumbnailServiceTest` | Mock StorageService，测试缩略图生成 |
| `PackageServiceTest` | Mock FileService / StorageService，测试打包任务 |

---

## 7. 模块 M4 — 分享管理

### 7.1 包结构

```
controller/ShareController.java
controller/GuestShareController.java       // 公开访问（无需认证）
dto/
├── ShareCreateRequest.java
├── ShareVerifyRequest.java
├── ShareCreateResponse.java
├── ShareListResponse.java
├── ShareAccessResponse.java
```

### 7.2 Controller 定义

```java
@RestController
@RequestMapping("/api/shares")
public class ShareController {

    /** 创建分享 */
    @PostMapping
    public Result<ShareCreateResponse> create(@AuthenticationPrincipal LoginUser loginUser,
                                              @Valid @RequestBody ShareCreateRequest request);

    /** 我的分享列表 */
    @GetMapping
    public Result<List<ShareListResponse>> list(@AuthenticationPrincipal LoginUser loginUser);

    /** 取消分享 */
    @DeleteMapping("/{id}")
    public Result<Void> cancel(@AuthenticationPrincipal LoginUser loginUser,
                               @PathVariable Long id);
}

// 公开访问，permitAll
@RestController
@RequestMapping("/api/shares/access")
public class GuestShareController {

    /** 获取分享文件信息（需验证提取码或公开分享） */
    @GetMapping("/{token}")
    public Result<ShareAccessResponse> access(@PathVariable String token);

    /** 验证提取码 */
    @PostMapping("/{token}/verify")
    public Result<Void> verify(@PathVariable String token,
                               @Valid @RequestBody ShareVerifyRequest request);

    /** 分享内文件预览 */
    @GetMapping("/{token}/file/{fileId}/preview")
    public Result<FilePreviewResponse> preview(@PathVariable String token,
                                               @PathVariable Long fileId);
}
```

### 7.3 DTO 定义

```java
@Data
public class ShareCreateRequest {
    @NotNull
    private Long fileId;
    private LocalDateTime expireTime; // null 表示永久
    @Size(max = 6)
    private String accessPassword; // 可选提取码
    private Long teamId;           // 可选，团队文件分享
}

@Data
public class ShareCreateResponse {
    private String shareToken;
    private String shareUrl;     // 前端拼接的完整链接
    private LocalDateTime expireTime;
}

@Data
public class ShareListResponse {
    private Long id;
    private String fileName;
    private Long fileSize;
    private String shareToken;
    private String shareUrl;
    private LocalDateTime expireTime;
    private Integer status;      // 0-正常 1-已过期 2-已取消
    private Integer downloadCount;
    private LocalDateTime createdAt;
}

@Data
public class ShareVerifyRequest {
    @NotBlank
    private String password;
}

@Data
public class ShareAccessResponse {
    private String shareToken;
    private String fileName;
    private Long fileId;
    private Long fileSize;
    private boolean hasPassword;  // 是否设置了提取码
    private boolean verified;     // 当前请求是否已验证
}
```

### 7.4 状态机

```
NORMAL → (过期) → EXPIRED
NORMAL → (取消) → CANCELED
```

过期检查：分享访问时，`expire_time < now()` 视为过期。

### 7.5 测试策略

- `ShareControllerTest`: Mock ShareService，测试创建/列表/取消
- `GuestShareControllerTest`: Mock ShareService/FileService，测试访问/验证/预览
- 验证码校验：先验证密码再返回文件信息

---

## 8. 模块 M5 — 回收站

### 8.1 职责

回收站在 MVP 中仅作为**存储层**使用——文件"删除"时标记逻辑删除并移入回收站，**不物理删除 MinIO 对象**。用户端回收站管理界面（浏览、恢复、彻底删除）在 v0.2 实现。

### 8.2 调用方式

回收站逻辑在 FileController.delete 中内联调用，无独立 Controller：

```java
// FileController 删除逻辑
public Result<Void> delete(Long fileId, LoginUser loginUser) {
    File file = fileService.findById(fileId);
    // 校验权限...
    if (file.getIsDirectory()) {
        List<File> descendants = fileMapper.findDescendants(fileId, loginUser.getUserId());
        for (File f : descendants) {
            markAsDeleted(loginUser.getUserId(), f);
        }
    } else {
        markAsDeleted(loginUser.getUserId(), file);
    }
    return Result.success();
}

private void markAsDeleted(Long userId, File file) {
    // 仅写回收站记录，不物理删除 MinIO 对象（物理删除在 v0.2 回收站过期时执行）
    RecycleBin rb = new RecycleBin();
    rb.setUserId(userId);
    rb.setFileId(file.getId());
    rb.setOriginalName(file.getName());
    rb.setObjectName(file.getObjectName());
    rb.setParentId(file.getParentId());
    rb.setSize(file.getSize());
    rb.setMimeType(file.getMimeType());
    rb.setExpireTime(LocalDateTime.now().plusDays(30));
    recycleBinService.save(rb);
    // 逻辑删除 t_file 记录（status 字段）
    fileService.updateStatus(file.getId(), FileStatus.DELETED.getValue());
}
```

### 8.3 v0.2 规划

- `controller/RecycleBinController`: 查找回收站、恢复（还原 t_file.status=NORMAL 并删除 recycle_bin 记录）、彻底删除（物理删除 MinIO 对象 + 清理 recycle_bin 记录）
- `dto/RecycleBinListResponse`: id, originalName, mimeType, size, deletedTime, expireTime

### 8.4 测试策略

- Mock FileService / RecycleBinService
- 测试删除后 t_file.status=DELETED、t_recycle_bin 存在、MinIO 对象未被删除

---

## 9. 模块 M6 — 团队空间

### 9.1 包结构

```
controller/TeamController.java
controller/TeamFileController.java
entity/Team.java
entity/TeamMember.java
enums/TeamMemberRole.java
enums/TeamStatus.java
mapper/TeamMapper.java
mapper/TeamMemberMapper.java
service/TeamService.java
service/impl/TeamServiceImpl.java
dto/
├── TeamCreateRequest.java
├── TeamUpdateRequest.java
├── TeamListResponse.java
├── TeamDetailResponse.java
├── TeamMemberResponse.java
├── TeamInviteRequest.java
```

### 9.2 新增实体

```java
@Data
public class Team {
    private Long id;
    private String name;
    private Long ownerId;
    private String avatar;
    private String description;
    private TeamStatus status;
    private Long quota;
    private Long usedSpace;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

@Data
public class TeamMember {
    private Long id;
    private Long teamId;
    private Long userId;
    private TeamMemberRole role;  // MEMBER / ADMIN / OWNER
    private Integer status;       // 0-已退出 1-正常
    private LocalDateTime joinedAt;
}
```

### 9.3 新增枚举

```java
public enum TeamMemberRole {
    MEMBER(0),
    ADMIN(10),
    OWNER(20);
    private final int value;
}

public enum TeamStatus {
    DISSOLVED(0),
    NORMAL(1);
    private final int value;
}
```

### 9.4 Mapper 接口

```java
@Mapper
public interface TeamMapper {
    void insert(Team team);
    Team findById(Long id);
    List<Team> findByOwnerId(Long ownerId);
    List<Team> findByMemberId(Long userId);  // 通过 t_team_member 关联
    int update(Team team);
    int deleteById(Long id);  // 逻辑删除
}

@Mapper
public interface TeamMemberMapper {
    void insert(TeamMember member);
    TeamMember findByTeamAndUser(@Param("teamId") Long teamId, @Param("userId") Long userId);
    List<TeamMember> findByTeamId(Long teamId);
    int updateRole(@Param("id") Long id, @Param("role") TeamMemberRole role);
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    int countByTeamId(Long teamId);
}
```

### 9.5 Service 定义

```java
public interface TeamService {
    Team createTeam(Long userId, TeamCreateRequest request);
    Team updateTeam(Long userId, Long teamId, TeamUpdateRequest request);
    void dissolveTeam(Long userId, Long teamId);
    List<TeamListResponse> listMyTeams(Long userId);
    TeamDetailResponse getTeamDetail(Long userId, Long teamId);

    // 成员管理
    void inviteMember(Long userId, Long teamId, TeamInviteRequest request);
    void removeMember(Long userId, Long teamId, Long targetUserId);
    List<TeamMemberResponse> listMembers(Long teamId);
    void leaveTeam(Long userId, Long teamId);

    // 权限校验
    boolean isOwner(Long userId, Long teamId);
    boolean isAdminOrOwner(Long userId, Long teamId);
    boolean isMember(Long userId, Long teamId);
}
```

### 9.6 DTO 定义

```java
@Data
public class TeamCreateRequest {
    @NotBlank @Size(max = 64)
    private String name;
    @Size(max = 512)
    private String description;
}

@Data
public class TeamUpdateRequest {
    @Size(max = 64)
    private String name;
    @Size(max = 255)
    private String avatar;
    @Size(max = 512)
    private String description;
}

@Data
public class TeamListResponse {
    private Long id;
    private String name;
    private String avatar;
    private String description;
    private Integer memberCount;
    private TeamMemberRole myRole;
}

@Data
public class TeamDetailResponse {
    private Long id;
    private String name;
    private String avatar;
    private String description;
    private Long ownerId;
    private String ownerName;
    private Integer memberCount;
    private Long quota;
    private Long usedSpace;
    private TeamMemberRole myRole;
    private LocalDateTime createdAt;
}

@Data
public class TeamMemberResponse {
    private Long id;
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private TeamMemberRole role;
    private LocalDateTime joinedAt;
}

@Data
public class TeamInviteRequest {
    @NotBlank
    private String username;  // 通过用户名邀请
}
```

### 9.7 权限校验逻辑

```java
// TeamService 中核心权限校验
public void checkAdminOrOwner(Long userId, Long teamId) {
    TeamMember member = teamMemberMapper.findByTeamAndUser(teamId, userId);
    if (member == null || member.getStatus() != 1) {
        throw new BusinessException(TEAM_NOT_FOUND, "你不在该团队中");
    }
    if (member.getRole() == TeamMemberRole.MEMBER) {
        throw new BusinessException(FORBIDDEN, "仅管理员可执行此操作");
    }
}
```

### 9.8 团队文件操作

```java
// TeamFileController - 复用 FileController 逻辑
// 设计原则：所有文件操作接口增加一个可选的 teamId 参数
// 当 teamId 不为 null 时，user_id 校验改为 team_member 校验
// 文件记录的 team_id 字段设为对应团队 ID

@RestController
@RequestMapping("/api/teams/{teamId}/files")
public class TeamFileController {

    /** 团队文件列表 */
    @GetMapping
    public Result<PageResponse<FileNodeResponse>> listFiles(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long teamId,
            @RequestParam(required = false, defaultValue = "0") Long parentId,
            PageRequest page);

    /** 创建团队目录 */
    @PostMapping("/directory")
    public Result<Void> createDirectory(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long teamId,
            @Valid @RequestBody DirectoryCreateRequest request);
}
```

注：为减少重复代码，`FileController` 中的大多数操作也可以接受 `teamId` 参数，通过 `isMember()` 校验替代 `userId` 校验。或者在 `TeamFileController` 中委托给 `FileController` 的内部方法。

### 9.9 数据库 DDL

```sql
CREATE TABLE IF NOT EXISTS t_team (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(64)    NOT NULL COMMENT '团队名称',
    owner_id    BIGINT         NOT NULL COMMENT '创建者用户ID',
    avatar      VARCHAR(256)   DEFAULT NULL COMMENT '团队头像',
    description VARCHAR(512)   DEFAULT NULL COMMENT '团队描述',
    quota       BIGINT         NOT NULL DEFAULT 10737418240 COMMENT '团队总配额（默认10GB）',
    used_space  BIGINT         NOT NULL DEFAULT 0 COMMENT '团队已用空间',
    status      TINYINT        NOT NULL DEFAULT 1 COMMENT '0-解散 1-正常',
    created_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='团队空间';

CREATE TABLE IF NOT EXISTS t_team_member (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_id   BIGINT   NOT NULL COMMENT '团队ID',
    user_id   BIGINT   NOT NULL COMMENT '用户ID',
    role      TINYINT  NOT NULL DEFAULT 0 COMMENT '0-成员 10-管理员 20-所有者',
    status    TINYINT  NOT NULL DEFAULT 1 COMMENT '0-已退出 1-正常',
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_team_user (team_id, user_id),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='团队成员';

-- t_file 新增字段
ALTER TABLE t_file ADD COLUMN team_id BIGINT DEFAULT NULL COMMENT '所属团队ID，NULL表示个人文件';
ALTER TABLE t_file ADD INDEX idx_team (team_id, parent_id, status);
```

### 9.10 测试策略

- `TeamServiceTest`: Mock TeamMapper / TeamMemberMapper / FileMapper
  - 测试创建/更新/解散团队
  - 测试成员邀请/移除/退出
  - 测试权限校验（OWNER 可编辑/解散，ADMIN 可邀请，MEMBER 不能移除成员）
- `TeamFileControllerTest`: Mock TeamService / FileService
  - 测试团队文件列表/上传/删除

---

## 10. 模块 M7 — 管理后台

### 10.1 包结构（新增）

```
controller/admin/AdminTeamController.java
```

### 10.2 新增 Controller

```java
@RestController
@RequestMapping("/api/admin/teams")
public class AdminTeamController {

    /** 全局团队列表 */
    @GetMapping
    public Result<List<TeamListResponse>> listTeams();

    /** 强制解散团队 */
    @DeleteMapping("/{id}")
    public Result<Void> dissolveTeam(@PathVariable Long id);
}
```

### 10.3 日志查询增强

```java
// OperationLogService 新增过滤查询
public interface OperationLogService {
    // 已有
    void log(OperationLog operationLog);
    List<OperationLog> listByUserId(Long userId);
    List<OperationLog> listAll();

    // 新增：带过滤条件的分页查询
    PageResponse<OperationLog> listWithFilter(LogFilterRequest request, PageRequest page);
}

@Data
public class LogFilterRequest {
    private Long userId;             // 按用户筛选
    private String operation;        // 按操作类型筛选
    private String targetType;       // 按目标类型筛选
    private LocalDateTime startTime; // 开始时间
    private LocalDateTime endTime;   // 结束时间
}
```

### 10.4 测试策略

- `AdminTeamControllerTest`: Mock TeamService，测试列表和强制解散
- AdminController / AdminUserController / AdminAccountController 已有实现，补充测试

---

## 11. 模块 M8 — 操作审计（已有，变化极小）

### 11.1 现有实现

```java
// OperationType 枚举需要补充的值
public enum OperationType {
    LOGIN, REGISTER, LOGOUT,
    UPLOAD, DOWNLOAD, DELETE, RENAME, MOVE, COPY,
    SHARE, CANCEL_SHARE,
    TEAM_CREATE, TEAM_DISSOLVE, TEAM_INVITE, TEAM_REMOVE, TEAM_LEAVE,
    ADMIN_OPERATION
}

public enum TargetType {
    USER, FILE, SHARE, TEAM
}
```

### 11.2 调用方式

```java
// 各 Controller 中直接注入 OperationLogService，不引入 BaseController 继承体系

operationLogService.log(new OperationLog(
    userId,
    OperationType.UPLOAD.name(),
    TargetType.FILE.name(),
    fileId,
    "上传文件: " + fileName,
    IpUtil.getIp(request),
    request.getHeader("User-Agent")
));
```

---

## 12. 模块 M9 — WebSocket 通信

### 12.1 包结构

```
config/WebSocketConfig.java
handler/UploadProgressHandler.java
handler/PackageProgressHandler.java
interceptor/WebSocketAuthInterceptor.java
dto/ProgressMessage.java
```

### 12.2 Handler 定义

```java
public class UploadProgressHandler extends TextWebSocketHandler {

    private final Map<String, List<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String uploadId = extractUploadId(session.getUri());
        sessions.computeIfAbsent(uploadId, k -> new CopyOnWriteArrayList<>()).add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String uploadId = extractUploadId(session.getUri());
        List<WebSocketSession> sessionList = sessions.get(uploadId);
        if (sessionList != null) {
            sessionList.remove(session);
            if (sessionList.isEmpty()) {
                sessions.remove(uploadId);
            }
        }
    }

    /** 供 UploadService 调用的推送方法 */
    public void sendProgress(String uploadId, ProgressMessage message) {
        List<WebSocketSession> sessionList = sessions.get(uploadId);
        if (sessionList == null) return;
        String payload = objectMapper.writeValueAsString(message);
        for (WebSocketSession session : sessionList) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(payload));
                } catch (Exception e) {
                    log.warn("WebSocket send failed, removing session: {}", session.getId(), e);
                    sessionList.remove(session);
                }
            }
        }
    }
}
```

### 12.3 消息格式

```java
@Data
public class ProgressMessage {
    private String type;          // upload_progress / package_progress
    private String taskId;        // uploadId 或 packageTaskId
    private int current;          // 当前进度（已上传分片数 / 已处理文件数）
    private int total;            // 总量（总分片数 / 总文件数）
    private int percentage;       // 百分比 0-100
    private String status;        // processing / completed / failed
    private String errorMessage;  // 失败时携带
}
```

### 12.4 认证拦截器

```java
public class WebSocketAuthInterceptor implements HandshakeInterceptor {
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        // 从 query 参数获取 token
        String token = UriComponentsBuilder.fromUri(request.getURI())
                .build().getQueryParams().getFirst("token");
        // 校验 token 有效性
        if (token == null || !jwtTokenUtil.validateToken(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        attributes.put("userId", jwtTokenUtil.getUserIdFromToken(token));
        return true;
    }
}
```

### 12.5 测试策略

- Mock WebSocketSession，测试消息推送
- 测试连接建立时的 Token 校验
- 测试断连时清理 session

---

## 13. 前端模块设计

### 13.1 项目结构

```
src/
├── api/                           # API 调用层
│   ├── auth.ts
│   ├── file.ts
│   ├── share.ts
│   ├── team.ts
│   └── admin.ts
├── assets/
├── components/                    # 通用组件
│   ├── FileList.vue
│   ├── DirectoryTree.vue
│   ├── UploadDialog.vue
│   ├── TransferQueue.vue
│   ├── BreadcrumbNav.vue
│   └── ShareDialog.vue
├── composables/                   # 组合式函数
│   ├── useWebSocket.ts
│   └── useUpload.ts
├── layout/
│   ├── MainLayout.vue
│   └── AdminLayout.vue
├── router/
│   └── index.ts
├── stores/
│   ├── userStore.ts
│   ├── fileStore.ts
│   ├── uploadStore.ts
│   ├── shareStore.ts
│   ├── teamStore.ts
│   └── adminStore.ts
├── types/                         # TypeScript 类型定义
│   ├── api.ts
│   ├── file.ts
│   └── team.ts
├── utils/
│   ├── request.ts                 # Axios 封装
│   └── sha256.ts                  # 前端 SHA256 计算
└── views/
    ├── login/
    ├── register/
    ├── forgot-password/
    ├── file/
    ├── share/
    ├── share-access/
    ├── team/
    ├── profile/
    └── admin/
        ├── Dashboard.vue
        ├── UserManage.vue
        ├── FileManage.vue
        ├── ShareManage.vue
        ├── LogManage.vue
        ├── AdminManage.vue
        └── TeamManage.vue
```

### 13.2 API 层定义

```typescript
// src/api/file.ts
export const fileApi = {
    list(params: { parentId: number; teamId?: number; page: number; size: number }) =>
        request.get<PageResponse<FileNode>>('/api/files', { params }),

    createDirectory(data: { parentId: number; name: string; teamId?: number }) =>
        request.post('/api/files/directory', data),

    uploadInit(data: UploadInitRequest) =>
        request.post<UploadInitResponse>('/api/files/upload/init', data),

    uploadChunk(data: FormData) =>
        request.post('/api/files/upload/chunk', data, {
            headers: { 'Content-Type': 'multipart/form-data' }
        }),

    uploadMerge(data: UploadMergeRequest) =>
        request.post<FileNode>('/api/files/upload/merge', data),

    uploadSec(data: UploadSecRequest) =>
        request.post<FileNode>('/api/files/upload/sec', data),

    download(id: number) =>
        request.get(`/api/files/download/${id}`, { responseType: 'blob' }),

    batchDownload(data: { fileIds: number[] }) =>
        request.post('/api/files/download/batch', data),

    rename(id: number, data: { name: string }) =>
        request.put(`/api/files/${id}/rename`, data),

    move(id: number, data: { targetParentId: number }) =>
        request.post(`/api/files/${id}/move`, data),

    copy(id: number, data: { targetParentId: number }) =>
        request.post(`/api/files/${id}/copy`, data),

    delete(id: number) =>
        request.delete(`/api/files/${id}`),

    search(params: { keyword: string; parentId?: number; teamId?: number }) =>
        request.get<PageResponse<FileNode>>('/api/files/search', { params }),

    preview(id: number) =>
        request.get<FilePreviewResponse>(`/api/files/${id}/preview`),

    getTree(params: { parentId?: number; teamId?: number }) =>
        request.get<FileTree[]>('/api/files/tree', { params }),
};
```

### 13.3 Pinia Store 定义

```typescript
// src/stores/fileStore.ts
export const useFileStore = defineStore('file', () => {
    const fileList = ref<FileNode[]>([]);
    const currentDir = ref<FileNode | null>(null);
    const breadcrumb = ref<BreadcrumbItem[]>([]);
    const selectedFiles = ref<FileNode[]>([]);
    const viewMode = ref<'list' | 'grid'>('list');

    async function loadFiles(parentId: number, teamId?: number) {
        const res = await fileApi.list({ parentId, teamId, page: 1, size: 50 });
        fileList.value = res.data.records;
    }

    async function navigateToDir(dir: FileNode) {
        breadcrumb.value.push({ id: dir.id, name: dir.name });
        currentDir.value = dir;
        await loadFiles(dir.id);
    }

    return { fileList, currentDir, breadcrumb, selectedFiles, viewMode, loadFiles, navigateToDir };
});

// src/stores/uploadStore.ts
export const useUploadStore = defineStore('upload', () => {
    const uploadQueue = ref<UploadTask[]>([]);

    function addTask(task: UploadTask) { uploadQueue.value.push(task); }
    function updateProgress(uploadId: string, current: number, total: number) {
        const task = uploadQueue.value.find(t => t.uploadId === uploadId);
        if (task) {
            task.progress = Math.round((current / total) * 100);
        }
    }
    function removeTask(uploadId: string) {
        uploadQueue.value = uploadQueue.value.filter(t => t.uploadId !== uploadId);
    }

    return { uploadQueue, addTask, updateProgress, removeTask };
});

// src/stores/teamStore.ts
export const useTeamStore = defineStore('team', () => {
    const teamList = ref<TeamInfo[]>([]);
    const currentTeam = ref<TeamInfo | null>(null);

    async function loadMyTeams() {
        const res = await teamApi.listMyTeams();
        teamList.value = res.data;
    }

    return { teamList, currentTeam, loadMyTeams };
});
```

### 13.4 组件 Props/Events 约定

```typescript
// FileList.vue
interface FileListProps {
    files: FileNode[];
    viewMode: 'list' | 'grid';
    loading: boolean;
}
interface FileListEmits {
    (e: 'file-click', file: FileNode): void;
    (e: 'file-dblclick', file: FileNode): void;     // 双击进入目录 / 预览文件
    (e: 'selection-change', files: FileNode[]): void;
    (e: 'rename', file: FileNode): void;
    (e: 'delete', file: FileNode): void;
    (e: 'move', file: FileNode): void;
    (e: 'copy', file: FileNode): void;
    (e: 'share', file: FileNode): void;
    (e: 'download', file: FileNode): void;
}

// DirectoryTree.vue
interface DirectoryTreeProps {
    teamId?: number;
}
interface DirectoryTreeEmits {
    (e: 'node-click', node: FileTree): void;  // 切换目录
}

// UploadDialog.vue
interface UploadDialogProps {
    visible: boolean;
    targetDirId: number;
    teamId?: number;
}
interface UploadDialogEmits {
    (e: 'close'): void;
    (e: 'upload-complete', files: FileNode[]): void;
}
```

### 13.5 分片上传前端流程

```typescript
// composables/useUpload.ts
async function uploadFile(file: File, parentId: number, teamId?: number) {
    const fileHash = await computeSHA256(file);              // 1. 计算 hash
    const chunkSize = 5 * 1024 * 1024;                       // 5MB
    const totalChunks = Math.ceil(file.size / chunkSize);

    // 2. 尝试秒传
    try {
        const secResult = await fileApi.uploadSec({
            fileHash, fileName: file.name, fileSize: file.size,
            parentId, teamId
        });
        // 秒传成功
        return secResult.data;
    } catch (e) {
        if (e.code !== 'hash_not_found') throw e;
    }

    // 3. 初始化分片上传
    const initResult = await fileApi.uploadInit({
        fileName: file.name, fileSize: file.size,
        fileHash, parentId, teamId
    });
    const { uploadId } = initResult.data;

    // 4. 建立 WebSocket 连接
    const ws = useWebSocket(`/ws/upload/progress/${uploadId}`);

    // 5. 并发上传分片
    const tasks = [];
    for (let i = 0; i < totalChunks; i++) {
        const start = i * chunkSize;
        const chunk = file.slice(start, start + chunkSize);
        const formData = new FormData();
        formData.append('file', chunk);
        formData.append('uploadId', uploadId);
        formData.append('chunkNumber', String(i + 1));
        formData.append('totalChunks', String(totalChunks));
        tasks.push(fileApi.uploadChunk(formData));
    }
    await Promise.all(tasks);

    // 6. 合并
    const mergeResult = await fileApi.uploadMerge({ uploadId, fileName, parentId, teamId });
    ws.close();
    return mergeResult.data;
}
```

### 13.6 路由设计

```typescript
// router/index.ts
const routes = [
    { path: '/login', component: () => import('@/views/login/LoginPage.vue') },
    { path: '/register', component: () => import('@/views/register/RegisterPage.vue') },
    { path: '/forgot-password', component: () => import('@/views/forgot-password/ForgotPasswordPage.vue') },

    // 需要登录
    {
        path: '/',
        component: MainLayout,
        meta: { requiresAuth: true },
        children: [
            { path: 'files', component: () => import('@/views/file/FilePage.vue') },
            { path: 'shares', component: () => import('@/views/share/SharePage.vue') },
            { path: 'teams', component: () => import('@/views/team/TeamListPage.vue') },
            { path: 'teams/:id', component: () => import('@/views/team/TeamDetailPage.vue') },
            { path: 'teams/:id/files', component: () => import('@/views/file/FilePage.vue') },
            { path: 'profile', component: () => import('@/views/profile/ProfilePage.vue') },
        ]
    },

    // 公开分享页（无需登录）
    { path: '/s/:token', component: () => import('@/views/share-access/ShareAccessPage.vue') },

    // 管理后台
    {
        path: '/admin',
        component: AdminLayout,
        meta: { requiresAuth: true, role: 'admin' },
        children: [
            { path: '', component: () => import('@/views/admin/Dashboard.vue') },
            { path: 'users', component: () => import('@/views/admin/UserManage.vue') },
            { path: 'files', component: () => import('@/views/admin/FileManage.vue') },
            { path: 'shares', component: () => import('@/views/admin/ShareManage.vue') },
            { path: 'logs', component: () => import('@/views/admin/LogManage.vue') },
            { path: 'admins', component: () => import('@/views/admin/AdminManage.vue') },
            { path: 'teams', component: () => import('@/views/admin/TeamManage.vue') },
        ]
    },
];
```

---

## 14. 模块独立测试策略总表

| 模块 | 测试框架 | Mock 策略 | 关键测试场景 |
|------|---------|-----------|-------------|
| M1 认证 | JUnit5 + Mockito | Mock UserService, CaptchaService, EmailService, RedisTemplate | 登录成功/失败/锁定, 注册, 验证码冷却 |
| M2 用户 | JUnit5 + Mockito | Mock UserService, PasswordEncoder | 资料修改, 密码修改 |
| M3 文件 | JUnit5 + Mockito | Mock FileMapper, StorageService, RedisTemplate, FileService | 分片上传完整流程, 秒传, 目录树构建, 搜索, 预览 |
| M4 分享 | JUnit5 + Mockito | Mock ShareService, FileService | 创建/取消/过期访问, 提取码校验 |
| M5 回收站 | JUnit5 + Mockito | Mock FileService, RecycleBinService, StorageService | 删除移入回收站, 恢复(原目录存在/不存在) |
| M6 团队 | JUnit5 + Mockito | Mock TeamMapper, TeamMemberMapper, FileMapper | 创建/解散团队, 角色权限校验, 成员管理 |
| M7 管理后台 | JUnit5 + Mockito | Mock 各 Service | 统计, 日志过滤, 强制解散 |
| M8 审计 | JUnit5 + Mockito | Mock OperationLogMapper | 日志写入, 条件查询 |
| M9 WebSocket | JUnit5 + Spring WS Test | Mock WebSocketSession | 连接/消息推送/断连清理 |
| 前端 | Vitest + Vue Test Utils | Mock Axios + Pinia | 组件渲染, 交互事件, API 调用 |

---

## 15. 附录：开放问题决策建议

基于详细设计的分析，对 HLD 中的开放问题给出建议：

| 问题 | 建议方案 | 理由 |
|------|---------|------|
| 打包下载 | 异步任务模式 | 大文件打包可能耗时较长，异步不阻塞请求，WebSocket 通知完成后下载 |
| 缩略图生成 | 使用 Thumbnailator | 纯 Java 实现，无需外部依赖，适合 MVP |
| 文件搜索 | MVP 仅文件名 LIKE | LIKE 查询实现简单，后续可引入 Elasticsearch |
| 团队空间配额 | 独立配额（t_team 表加 quota 字段/或未来单独配额表） | 共享空间不应计入个人配额 |
| WebSocket 集群 | MVP 不做集群，单实例部署 | 单实例足够家庭使用，后续需要时引入 Redis Pub/Sub 广播 |
