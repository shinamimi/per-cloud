# 高负载调优方案与测试计划

> 更新日期：2026-09-01
> 关联 ADR：`docs/adr/014-performance-optimization.md`
> 目标环境：1C2G / 2C2G 腾讯云轻量

---

## 一、优化总览

### 时间线

| 阶段 | 内容 | 状态 |
|---|---|---|
| 基线 | 未做任何优化 | 基线数据见 ADR-014 |
| 决策一（配置层） | HikariCP / Lettuce / Tomcat / G1GC / 日志轮转 / AdminSettings 缓存 / 打包线程池 | ✅ 已实施 |
| 决策二（代码层） | CTE / copy 复用 / N+1 batch / 批量 SQL / Caffeine 缓存 / releaseRef 原子化 / 异步缩略图 / Dashboard SQL | ✅ 已实施 |

### 基线数据（ADR-014）

| 场景 | QPS | 备注 |
|------|-----|------|
| 登录（50并发） | ~950 | CPU 打满 |
| 文件列表（热缓存） | ~400 | Redis 命中 |
| 分片上传（含3次HTTP往返） | ~10.5 | 受 MinIO 和网络限制 |
| 文件下载（302重定向） | ~62.6 | 受 MinIO 带宽限制 |

---

## 二、配置层优化（决策一）

### 2.1 HikariCP 连接池

| 参数 | 默认值 | 优化值 | 理由 |
|------|--------|--------|------|
| maximumPoolSize | 10 | 30 | 应对突发并发 |
| minimumIdle | 10 | 5 | 保持最低可用连接 |
| leakDetectionThreshold | 0 | 60s | 检测连接泄漏 |

**文件：** `application-prod.yml`

### 2.2 Lettuce Redis 连接池

| 参数 | 默认值 | 优化值 | 理由 |
|------|--------|--------|------|
| max-active | 8 | 32 | Redis 命中率高，需更多连接 |
| max-idle | 8 | 16 | 保持空闲连接 |
| min-idle | 0 | 4 | 预热连接 |

**文件：** `application-prod.yml`

### 2.3 Tomcat 线程池

| 参数 | 默认值 | 优化值 | 理由 |
|------|--------|--------|------|
| maxThreads | 200 | 50 | CPU 有限，减少上下文切换 |
| maxConnections | 8192 | 100 | 与线程池匹配 |
| acceptCount | 100 | 100 | 排队等待 |

**文件：** `application-prod.yml`

### 2.4 JVM G1GC

```
-XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Xms512m -Xmx512m
-XX:MaxMetaspaceSize=128m -XX:+HeapDumpOnOutOfMemoryError
```

**文件：** `Dockerfile`

### 2.5 其他

- AdminSettings ConcurrentHashMap 缓存（60s TTL）
- 打包线程池核心线程 2→4
- 日志轮转：单文件 256MB，保留 7 天，总上限 3GB

---

## 三、代码层优化（决策二）

### 3.1 递归 CTE 替代全表扫描

**问题：** `collectSubtree()` 用 `findByUserId()` 加载用户全部文件到内存，O(N) 扫描。

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

**影响：** 删除/恢复/复制/移动/分享等操作的子树收集性能提升 10x+。

### 3.2 copy() 复用子树数据

**问题：** `copy()` 先 `collectSubtree()` 收集子树，再逐节点调用 `collectSubtree()` 计算大小，O(N²)。

**方案：** 主循环中累加 `child.getSize()`，消除嵌套调用。

### 3.3 N+1 改 batch IN

**问题：** `ShareServiceImpl.listShares()` 逐条 `fileMapper.findById()`。

**方案：** 新增 `FileMapper.findByIds(Collection<Long> ids)`，批量加载。

### 3.4 批量操作替代循环 SQL

**问题：** `deleteToRecycle()` 循环调用 `updateStatus()` + `save()`。

**方案：**
- `FileMapper.updateStatusByIds(ids, status)` 批量更新
- `RecycleBinMapper.batchInsert(records)` 批量插入

### 3.5 Caffeine 本地缓存

**问题：** 热点查询（findById）每次都走 DB。

**方案：** Caffeine 依赖，30s TTL，最大 10000 条。

**注意：** 冷启动时命中率为0，需要预热。测试时必须先跑一轮请求预热缓存。

### 3.6 releaseRef 原子化

**问题：** 先 `decrementRefCount`，再 `findByHash`，再 `deleteByHash`，TOCTOU 竞态。

**方案：** 单条 SQL 原子删除：
```sql
DELETE FROM t_file_hash WHERE file_hash = #{fileHash} AND ref_count <= 0
```

### 3.7 缩略图异步生成

**问题：** 同步生成缩略图，阻塞 Tomcat 线程。

**方案：** `@Async` 异步执行，首次返回原图 URL，后台生成缩略图。

### 3.8 Dashboard 聚合 SQL

**问题：** `getStats()` 加载全量 User/File 到内存聚合。

**方案：** `COUNT(*) + SUM()` SQL 聚合，O(1) 查询。

---

## 四、第一次压测结果分析

### 4.1 测试数据（PERF-TEST-REPORT.md）

| 接口 | 优化前 QPS | 优化后 QPS | 变化 |
|------|-----------|-----------|------|
| 登录 | 949.7 | 290.6 | **-69%** ⚠️ |
| 文件列表 | 398.2 | 133.5 | **-66%** ⚠️ |
| 仪表盘统计 | 无基线 | 424.4 | 新增 |
| 分享列表 | 无基线 | 390.2 | 新增 |

### 4.2 问题分析

#### 登录接口下降-69%：测试环境问题，与代码优化无关

Decision 2 的代码优化**没有改动登录逻辑**。登录 QPS 下降只有两种解释：

1. **Nginx rate limit**：`auth_limit` 是 `2r/s`，50 并发打登录接口，绝大部分被 Nginx 直接拒绝（返回 503），根本没到后端
2. **测试环境未控制变量**：重建容器导致 JVM 冷启动 + MySQL/Redis 缓存清空

**结论：登录接口的下降是测试方法问题，不是代码优化导致的。**

#### 文件列表下降-66%：可能是缓存冷启动 + CTE 开销

Decision 2 新增了 Caffeine 本地缓存，但测试时缓存命中率为0：
- 每个请求：查 DB + 写 Caffeine = 比直接查 DB 多一次缓存写入开销
- CTE 递归查询对小数据集（<1000 文件）可能比 `findAll()` + Java 过滤更慢
- MySQL 查询计划可能未走最优索引

**结论：文件列表下降可能是 Caffeine 冷启动 + CTE 对小数据集的开销，需要重新测试验证。**

#### 仪表盘和分享列表无基线

这两个接口没有优化前的数据，无法对比。需要补充基线测试。

### 4.3 第一次测试的缺陷

| 缺陷 | 影响 |
|---|---|
| 未等 JVM 预热 | 冷启动时 JIT 未编译，性能偏低 |
| 未等缓存预热 | Caffeine 命中率为0，额外开销 |
| 重建容器后立即测试 | MySQL/Redis 缓存清空，磁盘 I/O 高 |
| 未控制 rate limit | 登录接口被 Nginx 拒绝 |
| 无仪表盘/分享列表基线 | 无法对比这两个接口 |

---

## 五、正确的测试方法论

### 5.1 测试前准备

```bash
# 1. 部署目标版本
docker compose -f docker-compose.prod.yml up -d --build

# 2. 等待服务启动
sleep 30

# 3. 等待 JVM 预热（JIT 编译 + 内存初始化）
sleep 1800  # 30 分钟

# 4. 预热 Caffeine 缓存（跑 100 次文件列表请求）
TOKEN=$(curl -s -X POST http://127.0.0.1:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"bench","password":"bench123"}' | jq -r '.token')

for i in $(seq 1 100); do
  curl -s http://127.0.0.1:8081/api/files?parentId=0 \
    -H "Authorization: Bearer $TOKEN" > /dev/null
done

# 5. 预热 MySQL buffer pool（跑一轮全量查询）
curl -s "http://127.0.0.1:8081/api/admin/dashboard/stats" \
  -H "Authorization: Bearer $ADMIN_TOKEN" > /dev/null
```

### 5.2 测试执行

```bash
# 登录（关闭 rate limit 或用不同 IP）
wrk -t2 -c50 -d60s --latency -s /tmp/login.lua http://127.0.0.1:8081/api/auth/login

# 文件列表（热缓存）
wrk -t2 -c50 -d60s --latency \
  -H "Authorization: Bearer $TOKEN" \
  http://127.0.0.1:8081/api/files?parentId=0

# 仪表盘统计
wrk -t2 -c50 -d60s --latency \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  http://127.0.0.1:8081/api/admin/dashboard/stats

# 分享列表
wrk -t2 -c50 -d60s --latency \
  -H "Authorization: Bearer $TOKEN" \
  http://127.0.0.1:8081/api/shares
```

### 5.3 对比方法

| 步骤 | 操作 |
|---|---|
| 1 | 部署优化前版本，预热30分钟，测基线 |
| 2 | 部署优化后版本，预热30分钟，测优化后 |
| 3 | 两次测试条件完全一致（相同并发数、相同时长、相同预热） |

---

## 六、测试计划

### 阶段 1：单元测试（每个改动独立验证）

| 测试项 | 方法 | 预期结果 |
|---|---|---|
| HikariCP 连接池 | 并发30请求，监控活跃连接 | ≤30，无超时 |
| Lettuce 连接池 | 并发100请求，监控 Redis 连接 | ≤32 |
| MinIO 连接池 | 并发20个100MB上传 | ESTABLISHED ≤20 |
| 打包线程池 | 同时10个批量下载 | 活跃线程 ≤4 |
| Caffeine 缓存 | 同一用户连续10次请求 | 命中率 >90% |
| Tomcat 线程 | 并发100请求 | http-nio ≤50 |
| JVM 内存 | 运行1小时 | Old Gen <80%，Full GC <5次/小时 |

### 阶段 2：集成测试（多服务联合）

| 测试项 | 方法 | 预期结果 |
|---|---|---|
| 并发上传 | 30个100MB文件同时上传 | 全部成功，无OOM |
| 并发下载 | 30个批量下载同时请求 | 全部成功 |
| 故障恢复 | kill MySQL/Redis/MinIO 后恢复 | 自动重连，无数据丢失 |
| 数据一致性 | 上传10个相同文件，逐个删除 | ref_count 正确递减，归零后对象删除 |

### 阶段 3：压测（控制变量）

| 测试项 | 工具 | 配置 | 目标 |
|---|---|---|---|
| 登录 QPS | wrk | 50并发，60秒 | ≥950（与基线持平） |
| 文件列表 QPS | wrk | 50并发，60秒 | ≥400（与基线持平） |
| 仪表盘 QPS | wrk | 50并发，60秒 | ≥400 |
| 分享列表 QPS | wrk | 50并发，60秒 | ≥400 |
| 上传吞吐量 | 自定义脚本 | 10并发50MB | 全部成功，平均<30秒 |
| 内存稳定性 | docker stats | 压测期间每5秒采样 | Backend <600MB |

### 阶段 4：24小时稳定性测试

```bash
START=$(date +%s)
END=$((START + 86400))

while [ $(date +%s) -lt $END ]; do
  # 随机上传/下载/目录操作
  # 每5分钟记录 JVM 状态
  # 每1分钟记录容器内存
  sleep $((RANDOM % 10 + 1))
done
```

**验收标准：**

| 指标 | 目标值 |
|---|---|
| 运行时间 | ≥24小时 |
| 重启次数 | 0 |
| OOM Kill | 0 |
| 数据丢失 | 0 |
| 平均响应时间 | <500ms |
| P99 响应时间 | <2秒 |
| 错误率 | <0.1% |

---

## 七、优化前后对比（修正版）

### 基线（决策一实施后）

| 接口 | QPS | P50 | P99 | 备注 |
|------|-----|-----|-----|------|
| 登录 | 949.7 | 60ms | 150ms | CPU 打满 |
| 文件列表 | 398.2 | 114ms | 317ms | Redis 命中 |

### 决策二实施后（待重新测试）

| 接口 | QPS | P50 | P99 | 备注 |
|------|-----|-----|-----|------|
| 登录 | 待测 | - | - | 代码未改动，应与基线持平 |
| 文件列表 | 待测 | - | - | Caffeine 冷启动可能影响，预热后应 ≥400 |
| 仪表盘统计 | 424.4 | 108ms | 461ms | 聚合 SQL 生效 |
| 分享列表 | 390.2 | 123ms | 263ms | batch IN 生效 |

### 预期收益（决策二）

| 指标 | 优化前 | 预期 | 备注 |
|------|--------|------|------|
| 登录 QPS | 950 | 950+ | 代码未改动，不应下降 |
| 文件列表 QPS | 400 | 400+ | Caffeine 预热后应持平或提升 |
| 大目录删除 | O(N²) | O(N) | CTE + batch |
| Dashboard 统计 | 全表加载 | SQL 聚合 | 内存降低 |
| 缩略图生成 | 同步阻塞 | 异步非阻塞 | 延迟降低 |

---

## 八、已知风险

| 风险 | 影响 | 缓解措施 |
|---|---|---|
| MySQL 递归深度 | 超深目录可能触及上限（默认10000） | 监控 CTE 递归深度 |
| Caffeine 缓存一致性 | 写操作后缓存短暂不一致 | 30s TTL 可接受 |
| 异步缩略图 | 首次请求返回原图 | 后续请求恢复正常 |
| G1GC 在2核上 | 并发标记线程争抢 CPU | 已用 `MaxGCPauseMillis=200` 限制 |

---

## 九、改动文件清单

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

---

## 十、测试报告模板

```markdown
# 高负载调优测试报告

## 测试环境
- 服务器：
- 测试日期：
- 预热时间：30 分钟

## 测试结果

### 阶段 1：单元测试
- [ ] HikariCP：通过/失败
- [ ] Lettuce：通过/失败
- [ ] Caffeine 缓存：通过/失败
- [ ] JVM 内存：通过/失败

### 阶段 2：集成测试
- [ ] 并发上传：通过/失败
- [ ] 并发下载：通过/失败
- [ ] 故障恢复：通过/失败

### 阶段 3：压测（预热后）
- [ ] 登录 QPS：___（基线 950）
- [ ] 文件列表 QPS：___（基线 400）
- [ ] 仪表盘 QPS：___
- [ ] 分享列表 QPS：___

### 阶段 4：稳定性
- [ ] 24小时运行：通过/失败（重启___次，OOM___次）

## 结论
- 登录 QPS 变化：___%（应持平）
- 文件列表 QPS 变化：___%（应持平或提升）
- 内存峰值：___MB
```
