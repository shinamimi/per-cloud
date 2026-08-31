# 高负载环境调优方案

> 更新日期：2026-08-31
> 目标环境：2C2G（腾讯云轻量）
> 目标负载：100+ 并发用户

---

## 一、现状问题

### 1.1 资源分配总览

| 服务 | 内存限制 | CPU | 当前配置 |
|---|---|---|---|
| MySQL | 800MB | 1.0 | buffer-pool=256M, max-connections=100 |
| MinIO | 640MB | 0.8 | 无连接池配置 |
| Backend (JVM) | 700MB | 1.0 | -Xmx512m, SerialGC, Tomcat 默认200线程 |
| Redis | 200MB | 0.2 | maxmemory=128mb, 无连接池配置 |
| Frontend (Nginx) | 128MB | 0.2 | gzip=on, 5个限流 zone 各10MB |

### 1.2 致命瓶颈清单

| # | 瓶颈 | 现状 | 100并发需求 | 缺口 |
|---|---|---|---|---|
| 1 | MySQL 连接池 | HikariCP 默认10连接 | 100+ 并发查询 | **90 连接** |
| 2 | Redis 连接池 | Lettuce 默认8连接 | 800+ ops/秒 | **24 连接** |
| 3 | MinIO HTTP 连接 | OkHttp 默认5连接 | 100+ 并发上传 | **15 连接** |
| 4 | 批量打包线程池 | 硬编码2线程 | 10+ 并发批量下载 | **8 线程** |
| 5 | Tomcat 线程池 | 默认200线程 | 50 线程足够 | **-150 线程（节省内存）** |
| 6 | 配置查询 | 每次查 DB | 应缓存 | **700 次/分钟 DB 查询** |

### 1.3 内存分布（当前）

```
总内存 2GB = 2048MB

JVM 堆:        512MB  (Xmx)
Metaspace:     无上限 (默认可用到256MB+)
线程栈:        200线程 × 1MB = 200MB (Tomcat 默认200线程)
NIO DirectBuf: 无上限
OS + 其他:     ~200MB
─────────────────────
总计:          1136MB+ (不含 Metaspace 和 DirectBuf)
```

**问题：线程栈占 200MB，加上堆和 Metaspace，总内存需求可能超过容器限制 700MB。**

---

## 二、优化方案（8 项改动）

### 改动 1：HikariCP 连接池

**文件：** `backend/src/main/resources/application-prod.yml`

**现状：** 无配置，Spring Boot 默认 `maximumPoolSize=10`

**问题：** 每个上传 init 要读 `t_setting` 4次（maxSizeUser、maxSizeVip、maxConcurrentUser、maxConcurrentVip），merge 要读写 `t_file` + `t_file_hash` + `t_user`。100 并发用户瞬间耗尽 10 个连接，后续请求排队等待 30 秒超时。

**改为：**

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 30
      minimum-idle: 5
      connection-timeout: 5000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
```

**理由：**
- `maximum-pool-size=30`：MySQL `max-connections=100`，后端是唯一客户端，30 连接够用且不浪费 MySQL 内存（每个连接 ~1MB buffer）
- `minimum-idle=5`：保持 5 个热连接，避免冷启动延迟
- `connection-timeout=5000`：5 秒拿不到连接快速失败，而不是等 30 秒
- `leak-detection-threshold=60000`：60 秒未归还连接打印泄漏警告

---

### 改动 2：Lettuce Redis 连接池

**文件：** `backend/src/main/resources/application-prod.yml`

**现状：** 无配置，默认 `max-active=8`

**问题：** 每个分片上传做 6-8 次 Redis 操作（isMember、add、remove、size、hasKey），merge 做 4 次（lock、members、meta、delete）。100 并发 = 800+ ops/秒，8 连接成为瓶颈。

**改为：**

```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 32
          max-idle: 16
          min-idle: 4
          max-wait: 3000
```

**理由：**
- `max-active=32`：Redis 单线程处理请求极快（微秒级），32 连接不会造成服务端压力
- `max-idle=16`：保持 16 个空闲连接，避免频繁创建/销毁
- `max-wait=3000`：3 秒拿不到连接快速失败

---

### 改动 3：MinIO 连接池

**文件：** `backend/src/main/java/com/cloud/backend/config/MinioConfig.java`

**现状：** 使用 MinIO SDK 默认 OkHttp，`connectionPool=5` 连接

**问题：** 所有 MinIO 操作（上传分片、下载分片做 merge、生成预签名 URL、objectExists 检查、删除）共享 5 个 HTTP 连接。100 并发上传 + merge 下载，5 个连接严重排队。

**改为：**

```java
@Bean
public MinioClient minioClient() {
    OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectionPool(new ConnectionPool(20, 5, TimeUnit.MINUTES))
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    return MinioClient.builder()
            .endpoint(properties.getEndpoint())
            .credentials(properties.getAccessKey(), properties.getSecretKey())
            .httpClient(httpClient)
            .build();
}
```

**`MinioProperties.java` 新增：**

```java
private int maxConnections = 20;
private int connectionTimeoutMs = 10000;
private int readTimeoutMs = 60000;
```

**理由：**
- `ConnectionPool(20, 5, MINUTES)`：最多 20 个并发连接，空闲 5 分钟回收
- `readTimeout=60s`：大文件上传/下载需要长超时
- 20 连接覆盖：100 并发用户 × 每用户 1 个分片上传 = 100 连接需求，但大部分是串行的（单文件内分片串行），实际并发约 20-30

---

### 改动 4：批量打包线程池

**文件：** `backend/src/main/java/com/cloud/backend/service/file/impl/DownloadServiceImpl.java`

**现状：** `Executors.newFixedThreadPool(2)` 硬编码

**问题：** 10 个用户同时请求批量下载，只有 2 个能执行，其余排队。每个打包操作涉及 N 次 MinIO 下载 + 写 zip + 上传，耗时可达分钟级。

**改为：**

```java
this.packExecutor = new java.util.concurrent.ThreadPoolExecutor(
    Runtime.getRuntime().availableProcessors(),           // 核心线程数（2核→2）
    Runtime.getRuntime().availableProcessors() * 2,       // 最大线程数（2核→4）
    60L, TimeUnit.SECONDS,
    new java.util.concurrent.LinkedBlockingQueue<>(100),
    runnable -> {
        Thread thread = new Thread(runnable, "pack-task");
        thread.setDaemon(true);
        return thread;
    },
    new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy()
);
```

**理由：**
- 核心线程数 = CPU 核数（I/O 密集型任务，线程数可以大于 CPU 核数）
- 最大线程数 = CPU×2（避免过多线程导致上下文切换）
- `CallerRunsPolicy`：队列满时让调用者线程执行，而不是丢弃任务

---

### 改动 5：缓存 isVip + admin settings

**文件：**
- `backend/src/main/java/com/cloud/backend/service/file/impl/UploadServiceImpl.java`
- `backend/src/main/java/com/cloud/backend/service/admin/impl/AdminSettingsServiceImpl.java`

**现状：** 每次调用都查 DB

**问题：** 100 并发用户，`isVip()` 每次上传调用 2-3 次 = 300 次 DB 查询/分钟，admin settings 每次上传调用 4 次 = 400 次 DB 查询/分钟。这些数据几乎不变。

**改为：**

**`AdminSettingsServiceImpl.java` 加 JVM 内存缓存：**

```java
private volatile Map<String, String> settingsCache = new ConcurrentHashMap<>();
private volatile long lastRefreshTime = 0;
private static final long REFRESH_INTERVAL_MS = 60_000; // 60秒刷新一次

private String getCachedSetting(String key, String defaultValue) {
    if (System.currentTimeMillis() - lastRefreshTime > REFRESH_INTERVAL_MS) {
        refreshCache();
    }
    return settingsCache.getOrDefault(key, defaultValue);
}

private synchronized void refreshCache() {
    if (System.currentTimeMillis() - lastRefreshTime > REFRESH_INTERVAL_MS) {
        settingsCache = settingMapper.findAll().stream()
            .collect(Collectors.toMap(Setting::getSettingKey, Setting::getSettingValue));
        lastRefreshTime = System.currentTimeMillis();
    }
}
```

**`UploadServiceImpl.java` 加 Redis 缓存 isVip：**

```java
private boolean isVip(Long userId) {
    String cacheKey = "user:vip:" + userId;
    String cached = redis.opsForValue().get(cacheKey);
    if (cached != null) {
        return "1".equals(cached);
    }
    boolean vip = Boolean.TRUE.equals(userService.findById(userId).getIsVip());
    redis.opsForValue().set(cacheKey, vip ? "1" : "0", Duration.ofMinutes(5));
    return vip;
}
```

**理由：**
- admin settings 变更频率极低（管理员手动改），60 秒缓存足够
- isVip 变更频率也低，5 分钟 Redis 缓存足够
- JVM 内存缓存避免每次查 DB，Redis 缓存避免跨实例不一致

---

### 改动 6：Tomcat 线程池

**文件：** `backend/src/main/resources/application-prod.yml`

**现状：** 未配置，默认 `server.tomcat.threads.max=200`

**问题：** 200 线程 × 1MB 栈 = 200MB 堆外内存，加上堆 512MB + Metaspace + DirectBuffer，总内存需求远超 700MB 容器限制。

**改为：**

```yaml
server:
  tomcat:
    threads:
      max: 50
      min-spare: 5
    max-connections: 100
    accept-count: 100
    connection-timeout: 10000
```

**理由：**
- `max=50`：50 线程 × 1MB = 50MB 栈内存，比默认节省 150MB
- 50 线程足够处理 100 并发用户（大部分是 I/O 等待，不是 CPU 计算）
- `accept-count=100`：队列长度 100，突发流量时排队而不是拒绝

---

### 改动 7：JVM 参数优化

**文件：** `backend/Dockerfile`

**现状：** `-Xms256m -Xmx512m -XX:+UseSerialGC`

**改为：**

```dockerfile
ENV JAVA_OPTS="-Xms256m -Xmx512m \
  -XX:+UseSerialGC \
  -XX:MaxGCPauseMillis=200 \
  -XX:MaxMetaspaceSize=128m \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:+UseGCOverheadLimit \
  -XX:NativeMemoryTracking=summary"
```

**理由：**
- `MaxGCPauseMillis=200`：限制 GC 暂停时间，Serial GC 会尽量在200ms内完成
- `MaxMetaspaceSize=128m`：防止 Metaspace 无限增长吃掉堆外内存
- `HeapDumpOnOutOfMemoryError`：OOM 时自动 dump，方便排查
- `UseGCOverheadLimit`：GC 耗时超 98% 且堆回收不到 2% 时提前 OOM，而不是无限 Full GC
- `NativeMemoryTracking=summary`：记录原生内存使用，排查内存泄漏

---

### 改动 8：日志上限

**文件：** `backend/src/main/resources/logback-spring.xml`

**现状：** prod profile 的 `totalSizeCap=3GB`

**问题：** 3GB 日志上限超过总内存 2GB，磁盘也会被填满。

**改为：**

```xml
<totalSizeCap>256MB</totalSizeCap>
```

**理由：** 2GB 磁盘/内存的机器，256MB 日志上限足够排查问题，又不会撑爆磁盘。

---

## 三、优化后内存分布

```
总内存 2GB = 2048MB

JVM 堆:          512MB  (Xmx 不变)
Metaspace:       ≤128MB (新增限制)
线程栈:          50线程 × 1MB = 50MB (从200降到50)
NIO DirectBuf:   ≤64MB  (MinIO + Tomcat)
OS + 其他:       ~200MB
─────────────────────
总计:            ≤954MB (安全，留1GB余量)
```

---

## 四、优化前后对比

| 指标 | 优化前 | 优化后 | 提升 |
|---|---|---|---|
| MySQL 连接池 | 10 | 30 | 3x |
| Redis 连接池 | 8 | 32 | 4x |
| MinIO HTTP 连接 | 5 | 20 | 4x |
| 打包线程池 | 2 | 4 | 2x |
| Tomcat 线程 | 200 | 50 | 内存 -150MB |
| 配置查询/分钟 | 700 | 0（缓存） | -100% |
| JVM Metaspace | 无上限 | 128MB | 可控 |
| 日志上限 | 3GB | 256MB | -91% |
| 堆外内存需求 | ~464MB | ~242MB | -48% |

---

## 五、测试方案

### 5.1 阶段 1：单元测试（每个改动独立验证）

#### 测试 1.1：HikariCP 连接池

```bash
# 监控命令
curl -s http://localhost:8081/actuator/metrics/hikaricp.connections.active
# 或通过 JMX
jconsole  # 连接到 Backend JVM → javax.sql → HikariPool
```

| 测试项 | 方法 | 预期结果 |
|---|---|---|
| 连接池大小 | 并发 30 个 /api/files/upload/init 请求 | 活跃连接 ≤30，无超时 |
| 连接泄漏检测 | 上传一个大文件，merge 期间 kill MySQL | 60 秒后日志打印泄漏警告 |
| 快速失败 | 停止 MySQL，发送上传请求 | 5 秒内返回错误，不等 30 秒 |
| 空闲回收 | 停止所有请求，等 10 分钟 | 活跃连接降到 5（minimum-idle） |

#### 测试 1.2：Lettuce Redis 连接池

```bash
# Redis 监控
redis-cli client list | grep -c addr
redis-cli info clients
```

| 测试项 | 方法 | 预期结果 |
|---|---|---|
| 连接池大小 | 并发 100 个分片上传请求 | Redis 客户端连接 ≤32 |
| 连接复用 | 串行上传 100 个分片 | 连接数稳定，不增长 |
| 快速失败 | 停止 Redis，发送上传请求 | 3 秒内返回错误 |

#### 测试 1.3：MinIO 连接池

```bash
# MinIO 连接监控
netstat -an | grep :9000 | grep ESTABLISHED | wc -l
```

| 测试项 | 方法 | 预期结果 |
|---|---|---|
| 连接池大小 | 并发 20 个 100MB 文件上传 | ESTABLISHED 连接 ≤20 |
| 连接复用 | 串行上传 50 个分片 | 连接数稳定 |
| 超时处理 | 上传时暂停 MinIO 容器 | 60 秒后超时报错 |

#### 测试 1.4：批量打包线程池

```bash
# 线程监控
jstack <backend_pid> | grep -c "pack-task"
```

| 测试项 | 方法 | 预期结果 |
|---|---|---|
| 线程数 | 同时发起 10 个批量下载请求 | 活跃线程 ≤4（最大线程数） |
| 队列满处理 | 同时发起 20 个批量下载请求 | 前 4 个执行，中间 10 个排队，最后 6 个由调用者线程执行 |
| 任务完成 | 每个打包任务包含 50 个文件 | 全部成功完成，无丢失 |

#### 测试 1.5：缓存命中率

```bash
# 日志搜索
grep -c "isVip cache hit" application.log
grep -c "settings cache hit" application.log
```

| 测试项 | 方法 | 预期结果 |
|---|---|---|
| isVip 缓存 | 同一用户连续上传 10 个文件 | 缓存命中率 >90%（第一次 miss，后续 hit） |
| settings 缓存 | 100 并发上传 | 缓存命中率 >99%（60 秒内只有第一次查 DB） |
| 缓存一致性 | 管理员修改 maxConcurrentUser | 60 秒内旧值生效，60 秒后新值生效 |

#### 测试 1.6：Tomcat 线程池

```bash
# 线程监控
jstack <backend_pid> | grep -c "http-nio"
```

| 测试项 | 方法 | 预期结果 |
|---|---|---|
| 最大线程数 | 并发 100 个 API 请求 | http-nio 线程 ≤50 |
| 队列排队 | 并发 200 个 API 请求 | 前 50 个执行，中间 100 个排队，后 50 个拒绝 |
| 响应时间 | 并发 50 个 API 请求 | P99 <500ms |

#### 测试 1.7：JVM 内存

```bash
# GC 监控
jstat -gc <backend_pid> 1000 60  # 每秒采样，共60秒
jcmd <backend_pid> VM.native_memory summary
```

| 测试项 | 方法 | 预期结果 |
|---|---|---|
| 堆使用率 | 并发上传/下载 1 小时 | Old Gen <80% |
| Full GC 频率 | 并发上传/下载 1 小时 | Full GC <5次/小时 |
| Metaspace | 运行 24 小时 | Metaspace <128MB |
| 线程栈内存 | 运行 24 小时 | 线程数 ≤60（50 Tomcat + 5 pack + 5 其他） |

---

### 5.2 阶段 2：集成测试（多服务联合）

#### 测试 2.1：并发上传

```bash
#!/bin/bash
# 并发上传脚本
for i in $(seq 1 30); do
  dd if=/dev/urandom of=/tmp/test_${i}.bin bs=1M count=100  # 100MB 测试文件
  curl -X POST http://localhost:8081/api/files/upload/init \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"fileName\":\"test_${i}.bin\",\"fileSize\":104857600,\"parentId\":0}" &
done
wait
echo "All uploads initiated"
```

| 测试项 | 方法 | 预期结果 |
|---|---|---|
| 30 并发上传 | 30 个 100MB 文件同时上传 | 全部成功，无 OOM，无超时 |
| 30 并发下载 | 30 个批量下载同时请求 | 全部成功，打包线程池不拒绝 |
| 混合负载 | 15 上传 + 15 下载同时进行 | 全部成功，响应时间 <5秒 |

#### 测试 2.2：故障恢复

| 测试项 | 方法 | 预期结果 |
|---|---|---|
| MySQL 重启 | 上传过程中 kill MySQL 容器 | 5 秒内报错，MySQL 恢复后自动重连 |
| Redis 重启 | 上传过程中 kill Redis 容器 | 3 秒内报错，Redis 恢复后自动重连 |
| MinIO 重启 | 上传过程中 kill MinIO 容器 | 60 秒内超时报错，MinIO 恢复后自动重连 |
| Backend 重启 | 重启 Backend 容器 | 30 秒内恢复服务，进行中的上传任务丢失（可重新上传） |

#### 测试 2.3：数据一致性

| 测试项 | 方法 | 预期结果 |
|---|---|---|
| 秒传一致性 | 两个用户上传相同文件 | t_file_hash.ref_count=2，删除一个后 ref_count=1，两个都删后对象被清理 |
| 引用计数准确性 | 上传 10 个相同文件，逐个删除 | ref_count 从10 递减到0，归零后 MinIO 对象被删除 |
| 并发删除 | 两个用户同时删除同一文件 | ref_count 正确递减，不会出现负数或重复删除 |

---

### 5.3 阶段 3：压测（模拟真实负载）

#### 压测工具

```bash
# 安装 wrk
brew install wrk  # macOS
# 或
apt install wrk   # Linux
```

#### 测试 3.1：API 吞吐量

```bash
# 文件列表 API
wrk -t4 -c100 -d60s http://localhost:8081/api/files?parentId=0

# 上传初始化 API
wrk -t4 -c100 -d60s -s upload_init.lua http://localhost:8081/api/files/upload/init
```

| 指标 | 目标值 |
|---|---|
| QPS（文件列表） | >200 |
| QPS（上传初始化） | >100 |
| P99 响应时间 | <500ms |
| 错误率 | <0.1% |

#### 测试 3.2：上传吞吐量

```bash
# 自定义压测脚本
for i in $(seq 1 10); do
  (
    TOKEN=$(login "user${i}" "pass${i}")
    dd if=/dev/urandom of=/tmp/load_${i}.bin bs=1M count=50
    upload_file $TOKEN /tmp/load_${i}.bin
  ) &
done
wait
```

| 指标 | 目标值 |
|---|---|
| 10 并发上传 50MB | 全部成功 |
| 平均上传耗时 | <30秒 |
| 最大上传耗时 | <60秒 |

#### 测试 3.3：下载吞吐量

```bash
# 批量下载压测
for i in $(seq 1 10); do
  (
    TOKEN=$(login "user${i}" "pass${i}")
    batch_download $TOKEN $DIR_ID
  ) &
done
wait
```

| 指标 | 目标值 |
|---|---|
| 10 并发批量下载 | 全部成功 |
| 平均下载耗时 | <60秒 |
| 最大下载耗时 | <120秒 |

#### 测试 3.4：内存稳定性

```bash
# 压测期间每 5 秒采样
while true; do
  docker stats --no-stream --format "{{.Name}} {{.MemUsage}}" | grep backend
  sleep 5
done
```

| 指标 | 目标值 |
|---|---|
| Backend 内存 | <600MB（700MB 限制的85%） |
| MySQL 内存 | <700MB（800MB 限制的87%） |
| Redis 内存 | <120MB（128MB 限制的93%） |
| OOM Kill | 0 次 |

---

### 5.4 阶段 4：24 小时稳定性测试

```bash
#!/bin/bash
# 24 小时自动化测试脚本
START=$(date +%s)
END=$((START + 86400))  # 24小时

while [ $(date +%s) -lt $END ]; do
  # 随机操作
  OP=$((RANDOM % 3))
  case $OP in
    0)  # 上传
      SIZE=$((RANDOM % 104857600 + 10240))  # 10KB - 100MB
      dd if=/dev/urandom of=/tmp/stress.bin bs=1 count=$SIZE 2>/dev/null
      upload_file $TOKEN /tmp/stress.bin
      ;;
    1)  # 下载
      download_file $TOKEN $FILE_ID
      ;;
    2)  # 目录操作
      mkdir -p /tmp/stress_$(date +%s)
      ;;
  esac

  # 记录指标
  echo "$(date +%H:%M:%S) $(docker stats --no-stream --format '{{.MemUsage}}' cloud-backend)" >> metrics.log

  sleep $((RANDOM % 10 + 1))  # 1-10秒间隔
done
```

#### 监控指标

| 指标 | 采样频率 | 告警阈值 |
|---|---|---|
| JVM 堆使用率 | 每 5 分钟 | >80% |
| Full GC 次数 | 每 5 分钟 | >5 次/小时 |
| 活跃 DB 连接 | 每 1 分钟 | >25（80% 池容量） |
| 活跃 Redis 连接 | 每 1 分钟 | >25（80% 池容量） |
| API 错误率 | 每 5 分钟 | >0.1% |
| P99 响应时间 | 每 5 分钟 | >1 秒 |
| 容器内存 | 每 1 分钟 | >600MB |

#### 最终验收标准

| 指标 | 目标值 |
|---|---|
| 运行时间 | ≥24 小时 |
| 重启次数 | 0 |
| OOM Kill | 0 |
| 数据丢失 | 0 |
| 平均响应时间 | <500ms |
| P99 响应时间 | <2 秒 |
| 错误率 | <0.1% |

---

## 六、测试报告模板

```markdown
# 高负载调优测试报告

## 测试环境
- 服务器：腾讯云轻量 2C2G
- 操作系统：Ubuntu 22.04
- Docker 版本：24.x
- Java 版本：21
- 测试时间：YYYY-MM-DD HH:MM ~ YYYY-MM-DD HH:MM

## 测试结果

### 阶段 1：单元测试
- [ ] HikariCP 连接池：通过/失败
- [ ] Lettuce 连接池：通过/失败
- [ ] MinIO 连接池：通过/失败
- [ ] 打包线程池：通过/失败
- [ ] 缓存命中率：通过/失败
- [ ] Tomcat 线程池：通过/失败
- [ ] JVM 内存：通过/失败

### 阶段 2：集成测试
- [ ] 并发上传：通过/失败
- [ ] 并发下载：通过/失败
- [ ] 故障恢复：通过/失败
- [ ] 数据一致性：通过/失败

### 阶段 3：压测
- [ ] API 吞吐量：通过/失败（QPS=___, P99=___ms）
- [ ] 上传吞吐量：通过/失败（平均___秒）
- [ ] 下载吞吐量：通过/失败（平均___秒）
- [ ] 内存稳定性：通过/失败（峰值___MB）

### 阶段 4：稳定性
- [ ] 24 小时运行：通过/失败（重启___次，OOM___次）

## 结论
- 优化前最大并发：___用户
- 优化后最大并发：___用户
- 内存使用降低：___%
- 响应时间降低：___%
```

---

## 七、改动优先级

| 优先级 | 改动 | 预期效果 | 风险 | 工作量 |
|---|---|---|---|---|
| 1 | HikariCP 连接池 | DB 不再排队 | 低 | 5 分钟 |
| 2 | Lettuce 连接池 | Redis 不再排队 | 低 | 5 分钟 |
| 3 | Tomcat 线程 200→50 | 节省 150MB 内存 | 低 | 5 分钟 |
| 4 | 缓存 isVip + settings | 减少 700 次/分钟 DB 查询 | 低 | 30 分钟 |
| 5 | MinIO 连接池 | MinIO 不再排队 | 中 | 15 分钟 |
| 6 | 批量打包线程池 | 批量下载不阻塞 | 低 | 10 分钟 |
| 7 | JVM 参数 | Metaspace 限制 + OOM 保护 | 低 | 5 分钟 |
| 8 | 日志上限 | 防止磁盘撑满 | 低 | 5 分钟 |

**总工作量：约 80 分钟**
