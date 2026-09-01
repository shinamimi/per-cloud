# Cloud 2C2G 全面性能测试计划

> 创建日期：2026-09-02
> 测试环境：腾讯云轻量 2C2G (101.35.233.30)
> 测试工具：wrk 4.1.0, curl, vmstat, iostat, docker stats
> 测试方法：控制变量法（每轮测试前预热 30 分钟）

---

## 一、测试环境

### 1.1 硬件配置

| 资源 | 规格 |
|------|------|
| CPU | 2 核 |
| 内存 | 1.9GB |
| 磁盘 | 50GB SSD |
| OS | Ubuntu 22.04 (5.15.0-181) |

### 1.2 容器资源分配

| 容器 | CPU | 内存 | 关键配置 |
|------|-----|------|----------|
| Backend | 2.0 | 700m | G1GC, Tomcat 50 threads, HikariCP 30 |
| MySQL | 1.0 | 800m | buffer_pool=256M, max_connections=100 |
| Redis | 0.2 | 200m | maxmemory=128mb, allkeys-lru |
| MinIO | 0.8 | 640m | 单盘 |
| Frontend | 0.2 | 128m | Nginx |

**总计**: 4.2 CPU / 2.5 GB（超出物理机，存在 CPU 竞争）

### 1.3 软件版本

| 组件 | 版本 |
|------|------|
| Java | 21.0.12 + G1GC |
| Spring Boot | 4.0.7 |
| MySQL | 8.4 |
| Redis | 7.2 |
| MinIO | latest |

---

## 二、测试前提

### 2.1 环境准备

```bash
# 1. 确认 wrk 已安装
wrk --version

# 2. 确认服务正常
curl http://127.0.0.1:8081/actuator/health

# 3. 获取 Token
TOKEN=$(curl -s http://localhost:8081/api/auth/login -X POST \
  -H "Content-Type: application/json" \
  -d '{"username":"bench","password":"bench123"}' | jq -r '.data.token')

# 4. 预热 JVM（30 分钟）
for i in $(seq 1 100); do
  curl -s http://127.0.0.1:8081/api/files?parentId=0 \
    -H "Authorization: Bearer $TOKEN" > /dev/null
done
```

### 2.2 测试规则

- 每轮测试间隔 5 分钟冷却
- 每项测试前清空相关缓存（Redis/JVM）
- 所有测试直连后端（127.0.0.1:8081），绕过 Nginx
- 记录完整 wrk 输出 + GC 日志 + 系统资源

---

## 三、测试矩阵

### 阶段 1：环境准备 + 数据初始化（30 min）

| 步骤 | 操作 | 验证 |
|------|------|------|
| 1.1 | 确认 wrk 安装 | `wrk --version` |
| 1.2 | 登录获取 Token | Token 非空 |
| 1.3 | 预热 JVM 30 分钟 | GC 日志显示 G1GC 工作正常 |
| 1.4 | 创建测试目录结构 | 目录创建成功 |

### 阶段 2：文件列表查询（核心，2 h）

#### 场景矩阵

| 场景 | 数据量 | 并发 | 持续 | 重点观察 |
|------|--------|------|------|----------|
| 2.1 | 100 文件 | 50 | 60s | 基线 QPS |
| 2.2 | 1000 文件 | 50 | 60s | 分页性能 |
| 2.3 | 10000 文件 | 50 | 60s | 扫描行数 |
| 2.4 | 1000 文件, page=50 | 50 | 60s | 深分页 |
| 2.5 | 10000 文件 | 100 | 60s | 极限压测 |

#### 测试命令

```bash
# 场景 2.1-2.3：不同数据量
wrk -t2 -c50 -d60s --latency \
  -H "Authorization: Bearer $TOKEN" \
  "http://127.0.0.1:8081/api/files?parentId={DIR_ID}&size=20"

# 场景 2.4：深分页
wrk -t2 -c50 -d60s --latency \
  -H "Authorization: Bearer $TOKEN" \
  "http://127.0.0.1:8081/api/files?parentId={DIR_ID}&page=50&size=20"

# 场景 2.5：极限压测
wrk -t2 -c100 -d60s --latency \
  -H "Authorization: Bearer $TOKEN" \
  "http://127.0.0.1:8081/api/files?parentId={DIR_ID}&size=20"
```

#### 记录指标

| 指标 | 含义 | 采集方式 |
|------|------|----------|
| QPS | 每秒查询次数 | wrk 输出 |
| Avg RT | 平均响应时间 | wrk 输出 |
| P50 | 中位数延迟 | wrk 输出 |
| P95 | 95% 延迟 | wrk 输出 |
| P99 | 99% 延迟 | wrk 输出 |
| 错误率 | 失败请求比例 | wrk 输出 |
| Slow Queries | MySQL 慢查询增量 | `SHOW STATUS` |
| GC 次数 | Young/Full GC 次数 | GC 日志 |
| GC 暂停 | 总暂停时间 | GC 日志 |

### 阶段 3：登录认证（30 min）

#### 场景

| 场景 | 并发 | 持续 | 重点 |
|------|------|------|------|
| 3.1 | 1 | 30s | 纯 RT 基线 |
| 3.2 | 50 | 60s | 正常负载 QPS |
| 3.3 | 100 | 60s | 极限压测 |

#### 测试命令

```bash
# 场景 3.1：单用户基线
wrk -t1 -c1 -d30s --latency -s /tmp/login.lua \
  http://127.0.0.1:8081/api/auth/login

# 场景 3.2：正常负载
wrk -t2 -c50 -d60s --latency -s /tmp/login.lua \
  http://127.0.0.1:8081/api/auth/login

# 场景 3.3：极限压测
wrk -t2 -c100 -d60s --latency -s /tmp/login.lua \
  http://127.0.0.1:8081/api/auth/login
```

#### Lua 脚本（login.lua）

```lua
wrk.method = "POST"
wrk.headers["Content-Type"] = "application/json"
wrk.body = "{\"username\":\"bench\",\"password\":\"bench123\"}"
```

#### 记录指标

| 指标 | 含义 | 采集方式 |
|------|------|----------|
| QPS | 每秒登录次数 | wrk 输出 |
| RT | 响应时间 | wrk 输出 |
| Redis INCR | rate_limit 操作 | Redis MONITOR |
| BCrypt CPU | 哈希计算占用 | docker stats |
| JWT 生成 | Token 生成耗时 | 应用日志 |

### 阶段 4：文件上传（2 h）

#### 4.1 吞吐量测试

| 文件大小 | 分片大小 | 并发分片 | 预期分片数 |
|----------|----------|----------|-----------|
| 10MB | 10MB | 1 | 1 |
| 100MB | 10MB | 1/3/5 | 10 |
| 1GB | 10MB | 1/3/5/10 | 100 |
| 5GB | 10MB | 1/3/5/10 | 500 |

#### 测试命令

```bash
# 生成测试文件
dd if=/dev/urandom of=/tmp/test_100MB.bin bs=1M count=100

# 上传分片（并发）
for i in $(seq 1 $CONCURRENCY); do
  curl -X POST "http://127.0.0.1:8081/api/files/upload/chunk" \
    -H "Authorization: Bearer $TOKEN" \
    -F "file=@chunk_${i}.bin" \
    -F "uploadId=$UPLOAD_ID" \
    -F "seq=$i" &
done
wait

# 触发 merge
curl -X POST "http://127.0.0.1:8081/api/files/upload/merge" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"uploadId\":\"$UPLOAD_ID\",\"fileName\":\"test.bin\",\"fileSize\":104857600,\"fileHash\":\"...\"}"
```

#### 记录指标

| 指标 | 含义 | 采集方式 |
|------|------|----------|
| 上传速度 | MB/s | 计算 |
| chunk RT | 每片上传耗时 | curl 计时 |
| merge RT | 合并耗时（含 SHA-256） | curl 计时 |
| CPU 占用 | 压测期间 CPU | docker stats |
| 内存占用 | 压测期间内存 | docker stats |

#### 4.2 秒传测试

| 步骤 | 操作 | 预期耗时 |
|------|------|----------|
| 第一次上传 | 1GB 完整上传 | 10-30s |
| 第二次上传 | 相同文件秒传 | <1s |
| 并发秒传 | 10 用户同时上传同一文件 | 全部秒传 |

### 阶段 5：Hash 计算（1 h）

#### 测试方法

在 merge 阶段测量 SHA-256 耗时：

| 文件大小 | 预期 Hash 耗时 | CPU 占用 |
|----------|---------------|----------|
| 10MB | <0.1s | 低 |
| 100MB | ~0.5s | 中 |
| 1GB | ~5s | 高 |
| 5GB | ~25s | 打满 |

#### 测试命令

```bash
# 测量 Hash 耗时
time sha256sum /tmp/test_1GB.bin
```

#### 记录指标

| 指标 | 含义 | 采集方式 |
|------|------|----------|
| Hash 时间 | SHA-256 计算耗时 | time 命令 |
| CPU 占用 | 计算期间 CPU | docker stats |
| 是否阻塞其他请求 | 并发影响 | 观察其他接口 RT |

### 阶段 6：下载性能（1 h）

#### 场景

| 场景 | 文件大小 | 并发 | 测试方法 |
|------|----------|------|----------|
| 6.1 小文件 | 1MB | 50 | 302 重定向 |
| 6.2 中文件 | 100MB | 20 | MinIO presigned |
| 6.3 大文件 | 1GB | 10 | MinIO presigned |

#### 测试命令

```bash
# 获取下载 URL
DOWNLOAD_URL=$(curl -s "http://127.0.0.1:8081/api/files/$FILE_ID/download" \
  -H "Authorization: Bearer $TOKEN" | jq -r '.data')

# 测试下载吞吐
wrk -t2 -c50 -d60s --latency \
  -H "Authorization: Bearer $TOKEN" \
  "http://127.0.0.1:8081/api/files/$FILE_ID/download"
```

#### 记录指标

| 指标 | 含义 | 采集方式 |
|------|------|----------|
| 吞吐量 | MB/s | 计算 |
| 302 重定向耗时 | 后端响应时间 | wrk 输出 |
| MinIO 带宽 | 实际下载速度 | iperf/docker stats |

### 阶段 7：数据库 + Redis（1 h）

#### 7.1 MySQL EXPLAIN

```sql
-- 文件列表查询执行计划
EXPLAIN SELECT * FROM t_file 
WHERE user_id = ? AND team_id = 0 AND parent_id = ? AND status != 0
ORDER BY type DESC, created_at DESC
LIMIT 0, 20;

-- 慢查询监控
SET GLOBAL slow_query_log = 1;
SET GLOBAL long_query_time = 0.1;  -- 100ms
```

#### 7.2 Redis 基准测试

```bash
# Redis 自带基准测试
docker exec cloud-redis redis-benchmark -n 100000 -c 50 -q

# 实际 Redis 操作监控
docker exec cloud-redis redis-cli MONITOR
```

#### 记录指标

| 指标 | 含义 | 采集方式 |
|------|------|----------|
| EXPLAIN type | 访问类型 | EXPLAIN |
| EXPLAIN rows | 扫描行数 | EXPLAIN |
| EXPLAIN key | 使用索引 | EXPLAIN |
| 慢 SQL 数量 | 超过阈值的查询 | slow_query_log |
| Redis QPS | GET/SET 吞吐 | redis-benchmark |
| Redis 延迟 | P99 延迟 | redis-benchmark |

### 阶段 8：并发一致性（2 h）

#### 8.1 引用计数一致性

```bash
# 100 线程同时删除同一文件
for i in $(seq 1 100); do
  curl -X DELETE "http://127.0.0.1:8081/api/files/$FILE_ID" \
    -H "Authorization: Bearer $TOKEN" &
done
wait

# 检查 ref_count
mysql -e "SELECT ref_count FROM t_file_hash WHERE file_hash = '$HASH';"
```

**验证**: `ref_count` 最终应为 0，且记录被清理

#### 8.2 秒传并发

```bash
# 10 用户同时上传相同文件
for i in $(seq 1 10); do
  # 上传分片 + merge
done

# 检查 t_file_hash
mysql -e "SELECT COUNT(*), ref_count FROM t_file_hash WHERE file_hash = '$HASH';"
```

**验证**: `t_file_hash` 应只有 1 条记录，`ref_count` = 10

#### 8.3 权限攻击

```bash
# 用户 A 尝试访问用户 B 的文件
curl -H "Authorization: Bearer $TOKEN_A" \
  "http://127.0.0.1:8081/api/files/$FILE_ID_B"
```

**验证**: 全部返回 403 或 404

### 阶段 9：JVM + 系统资源（贯穿全程）

#### 监控命令

```bash
# 每 5 秒采集一次系统资源
vmstat 1 5
iostat -x 1 5
docker stats --no-stream

# JVM 状态
jstat -gc $(docker exec cloud-backend jps | grep app | awk '{print $1}')

# 线程状态
jstack $(docker exec cloud-backend jps | grep app | awk '{print $1}')
```

#### 记录指标

| 指标 | 含义 | 采集频率 |
|------|------|----------|
| Heap 使用率 | 堆内存占用 | 每轮测试后 |
| Young GC 次数 | Young GC 频率 | 每轮测试后 |
| Young GC 暂停 | Young GC 总暂停 | 每轮测试后 |
| Full GC 次数 | Full GC 频率 | 每轮测试后 |
| Old 区占用 | 老年代内存 | 每轮测试后 |
| CPU 用户态 | 应用 CPU 占用 | 每 5 秒 |
| CPU 系统态 | 内核 CPU 占用 | 每 5 秒 |
| IO 等待 | 磁盘 IO 等待 | 每 5 秒 |
| 内存 RSS | 实际内存占用 | 每 5 秒 |
| 磁盘读写 | MB/s, IOPS | 每 5 秒 |
| 网络流量 | Mbps | 每 5 秒 |

---

## 四、测试执行顺序

| 阶段 | 内容 | 预计耗时 | 依赖 |
|------|------|----------|------|
| 1 | 环境准备 + 数据初始化 | 30 min | 无 |
| 2 | 文件列表（5 场景） | 2 h | 阶段 1 |
| 3 | 登录认证（3 场景） | 30 min | 阶段 1 |
| 4 | 文件上传（吞吐+并发+秒传） | 2 h | 阶段 1 |
| 5 | Hash 计算 | 1 h | 阶段 4 |
| 6 | 下载性能 | 1 h | 阶段 4 |
| 7 | 数据库 + Redis | 1 h | 无 |
| 8 | 并发一致性 | 2 h | 阶段 1 |
| 9 | JVM + 系统资源 | 贯穿全程 | 无 |
| **总计** | | **~10 h** | |

---

## 五、产出物

| 文件 | 内容 |
|------|------|
| `PERF-TEST-REPORT-2C2G.md` | 完整测试报告 |
| `scripts/perf-test/` | 所有测试脚本 |
| `scripts/perf-test/data/` | 原始数据 CSV |
| `scripts/perf-test/gc-logs/` | GC 日志片段 |

---

## 六、测试报告模板

```markdown
# Cloud 2C2G 全面性能测试报告

> 测试日期：YYYY-MM-DD
> 测试环境：腾讯云轻量 2C2G
> 测试时长：~10 小时

## 测试结果总览

| 测试项 | 指标 | 结果 | 评价 |
|--------|------|------|------|
| 文件列表 (100) | QPS | | |
| 文件列表 (1000) | QPS | | |
| 文件列表 (10000) | QPS | | |
| 登录 | QPS | | |
| 上传 (100MB) | MB/s | | |
| 下载 (100MB) | MB/s | | |
| Hash (1GB) | 秒 | | |
| 引用计数 | 一致性 | | |
| 秒传并发 | 一致性 | | |

## 详细测试数据

### 文件列表查询

| 场景 | QPS | P50 | P95 | P99 | 错误率 |
|------|-----|-----|-----|-----|--------|
| 100 文件 | | | | | |
| 1000 文件 | | | | | |
| 10000 文件 | | | | | |
| 1000 深分页 | | | | | |
| 10000 极限 | | | | | |

### 登录认证

| 场景 | QPS | P50 | P95 | P99 | 错误率 |
|------|-----|-----|-----|-----|--------|
| 单用户 | | | | | |
| 50 并发 | | | | | |
| 100 并发 | | | | | |

### 文件上传

| 文件大小 | 并发 | 速度 | chunk RT | merge RT | CPU |
|----------|------|------|----------|----------|-----|
| 10MB | 1 | | | | |
| 100MB | 1 | | | | |
| 100MB | 3 | | | | |
| 100MB | 5 | | | | |
| 1GB | 1 | | | | |
| 1GB | 5 | | | | |
| 1GB | 10 | | | | |

### Hash 计算

| 文件大小 | Hash 时间 | CPU 占用 |
|----------|----------|----------|
| 10MB | | |
| 100MB | | |
| 1GB | | |
| 5GB | | |

### 下载性能

| 文件大小 | 并发 | 吞吐量 |
|----------|------|--------|
| 1MB | 50 | |
| 100MB | 20 | |
| 1GB | 10 | |

### 数据库性能

| 场景 | type | key | rows | Extra |
|------|------|-----|------|-------|
| 文件列表 | | | | |

### Redis 性能

| 操作 | QPS | P99 |
|------|-----|-----|
| GET | | |
| SET | | |

### 并发一致性

| 场景 | 结果 | 说明 |
|------|------|------|
| 引用计数 | | |
| 秒传并发 | | |
| 权限攻击 | | |

### JVM 指标

| 指标 | 值 |
|------|-----|
| Heap 使用率 | |
| Young GC 次数 | |
| Young GC 暂停 | |
| Full GC 次数 | |
| Old 区占用 | |

### 系统资源

| 指标 | 平均值 | 峰值 |
|------|--------|------|
| CPU 用户态 | | |
| CPU 系统态 | | |
| IO 等待 | | |
| 内存 RSS | | |
| 磁盘读 | | |
| 磁盘写 | | |

## 结论与建议

### 优势

- 

### 瓶颈

- 

### 优化建议

1. 

### 扩容建议

| 配置 | 预期提升 | 成本 |
|------|----------|------|
| 2C2G → 4C4G | | |
| 增加 Redis 内存 | | |
| 增加 MySQL buffer_pool | | |
```

---

## 七、测试脚本目录结构

```
scripts/perf-test/
├── 01-prepare.sh          # 环境准备
├── 02-file-list.sh        # 文件列表测试
├── 03-login.sh            # 登录测试
├── 04-upload.sh           # 上传测试
├── 05-hash.sh             # Hash 测试
├── 06-download.sh         # 下载测试
├── 07-database-redis.sh   # 数据库 Redis 测试
├── 08-concurrency.sh      # 并发一致性测试
├── 09-monitor.sh          # 系统资源监控
├── lua/
│   ├── login.lua          # 登录 wrk 脚本
│   ├── filelist.lua       # 文件列表 wrk 脚本
│   └── download.lua       # 下载 wrk 脚本
└── data/                  # 测试数据目录
```

---

## 八、验收标准

| 指标 | 目标值 | 说明 |
|------|--------|------|
| 文件列表 QPS (100) | ≥400 | 与基线持平 |
| 文件列表 QPS (1000) | ≥300 | 大目录略有下降 |
| 文件列表 QPS (10000) | ≥100 | 万级目录可接受 |
| 登录 QPS | ≥950 | 与基线持平 |
| 上传速度 (100MB) | ≥30MB/s | 受 MinIO 限制 |
| 下载速度 (100MB) | ≥50MB/s | 受 MinIO 带宽限制 |
| Hash (1GB) | ≤10s | CPU 密集型 |
| 引用计数一致性 | 100% | 无竞态条件 |
| 秒传并发 | 100% | 无重复对象 |
| 权限攻击 | 100% 拒绝 | 无越权 |
| 错误率 | <1% | 稳定性 |
| JVM Full GC | 0 次/小时 | 内存稳定 |
