# 代码层优化方案

> 更新日期：2026-08-31
> 目标：解决应用层性能瓶颈，从 QPS 400→1000+

---

## 一、问题总览

基础设施配置优化解决的是"路宽不够"（连接池/线程池），代码优化解决的是"路上堵车"（N+1查询/全表扫描/循环SQL）。

### 性能瓶颈 Top 6

| # | 瓶颈 | 影响范围 | 严重程度 |
|---|------|---------|---------|
| 1 | `collectSubtree()` 全表扫描用户所有文件 | 文件删除/复制/移动/分享/目录树 | 🔴 致命 |
| 2 | `listShares()`/`listMyTeams()` N+1 查询 | 分享列表/团队列表 | 🟠 严重 |
| 3 | 循环单条 SQL（delete/recycle/snapshot） | 目录操作/分享创建 | 🟠 严重 |
| 4 | File/User/Share findById 无缓存 | 全局热点查询 | 🟡 中等 |
| 5 | 缩略图生成同步阻塞 | 图片预览 | 🟡 中等 |
| 6 | Dashboard findAll 全表加载 | 管理仪表盘 | 🟡 中等 |

---

## 二、优化方案

### 优化 1：递归 CTE 替代全表扫描

**问题：** `collectSubtree()` 调用 `fileMapper.findByUserId(userId)` 加载用户全部文件到内存，然后在 Java 中按 parentId 分组。1 万文件用户每次操作扫描 1 万行。

**方案：** MySQL 8 递归 CTE，只加载目标子树。

**改动文件：**
- `FileMapper.xml` — 新增 `findSubtree` 查询
- `FileServiceImpl.java` — `collectSubtree()` 改用递归查询
- `DownloadServiceImpl.java` — `collectFiles()` 同理
- `ShareServiceImpl.java` — `snapshotSubtree()` 同理

**新 SQL：**
```xml
<!-- 递归查询子树 -->
<select id="findSubtree" resultMap="fileResult">
    WITH RECURSIVE subtree AS (
        SELECT * FROM t_file
        WHERE id = #{rootId} AND user_id = #{userId} AND team_id = 0 AND status != 0
        UNION ALL
        SELECT f.* FROM t_file f
        INNER JOIN subtree s ON f.parent_id = s.id
        WHERE f.user_id = #{userId} AND f.team_id = 0 AND f.status != 0
    )
    SELECT * FROM subtree
</select>
```

**改动文件方法签名：**
```java
// FileServiceImpl.java
- private List<File> collectSubtree(Long userId, Long fileId)
+ private List<File> collectSubtree(Long userId, Long fileId) {
+     return fileMapper.findSubtree(fileId, userId);
+ }
```

**预期收益：** 万级文件用户，操作从 O(N)→O(子树大小)，通常 10-50 行 vs 1 万行。

---

### 优化 2：copy() 复用子树数据

**问题：** `FileServiceImpl.copy()` 调用 `collectSubtree()` 3 次，每次都执行全表扫描。

**方案：** 收集一次，复用结果。

**改动文件：** `FileServiceImpl.java`

**当前代码（简化）：**
```java
// 第一次：收集子节点
List<File> children = collectSubtree(userId, fileId);
for (File child : children) { copyNode(...); }

// 第二次：计算额外空间（对新创建的副本再查一次）
long extraSize = collectSubtree(userId, copied.getId()).stream()
    .mapToLong(File::getSize).sum();
```

**改为：**
```java
List<File> children = collectSubtree(userId, fileId);
for (File child : children) { copyNode(...); }

// 复用 children 计算空间
long extraSize = children.stream().mapToLong(File::getSize).sum();
```

**预期收益：** copy 操作从 3 次全表扫描降为 1 次。

---

### 优化 3：N+1 查询改 Batch IN

**问题 3a：** `ShareServiceImpl.listShares()` 循环内逐条 `fileMapper.findById()`

```java
// 当前：N+1
return shareMapper.findByUserId(userId).stream().map(share -> {
    File file = fileMapper.findById(share.getFileId()); // 每次一条
    ...
}).toList();
```

**改为：**
```java
// 批量加载
List<Share> shares = shareMapper.findByUserId(userId);
Set<Long> fileIds = shares.stream().map(Share::getFileId).collect(Collectors.toSet());
Map<Long, File> fileMap = fileMapper.findByIds(fileIds).stream()
    .collect(Collectors.toMap(File::getId, f -> f));

return shares.stream().map(share -> {
    File file = fileMap.get(share.getFileId());
    ...
}).toList();
```

**需新增 Mapper 方法：**
```java
// FileMapper.java
List<File> findByIds(@Param("ids") Collection<Long> ids);
```

```xml
<!-- FileMapper.xml -->
<select id="findByIds" resultMap="fileResult">
    SELECT * FROM t_file WHERE id IN
    <foreach collection="ids" item="id" open="(" separator="," close=")">
        #{id}
    </foreach>
</select>
```

**问题 3b：** `TeamServiceImpl.listMyTeams()` 循环内逐条查 Owner + Count + Role

**改为：** 批量查 owner + 批量 count

```java
// 批量加载 owner
Set<Long> ownerIds = teams.stream().map(Team::getOwnerId).collect(Collectors.toSet());
Map<Long, User> ownerMap = userMapper.findByIds(ownerIds).stream()
    .collect(Collectors.toMap(User::getId, u -> u));

// 批量 count 成员数
Map<Long, Integer> countMap = teamMemberMapper.countByTeamIds(teamIds).stream()
    .collect(Collectors.toMap(TeamMemberCount::getTeamId, TeamMemberCount::getCount));
```

**需新增 Mapper 方法：**
```java
// TeamMemberMapper.java
List<TeamMemberCount> countByTeamIds(@Param("teamIds") Collection<Long> teamIds);
```

**预期收益：** 分享/团队列表从 N 次查询降为 2-3 次。

---

### 优化 4：批量操作替代循环单条 SQL

**问题 4a：** `deleteToRecycle()` 循环内逐条 UPDATE + INSERT

```java
// 当前：100 文件 = 100 UPDATE + 100 INSERT
for (File node : nodes) {
    fileMapper.updateStatus(node.getId(), FileStatus.DELETED.getValue());
    recycleBinService.save(recycleBin);
}
```

**改为：**
```java
// 批量 UPDATE（FileMapper 已有 updateStatusByIds）
List<Long> ids = nodes.stream().map(File::getId).toList();
fileMapper.updateStatusByIds(ids, FileStatus.DELETED.getValue());

// 批量 INSERT（新增 batchInsert）
recycleBinMapper.batchInsert(recycleBinRecords);
```

**需新增 Mapper 方法：**
```xml
<!-- RecycleBinMapper.xml -->
<insert id="batchInsert">
    INSERT INTO t_recycle_bin (file_id, user_id, original_name, file_size,
        deleted_by, expire_time, created_at)
    VALUES
    <foreach collection="records" item="r" separator=",">
        (#{r.fileId}, #{r.userId}, #{r.originalName}, #{r.fileSize},
         #{r.deletedBy}, #{r.expireTime}, #{r.createdAt})
    </foreach>
</insert>
```

**问题 4b：** `ShareServiceImpl.snapshotSubtree()` 逐条 INSERT（ShareFileMapper 已有 batchInsert）

**改为：** 收集所有节点后调用 `shareFileMapper.batchInsert(nodes)`

**预期收益：** 100 文件目录操作从 200 条 SQL 降为 2 条。

---

### 优化 5：Caffeine 本地缓存

**问题：** File/User/Share/Settings 的 `findById` 每次请求重复查询。

**方案：** 引入 Caffeine 本地缓存，30-60s TTL。

**新增依赖（pom.xml）：**
```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

**新增配置类：**
```java
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofSeconds(30)));
        return manager;
    }
}
```

**改动文件及注解：**

| 文件 | 方法 | 缓存 key | TTL |
|------|------|----------|-----|
| `FileServiceImpl` | `getOwnedFile()` | `file:{userId}:{fileId}` | 30s |
| `UserMapper` | `findById()` | `user:{id}` | 60s |
| `ShareMapper` | `findByToken()` | `share:{token}` | 300s |
| `DisabledObjectMapper` | `countBlocked()` | `disabled:{hash}:{userId}` | 30s |

**缓存失效策略：**
- File：update/delete/copy 时 evict `file:{userId}:*`
- User：update 时 evict `user:{id}`
- Share：create/cancel 时 evict `share:{token}`

**预期收益：** 热点查询 90%+ 命中率，DB 查询减少 50%+。

---

### 优化 6：缩略图异步生成

**问题：** `PreviewServiceImpl.generateThumbnail()` 同步加载大图+缩写+上传 MinIO，阻塞 Tomcat 线程数秒。

**方案：** 异步生成，首次请求返回原图 URL，生成完成后返回缩略图。

**改动文件：** `PreviewServiceImpl.java`

```java
@Async("thumbnailExecutor")
public void generateThumbnailAsync(Long userId, Long fileId, String objectName) {
    // 生成缩略图并上传 MinIO
    // 完成后更新 t_file.thumbnail_object_name
}

public String getThumbnailUrl(Long userId, Long fileId) {
    File file = fileService.getOwnedFile(userId, fileId);
    if (file.getThumbnailObjectName() != null) {
        return storageService.getPresignedUrl(file.getThumbnailObjectName());
    }
    // 触发异步生成，返回原图 URL
    generateThumbnailAsync(userId, fileId, file.getObjectName());
    return storageService.getPresignedUrl(file.getObjectName());
}
```

**新增线程池配置：**
```java
@Bean("thumbnailExecutor")
public Executor thumbnailExecutor() {
    return Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "thumbnail-gen");
        t.setDaemon(true);
        return t;
    });
}
```

**预期收益：** 图片预览请求不再阻塞 Tomcat 线程。

---

### 优化 7：Dashboard 聚合 SQL

**问题：** `DashboardServiceImpl.getStats()` 调用 `userMapper.findAll()` + `fileMapper.findAll()` 加载全部数据到内存计数。

**改为：**
```sql
-- 用户统计
SELECT COUNT(*) as user_count,
       SUM(CASE WHEN is_vip = 1 THEN 1 ELSE 0 END) as vip_count
FROM t_user WHERE status = 2;

-- 文件统计
SELECT COUNT(*) as file_count,
       SUM(size) as total_size
FROM t_file WHERE is_directory = 0 AND status = 1;
```

**需新增 Mapper 方法：**
```java
// UserMapper.java
Map<String, Object> getStats();

// FileMapper.java
Map<String, Object> getStats(@Param("userId") Long userId);
```

**预期收益：** 仪表盘从 O(N) 内存降为 O(1) 单条 SQL。

---

### 优化 8：releaseRef() 原子化

**问题：** `FileHashServiceImpl.releaseRef()` 在 decrement 和 findByHash 之间存在 TOCTOU 竞态。

**当前代码：**
```java
int updated = fileHashMapper.decrementRefCount(fileHash);
FileHash after = fileHashMapper.findByHash(fileHash);
if (after == null || after.getRefCount() <= 0) {
    fileHashMapper.deleteByHash(fileHash);
}
```

**改为：**
```xml
<!-- 单条原子 DELETE -->
<delete id="deleteIfNoRef">
    DELETE FROM t_file_hash WHERE file_hash = #{fileHash} AND ref_count <= 0
</delete>
```

```java
// FileHashServiceImpl.java
public boolean releaseRef(String fileHash) {
    fileHashMapper.decrementRefCount(fileHash);
    return fileHashMapper.deleteIfNoRef(fileHash) > 0;
}
```

**预期收益：** 消除并发删除竞态条件。

---

## 三、实施计划

### 阶段 1：高收益低风险（1-2 天）

| 序号 | 优化 | 改动量 | 风险 |
|------|------|--------|------|
| 1 | 递归 CTE 替代全表扫描 | 3 文件 | 低 |
| 2 | copy() 复用子树数据 | 1 文件 | 低 |
| 3 | N+1 改 batch IN | 4 文件 | 低 |
| 4 | Dashboard 聚合 SQL | 2 文件 | 低 |

### 阶段 2：中等收益（2-3 天）

| 序号 | 优化 | 改动量 | 风险 |
|------|------|--------|------|
| 5 | 批量操作替代循环 SQL | 4 文件 | 中 |
| 6 | Caffeine 本地缓存 | 6 文件 | 中 |
| 7 | releaseRef 原子化 | 2 文件 | 低 |

### 阶段 3：异步优化（1 天）

| 序号 | 优化 | 改动量 | 风险 |
|------|------|--------|------|
| 8 | 缩略图异步生成 | 2 文件 | 中 |

---

## 四、验证方案

### 单元测试

每个优化项需验证：
1. 功能正确性：原有测试通过
2. 性能对比：优化前后 QPS 对比

### 压测场景

| 场景 | 指标 | 目标 |
|------|------|------|
| 万级文件用户目录操作 | 响应时间 | <500ms（优化前可能 >5s） |
| 50 个分享的用户查看分享列表 | P99 | <200ms |
| 100 文件目录删除到回收站 | 总耗时 | <2s（优化前可能 >10s） |
| 并发 50 用户文件列表 | QPS | >1000（优化前 400） |

### 监控指标

```bash
# 监控 DB 查询次数
docker exec cloud-mysql mysql -uroot -p$MYSQL_ROOT_PASSWORD cloud \
  -e "SHOW STATUS LIKE 'Queries';"

# 监控 HikariCP 活跃连接
grep "HikariPool" /app/logs/application.log | tail -5
```

---

## 五、简历写法

> 针对万级文件用户操作慢的问题，通过代码审查定位 6 处全表扫描和 N+1 查询瓶颈：用 MySQL 8 递归 CTE 替代 `collectSubtree()` 全表加载（万级文件从 O(N)→O(子树)），将目录删除/回收站的循环单条 SQL 改为批量操作（100 文件目录从 200 条 SQL→2 条），为 File/User/Share 引入 Caffeine 本地缓存（30s TTL，热点查询 90%+ 命中率），修复 Share 快照逐条 INSERT 为批量插入，缩略图生成改为异步非阻塞。预计可将文件列表 QPS 从 400 提升至 1000+，目录操作响应时间从秒级降至毫秒级。
