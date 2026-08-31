# 优化清单总览

> 更新日期：2026-08-31
> 目标环境：2C2G 腾讯云轻量，100+ 并发用户

---

## 优化分层

```
┌─────────────────────────────────────────────┐
│  第一层：基础设施配置优化（OPTIMIZATION-INFRA） │  ← 已完成 ✅
│  连接池 / 线程池 / GC / 日志 / 缓存          │
├─────────────────────────────────────────────┤
│  第二层：代码层优化（OPTIMIZATION-CODE）       │  ← 待实施 ❌
│  N+1查询 / 批量操作 / 缓存 / 异步 / 内存     │
├─────────────────────────────────────────────┤
│  第三层：数据库索引优化（OPTIMIZATION-INDEX）  │  ← 部分完成 🔶
│  索引补全 / 冗余删除 / 深分页                 │
└─────────────────────────────────────────────┘
```

---

## 第一层：基础设施配置（已完成 ✅）

详见 `OPTIMIZATION-INFRA.md`

| # | 改动 | 状态 |
|---|------|------|
| 1 | HikariCP 连接池 10→30 | ✅ 已部署 |
| 2 | Lettuce Redis 连接池 8→32 | ✅ 已部署 |
| 3 | Tomcat 线程池 200→50 | ✅ 已部署 |
| 4 | AdminSettings ConcurrentHashMap 60s 缓存 | ✅ 已部署 |
| 5 | 打包线程池 2→4 | ✅ 已部署 |
| 6 | JVM SerialGC→G1GC + MaxMetaspaceSize=128m | ✅ 已部署 |
| 7 | 日志 3GB→256MB | ✅ 已部署 |

**压测结果：** 登录 QPS 950、文件列表 QPS 400、内存余量 818MB

---

## 第二层：代码层优化（待实施 ❌）

详见 `OPTIMIZATION-CODE.md`

### P0 — 全表扫描 / N+1 查询

| # | 问题 | 文件 | 影响 |
|---|------|------|------|
| 1 | `collectSubtree()` 加载用户全部文件到内存 | FileServiceImpl:445 | 万级文件 O(N) 全表扫描 |
| 2 | `copy()` 调用 `collectSubtree()` 3次 | FileServiceImpl:287,301 | 3倍全表扫描 |
| 3 | `listShares()` 循环内逐条查 File | ShareServiceImpl:303 | N次 DB 查询 |
| 4 | `listMyTeams()` 循环内逐条查 Owner+Count+Role | TeamServiceImpl:176 | 3N次 DB 查询 |
| 5 | `snapshotSubtree()` 逐条 INSERT | ShareServiceImpl:765 | N条 INSERT（batchInsert 已有但未用） |
| 6 | `deleteToRecycle()` 逐条 UPDATE+INSERT | FileServiceImpl:358 | 2N条 SQL |

### P1 — 缺失缓存

| # | 问题 | 预期收益 |
|---|------|---------|
| 7 | File findById 无缓存 | 减少 50%+ DB 查询 |
| 8 | User findById 无缓存 | 减少 30%+ DB 查询 |
| 9 | Share findByToken 无缓存 | 分享访问零 DB 查询 |
| 10 | DisabledObject countBlocked 无缓存 | 下载/预览/分享零 DB 查询 |

### P2 — 阻塞 I/O / 内存

| # | 问题 | 影响 |
|---|------|------|
| 11 | 缩略图生成同步阻塞 Tomcat 线程 | 大图卡住请求数秒 |
| 12 | Dashboard findAll 加载全部用户+文件 | OOM 风险 |
| 13 | Merge 时所有分片流同时打开 | 连接泄漏风险 |

### P3 — 数据库索引

详见 `OPTIMIZATION-INDEX.md`

| # | 缺失索引 | 影响查询 |
|---|---------|---------|
| 1 | `t_file(file_hash, status, is_directory)` | disableByHash / restoreByHash |
| 2 | `t_file(user_id, team_id, status)` | 管理端文件搜索 |
| 3 | `t_share(status, expire_time, max_download)` | 分享下载计数 |
| 4 | `t_recycle_bin(user_id, parent_id)` | 回收站列表 |

---

## 第三层：安全 / 一致性（已完成 ✅）

| 项目 | 状态 |
|------|------|
| 四层权限校验（URL→Controller→Service→SQL） | ✅ |
| 7个 UNIQUE 索引保证幂等 | ✅ |
| 引用计数 CAS 原子递减 | ✅ |
| 分片上传断点续传 | ✅ |
| WebSocket 频道隔离 | ✅ |
| API 速率限制（滑动窗口） | ✅ |
| CAS 防负数 SQL 修复 | ✅ |

---

## 优先级排序

| 优先级 | 改动 | 预期收益 | 工作量 |
|--------|------|---------|--------|
| **P0** | MySQL 递归 CTE 替代全表扫描 | 万级文件操作 O(N)→O(子树) | 2h |
| **P0** | 批量操作替代循环单条 SQL | 100文件目录 200条→2条 SQL | 1h |
| **P1** | Caffeine 本地缓存 File/User/Settings | 减少 50%+ DB 查询 | 2h |
| **P1** | N+1 查询改 batch IN 查询 | 分享/团队列表 N→1 查询 | 1h |
| **P2** | 缩略图异步生成 | 释放 Tomcat 线程 | 1h |
| **P2** | 仪表盘改 COUNT/SUM 聚合 SQL | 消除 OOM 风险 | 0.5h |
| **P3** | 补充 4 个缺失索引 | 查询性能提升 | 0.5h |

---

## 简历写法

### 配置层优化（已完成）
> 负责云盘服务高负载调优，基于 Spring Boot + MySQL + Redis + MinIO 技术栈，针对 2C2G 腾讯云轻量实例，按"减少争抢→提升吞吐→控制开销"三层递进实施 7 项优化：HikariCP 连接池 10→30、Lettuce Redis 32 连接消除请求排队；Tomcat 50 线程适配低配 CPU、打包线程 2→4 提升并发处理能力；G1GC 替代 SerialGC 减少停顿、ConcurrentHashMap 60s 缓存省去每请求一次 DB 查询。使用 wrk 压测验证，最终实现登录 QPS 950、文件列表 QPS 400、支持 100+ 用户并发访问，系统内存占用稳定在 50% 以下。

### 代码层优化（待实施）
> 针对万级文件用户操作慢的问题，分析定位 6 处全表扫描和 N+1 查询瓶颈：用 MySQL 8 递归 CTE 替代 `collectSubtree()` 全表加载（万级文件从 O(N)→O(子树)），将目录删除/回收站的循环单条 SQL 改为批量操作（100 文件目录从 200 条 SQL→2 条），为 File/User/Share 引入 Caffeine 本地缓存（减少 50%+ DB 查询），修复 Share 快照逐条 INSERT 为批量插入。预计可将文件列表 QPS 从 400 提升至 1000+。
