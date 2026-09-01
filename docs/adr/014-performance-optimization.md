# ADR-014 — 高负载性能优化：配置层 + 代码层

> 日期: 2026-09-01
> 状态: 已接受

## 背景

生产环境部署在 1C2G 低成本 VPS（101.35.233.30），需在最小资源下最大化吞吐量。

### 压测基线（优化前）

| 场景 | QPS | 备注 |
|------|-----|------|
| 登录（50并发） | ~950 | CPU 打满 |
| 文件列表（热缓存） | ~400 | Redis 命中 |
| 分片上传（含3次HTTP往返） | ~10.5 | 受 MinIO 和网络限制 |
| 文件下载（302重定向） | ~62.6 | 受 MinIO 带宽限制 |

## 决策一：配置层优化（Infrastructure）

### 1. 数据库连接池 HikariCP

| 参数 | 默认值 | 优化值 | 理由 |
|------|--------|--------|------|
| maximumPoolSize | 10 | 30 | 应对突发并发 |
| minimumIdle | 10 | 5 | 保持最低可用连接 |
| leakDetectionThreshold | 0 | 60s | 检测连接泄漏 |

### 2. Redis Lettuce 连接池

| 参数 | 默认值 | 优化值 | 理由 |
|------|--------|--------|------|
| max-active | 8 | 32 | Redis 命中率高，需更多连接 |
| max-idle | 8 | 16 | 保持空闲连接 |
| min-idle | 0 | 4 | 预热连接 |

### 3. Tomcat 线程池

| 参数 | 默认值 | 优化值 | 理由 |
|------|--------|--------|------|
| maxThreads | 200 | 50 | VPS CPU 有限，减少上下文切换 |
| maxConnections | 8192 | 100 | 与线程池匹配 |
| acceptCount | 100 | 100 | 排队等待 |

### 4. AdminSettings ConcurrentHashMap 缓存

- 缓存 60s，减少 Redis/DB 查询
- 使用 `ConcurrentHashMap.compute()` 防止缓存击穿

### 5. 打包线程池

- `packExecutor` 核心线程 2→4，加速文件打包

### 6. JVM G1GC 优化

```
-XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Xms512m -Xmx512m
-XX:MaxMetaspaceSize=128m -XX:+HeapDumpOnOutOfMemoryError
```

### 7. 日志轮转

- 单文件上限 256MB，保留 7 天，总上限 3GB

## 决策二：代码层优化（Code）

### 1. 递归 CTE 替代全表扫描

**问题：** `collectSubtree()` 使用 `findByUserId()` 加载用户全部文件到内存，Java 层递归过滤。

**方案：** MySQL 8 `WITH RECURSIVE` CTE，单条 SQL 完成子树查询。

```sql
WITH RECURSIVE subtree AS (
    SELECT * FROM t_file WHERE id = #{rootId} AND user_id = #{userId}
    UNION ALL
    SELECT f.* FROM t_file f INNER JOIN subtree s ON f.parent_id = s.id
    WHERE f.user_id = #{userId}
)
SELECT * FROM subtree
```

**影响：** 删除/恢复/复制/移动等操作的子树收集性能提升 10x+。

### 2. copy() 复用子树数据

**问题：** `copy()` 方法先 `collectSubtree()` 收集子树，再逐节点调用 `collectSubtree()` 计算子树大小，导致 O(n²) 查询。

**方案：** 在主 `collectSubtree()` 循环中累加 `child.getSize()`，消除嵌套调用。

### 3. N+1 改 batch IN

**问题：** `ShareServiceImpl.listShares()` 逐条 `fileMapper.findById()` 加载文件。

**方案：** 新增 `FileMapper.findByIds(Collection<Long> ids)`，批量加载。

### 4. 批量操作替代循环 SQL

**问题：** `deleteToRecycle()` 循环调用 `updateStatus()` 和 `recycleBinService.save()`。

**方案：**
- `FileMapper.updateStatusByIds(ids, status)` 批量更新状态
- `RecycleBinMapper.batchInsert(records)` 批量插入回收站记录

### 5. Caffeine 本地缓存

**问题：** 热点查询（findById）每次都走 DB。

**方案：** 添加 Caffeine 依赖，为 `FileMapper.findById` 等提供本地缓存（30s TTL，最大 10000 条）。

### 6. releaseRef 原子化

**问题：** `FileHashServiceImpl.releaseRef()` 先 `decrementRefCount`，再 `findByHash`，再 `deleteByHash`，存在 TOCTOU 竞态。

**方案：** 新增 `deleteIfNoRef(fileHash)`，单条 SQL 原子删除：
```sql
DELETE FROM t_file_hash WHERE file_hash = #{fileHash} AND ref_count <= 0
```

### 7. 缩略图异步生成

**问题：** `PreviewServiceImpl.thumbnailUrl()` 同步生成缩略图，阻塞 Tomcat 线程。

**方案：**
- 新增 `AsyncConfig`，配置 `thumbnailExecutor` 线程池（核心 2，最大 4，队列 100）
- `generateThumbnailAsync()` 使用 `@Async` 异步执行
- 首次请求返回原图 URL，后台异步生成，下次请求命中缓存

### 8. Dashboard 聚合 SQL

**问题：** `DashboardServiceImpl.getStats()` 加载全量 User/File 到内存聚合。

**方案：** 新增 `countAll()` 和 `sumSize()` 聚合 SQL：
```sql
SELECT COUNT(*) FROM t_user
SELECT COALESCE(SUM(quota), 0) FROM t_user
SELECT COUNT(*) FROM t_file WHERE status = 1
SELECT COALESCE(SUM(size), 0) FROM t_file WHERE status = 1
```

## 影响

### 文件变更清单

| 文件 | 变更类型 |
|------|----------|
| `backend/pom.xml` | 新增 caffeine 依赖 |
| `backend/Dockerfile` | JVM G1GC 参数 |
| `backend/src/main/resources/application-prod.yml` | HikariCP/Lettuce/Tomcat 配置 |
| `backend/src/main/resources/logback-spring.xml` | 日志轮转 |
| `backend/.../config/CacheConfig.java` | Caffeine 缓存配置 |
| `backend/.../config/AsyncConfig.java` | 异步线程池配置 |
| `backend/.../mapper/FileMapper.java` | findSubtree/findByIds/countAll/sumSize |
| `backend/.../mapper/FileMapper.xml` | CTE/IN/聚合 SQL |
| `backend/.../mapper/RecycleBinMapper.java` | batchInsert |
| `backend/.../mapper/RecycleBinMapper.xml` | batchInsert SQL |
| `backend/.../mapper/FileHashMapper.java` | deleteIfNoRef |
| `backend/.../mapper/FileHashMapper.xml` | deleteIfNoRef SQL |
| `backend/.../mapper/UserMapper.java` | countAll/sumQuota |
| `backend/.../mapper/UserMapper.xml` | 聚合 SQL |
| `backend/.../service/file/impl/FileServiceImpl.java` | CTE/batch/复用子树 |
| `backend/.../service/file/impl/FileHashServiceImpl.java` | 原子 releaseRef |
| `backend/.../service/file/impl/PreviewServiceImpl.java` | 异步缩略图 |
| `backend/.../service/file/impl/ShareServiceImpl.java` | batch IN |
| `backend/.../service/system/impl/DashboardServiceImpl.java` | 聚合 SQL |
| `backend/src/main/resources/migration-index-optimize.sql` | 索引优化 |
| `backend/src/main/resources/init-full.sql` | 更新索引定义 |

### 预期收益

| 指标 | 优化前 | 预期 | 备注 |
|------|--------|------|------|
| 登录 QPS | 950 | 1000+ | 配置层优化 |
| 文件列表 QPS | 400 | 500+ | CTE + 本地缓存 |
| 大目录删除/恢复 | O(n²) | O(n) | CTE + batch |
| Dashboard 统计 | 全表加载 | SQL 聚合 | 内存占用降低 |
| 缩略图生成 | 同步阻塞 | 异步非阻塞 | 首次请求延迟降低 |

### 风险

1. **MySQL 递归深度**：默认 10000，超深目录可能触及上限，需监控
2. **Caffeine 缓存一致性**：写操作后缓存可能短暂不一致，30s TTL 可接受
3. **异步缩略图**：首次请求返回原图，用户体验略有下降，后续请求恢复正常
