# 问题一：项目里文件存在哪？怎么组织的？

> 面试官问法："你项目里文件存在哪？怎么组织的？"——这是文件模块面试的第一必问题。

---

## 1. 一句话回答

文件存在 **MinIO 对象存储**（S3 兼容）的单 bucket `cloud-storage` 中，按前缀分区组织；数据库 `t_file` 表保存元数据（文件名、路径、大小、MinIO 对象路径），`t_file_hash` 表保存秒传索引。

---

## 2. 存储引擎：MinIO（为什么不用磁盘/云 OSS）

### 2.1 选型决策

| 维度 | 服务器磁盘 | MinIO（选择） | 云 OSS |
|------|-----------|-------------|--------|
| 成本 | 最低 | 单容器 100-200MB 内存，2C2G 可跑 | 按量计费，单机项目不划算 |
| 扩展 | 磁盘上限，需手工扩 | 对象存储横向可扩 | 弹性 |
| 迁移 | — | S3 协议，未来换 OSS 协议级直迁 | — |
| 单点风险 | 高（整机盘） | 中（需备份快照兜底） | 低（厂商 SLA） |

**选 MinIO 的核心理由：** 2C2G 单机 + Docker Compose 下，MinIO 用 S3 标准协议，未来换云 OSS 是协议级直迁（`StorageService` 接口即为此留的接缝）。

### 2.2 配置

**MinIO 连接配置**（`MinioProperties.java`，绑定 `minio.*`）：

```yaml
# application-dev.yml
minio:
  endpoint: http://${MINIO_HOST:minio}:9000    # 容器内网地址（业务读写）
  public-url: http://${MINIO_HOST:minio}:9000  # 浏览器可达地址（presigned 签名用）
  bucket: cloud-storage                         # 单 bucket
  auto-create-bucket: true                      # dev 自动建桶
```

```yaml
# application-prod.yml
minio:
  endpoint: ${MINIO_ENDPOINT}
  public-url: ${MINIO_PUBLIC_URL}               # 必须是浏览器可达地址（S3 v4 签名 host）
  bucket: ${MINIO_BUCKET:cloud-storage}
  auto-create-bucket: false                     # prod 关闭自动建桶
```

**文件上传阈值**（`FileProperties.java`，绑定 `file.*`）：

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `file.small-file-threshold` | 5MB | 小于此值不分片，直传 |
| `file.chunk-size` | 10MB | 分片大小 |
| `file.max-size-user` | 512MB | 普通用户单文件上限 |
| `file.max-size-vip` | 2GB | VIP 单文件上限 |
| `file.max-concurrent-user` | 3 | 普通用户并发上传任务数 |
| `file.max-concurrent-vip` | 5 | VIP 并发上传任务数 |
| `file.upload-expire-hours` | 24 | 上传会话 TTL |
| `file.recycle-days` | 30 | 回收站保留天数 |

**Docker Compose（MinIO 容器）**：

```yaml
# docker-compose.prod.yml
minio:
  image: minio/minio:latest
  container_name: cloud-minio
  command: server /data --console-address ":9001"
  ports:
    - "9000:9000"            # API：公开（presigned URL 直连）
    - "127.0.0.1:9001:9001" # Console：仅本地（SSH 隧道访问）
  volumes:
    - minio-data:/data
  mem_limit: 640m
  cpus: 0.8
```

---

## 3. 对象路径组织（前缀分区）

### 3.1 路径生成代码

**`IdUtil.java`**（`backend/src/main/java/com/cloud/backend/utils/IdUtil.java`）：

```java
/** 分片临时对象路径 */
public static String uploadChunkObject(Long userId, String uploadId, int seq) {
    return "uploads/" + userId + "/" + uploadId + "/chunk_" + seq;
}

/** 个人文件对象路径（路径含 fileId，移动不影响对象） */
public static String fileObject(Long userId, Long fileId, String fileName) {
    return "files/" + userId + "/" + fileId + "/" + fileName;
}

/** 团队文件对象路径 */
public static String teamFileObject(Long teamId, Long fileId, String fileName) {
    return "files/team/" + teamId + "/" + fileId + "/" + fileName;
}

/** 缩略图对象路径 */
public static String thumbnailObject(Long userId, Long fileId) {
    return "thumbnails/" + userId + "/" + fileId + ".jpg";
}

/** 打包下载产物对象路径 */
public static String packageObject(String taskId) {
    return "packages/" + taskId + ".zip";
}
```

### 3.2 前缀分区总览

| 用途 | 路径模式 | 示例 |
|------|---------|------|
| **分片临时** | `uploads/{userId}/{uploadId}/chunk_{seq}` | `uploads/42/abc123def/chunk_3` |
| **个人文件** | `files/{userId}/{fileId}/{fileName}` | `files/42/1001/report.pdf` |
| **团队文件** | `files/team/{teamId}/{fileId}/{fileName}` | `files/team/5/1002/design.psd` |
| **缩略图** | `thumbnails/{userId}/{fileId}.jpg` | `thumbnails/42/1001.jpg` |
| **打包产物** | `packages/{taskId}.zip` | `packages/xyz789.zip` |

### 3.3 为什么路径里含 fileId？

`files/{userId}/{fileId}/{fileName}` —— fileId 是数据库主键，对象路径随之固定：

- **移动/重命名只改数据库**，MinIO 对象完全不动（`file-module.md §8.3`）
- 这是"**数据库为路径事实源、存储为内容事实源**"的分离设计
- 路径含 userId 保证用户级隔离

---

## 4. 数据库元数据

### 4.1 t_file 表（文件元数据）

**`sql/init-full.sql`**：

```sql
CREATE TABLE IF NOT EXISTS t_file (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL DEFAULT 0 COMMENT '0-个人空间，>0-团队空间',
    parent_id BIGINT NOT NULL DEFAULT 0,          -- 父目录 ID，0=根目录
    name VARCHAR(255) NOT NULL,                   -- 用户展示名
    path VARCHAR(500) NOT NULL,                   -- 完整路径（如 /documents/report.pdf）
    size BIGINT NOT NULL DEFAULT 0,
    mime_type VARCHAR(100) DEFAULT '',
    extension VARCHAR(20) DEFAULT '',
    file_hash VARCHAR(64) DEFAULT '',             -- SHA256，用于秒传校验
    is_directory TINYINT NOT NULL DEFAULT 0 COMMENT '0-文件 1-目录',
    type TINYINT NOT NULL DEFAULT 0 COMMENT '0-文件 1-目录',
    category TINYINT NOT NULL DEFAULT 5 COMMENT '0-图片 1-文档 2-视频 3-音频 4-压缩包 5-其他',
    object_name VARCHAR(255) DEFAULT '',          -- MinIO 中的对象路径
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0-已删除 1-正常 2-禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_parent (user_id, parent_id, status),
    UNIQUE KEY uk_user_parent_name (user_id, parent_id, name, team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件表';
```

**关键字段说明：**

| 字段 | 作用 | 与存储的关联 |
|------|------|-------------|
| `object_name` | MinIO 对象路径（如 `files/42/1001/report.pdf`） | 目录的 `object_name` 为空 |
| `file_hash` | 文件 SHA256 | 用于秒传匹配 `t_file_hash` |
| `parent_id` | 父目录 ID | 形成树结构，0=根目录 |
| `team_id` | 0=个人空间，>0=团队空间 | 区分个人/团队文件 |
| `type` | 0=文件，1=目录 | 统一表存储文件和目录 |
| `status` | 0=已删除，1=正常，2=禁用 | 逻辑删除 + 管理员禁用 |

### 4.2 t_file_hash 表（秒传索引）

**`sql/init-full.sql`**：

```sql
CREATE TABLE IF NOT EXISTS t_file_hash (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_hash VARCHAR(64) NOT NULL COMMENT '文件 SHA256',
    object_name VARCHAR(255) NOT NULL COMMENT '共享对象路径',
    size BIGINT NOT NULL DEFAULT 0,
    mime_type VARCHAR(100) DEFAULT '',
    ref_count INT NOT NULL DEFAULT 0 COMMENT '全局引用计数，归零物理删除对象',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_hash (file_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒传索引表';
```

**引用计数逻辑：**

| 操作 | ref_count 变化 | 说明 |
|------|---------------|------|
| 首次上传 | 插入，ref_count=1 | 新建索引 |
| 秒传命中 | ref_count += 1 | 新文件记录指向同一 object_name |
| 回收站到期物理删除 | ref_count -= 1 | 归零才删 MinIO 对象 |

---

## 5. 存储抽象层

### 5.1 StorageService 接口

**`backend/src/main/java/com/cloud/backend/service/file/StorageService.java`**：

```java
public interface StorageService {
    String upload(String objectName, InputStream inputStream, long size, String contentType);
    InputStream download(String objectName);
    void delete(String objectName);
    String generateDownloadUrl(String objectName, int expiryInMinutes);  // presigned URL
    void copyObject(String sourceObjectName, String destObjectName);    // 秒传同桶复制
    boolean objectExists(String objectName);
    ObjectInfo getObjectInfo(String objectName);
    java.util.List<String> listObjects(String prefix);                   // 定时清理用
    boolean bucketExists(String bucketName);
    void createBucket(String bucketName);

    record ObjectInfo(long size, String contentType, String etag) {}
}
```

### 5.2 MinioConfig：双 Client 设计

**`backend/src/main/java/com/cloud/backend/config/MinioConfig.java`**：

```java
@Configuration
public class MinioConfig {

    /** 业务读写用：用内网 endpoint */
    @Bean
    public MinioClient minioClient(MinioProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }

    /**
     * presigned URL 专用：用 public-url 做 endpoint。
     * S3 v4 签名把 host 值签进签名（SignedHeaders=host），
     * 若用内网 endpoint 签名，浏览器访问公网地址时签名校验必失败（403）。
     */
    @Bean
    public MinioClient presignMinioClient(MinioProperties properties) {
        String endpoint = properties.getPublicUrl();
        if (endpoint == null || endpoint.isBlank()) {
            endpoint = properties.getEndpoint();
        }
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }
}
```

**为什么两个 Client？** S3 v4 签名把 `host` 作为 SignedHeaders 参与签名计算，内网 `http://minio:9000` 与浏览器访问的公网地址不一致，服务端验签必失败（403）。这是真实踩过的坑（`docs/ARCHITECTURE-TECHNOLOGY-MAP.md` 记录过 `MINIO_ENDPOINT` 用错导致的故障）。

### 5.3 调用方汇总

| Service | 用途 |
|---------|------|
| `UploadServiceImpl` | 写分片（`uploads/...`）、写合并文件（`files/...`）、校验分片存在性 |
| `DownloadServiceImpl` | 生成 presigned 下载 URL、流式读取对象打包 |
| `PreviewServiceImpl` | 生成 presigned URL、下载原图生成缩略图（`thumbnails/...`） |
| `RecycleBinServiceImpl` | 物理删除 MinIO 对象 |
| `AdminFileServiceImpl` | 管理员下载/预览 |
| `FileCleanupTask` | 列举 `uploads/` 前缀清理孤儿分片 |

---

## 6. 上传/下载链路中的存储流转

### 6.1 上传链路（分片 → MinIO）

```
浏览器 → POST /api/files/upload/init → 后端校验 → Redis 写元数据
       → POST /api/files/upload/chunk ×N → MinIO: uploads/{userId}/{uploadId}/chunk_{seq}
       → POST /api/files/upload/merge → 组合分片 → MinIO: files/{userId}/{fileId}/{fileName}
                                              → 注册 t_file_hash 秒传索引
```

### 6.2 下载链路（presigned 302 直连）

```
浏览器 → GET /api/files/{id}/download → 后端归属校验
       → 生成 presigned URL（10 分钟有效期）
       → 302 重定向 → 浏览器直连 MinIO 获取文件流
```

**关键点：** 后端只做"校验归属 → 生成签名 → 返回 Location"，字节流完全不经过应用。

### 6.3 秒传链路（零复制）

```
浏览器算 SHA256 → POST /api/files/upload/sec
  → t_file_hash 命中 → INSERT t_file（引用共享 object_name）+ ref_count += 1
  → 未命中 → 走 init/chunk/merge 正常分片链路
```

---

## 7. 临时对象清理

### 7.1 分片临时对象

- 上传会话 TTL 24 小时（Redis `upload:meta:{uploadId}`）
- 每日 04:00 定时任务扫描 `uploads/` 前缀，Redis 元数据不存在的孤儿分片删除

### 7.2 打包产物

- 有效期 24 小时，定时任务清理 `packages/` 前缀

### 7.3 清理代码

**`FileCleanupTask.java`**（`backend/src/main/java/com/cloud/backend/config/FileCleanupTask.java`）：

```java
@Scheduled(cron = "0 0 3 * * ?")   // 每日 03:00
public void cleanupExpiredData() {
    recycleBinService.purgeExpired();           // 回收站到期(30天)物理清理
    downloadService.cleanupExpiredPackages();   // 打包产物清理
}

@Scheduled(cron = "0 0 4 * * ?")   // 每日 04:00
public void cleanupStaleUploads() {
    // 扫描 uploads/ 前缀，Redis 元数据已不存在的孤儿分片 → 删除
}
```

---

## 8. 面试回答模板

**完整回答（背诵模板）：**

> 项目文件存在 MinIO 对象存储，单 bucket `cloud-storage`，按前缀分区组织：
> - 分片临时 `uploads/{userId}/{uploadId}/chunk_{seq}`
> - 个人文件 `files/{userId}/{fileId}/{fileName}`
> - 团队文件 `files/team/{teamId}/{fileId}/{fileName}`
> - 缩略图 `thumbnails/{userId}/{fileId}.jpg`
> - 打包产物 `packages/{taskId}.zip`
>
> 数据库 `t_file` 表保存元数据（文件名、路径、大小、MinIO 对象路径 `object_name`），`t_file_hash` 表保存秒传索引（SHA256 → 共享对象路径，引用计数归零才物理删除）。
>
> 选 MinIO 而不是磁盘：容量扩展、备份快照、S3 协议保证未来换 OSS 是协议级直迁。选单 bucket 而不是多 bucket：秒传 `copyObject` 同桶复制最简单。
>
> 路径含 fileId 的设计：移动/重命名只改数据库，MinIO 对象完全不动——"数据库为路径事实源、存储为内容事实源"的分离。

---

## 9. 相关文件索引

| 类别 | 文件路径 |
|------|---------|
| 存储接口 | `backend/src/main/java/com/cloud/backend/service/file/StorageService.java` |
| 存储实现 | `backend/src/main/java/com/cloud/backend/service/file/impl/StorageServiceImpl.java` |
| MinIO 配置 | `backend/src/main/java/com/cloud/backend/config/MinioProperties.java` |
| MinIO Bean | `backend/src/main/java/com/cloud/backend/config/MinioConfig.java` |
| 文件配置 | `backend/src/main/java/com/cloud/backend/config/FileProperties.java` |
| 路径生成 | `backend/src/main/java/com/cloud/backend/utils/IdUtil.java` |
| File 实体 | `backend/src/main/java/com/cloud/backend/entity/File.java` |
| FileHash 实体 | `backend/src/main/java/com/cloud/backend/entity/FileHash.java` |
| SQL 建表 | `sql/init-full.sql` |
| 上传服务 | `backend/src/main/java/com/cloud/backend/service/file/impl/UploadServiceImpl.java` |
| 下载服务 | `backend/src/main/java/com/cloud/backend/service/file/impl/DownloadServiceImpl.java` |
| 清理任务 | `backend/src/main/java/com/cloud/backend/config/FileCleanupTask.java` |
| ADR: MinIO 桶 | `docs/adr/001-minio-bucket.md` |
| ADR: presigned | `docs/adr/002-presigned-url.md` |
| ADR: 秒传 | `docs/adr/003-sec-upload.md` |
| 文件模块设计 | `docs/file-module.md` |
