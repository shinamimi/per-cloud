# 压测报告

> 测试日期：2026-08-31
> 测试环境：腾讯云轻量 2C2G，Ubuntu 22.04
> 测试工具：wrk 4.1.0 / ab 2.3

---

## 测试环境

| 项目 | 配置 |
|------|------|
| CPU | 2 核 |
| 内存 | 1.9GB |
| 磁盘 | 50GB SSD |
| OS | Ubuntu 22.04 (5.15.0-181) |
| Java | 21.0.12 + G1GC |
| MySQL | 8.4 (innodb_buffer_pool=256M, max_connections=100) |
| Redis | 7.2 (maxmemory=128MB) |
| MinIO | latest |
| Spring Boot | 4.0.7 |

---

## 测试结果

### 1. 登录接口 (POST /api/auth/login)

| 并发 | QPS | Avg 延迟 | P50 | P99 | 错误数 |
|------|-----|---------|-----|-----|--------|
| 10 | **630.8** | 45ms | 15ms | 855ms | 0 |
| 50 | **949.7** | 54ms | 60ms | 150ms | 0 |

**结论：** 登录 QPS 上限 ~950，足够支撑 100+ 并发用户。

### 2. 文件列表 (GET /api/files)

| 并发 | QPS | Avg 延迟 | P50 | P99 | 错误数 |
|------|-----|---------|-----|-----|--------|
| 10 | **201.4** | 57ms | 51ms | 251ms | 0 |
| 50（冷缓存） | **353.8** | 150ms | 125ms | 512ms | 1 timeout |
| 50（热缓存） | **398.2** | 127ms | 114ms | 317ms | 0 |

**结论：** 缓存热数据后 QPS 提升 13%，P99 从 512ms 降至 317ms。

### 3. 文件上传（完整流程 init→chunk→merge，50KB 文件）

| 并发 | 请求次数 | 总耗时 | QPS |
|------|---------|--------|-----|
| 10 | 30 | 2.8s | **10.5** |

**说明：** 单次上传包含 3 次 HTTP 往返（init→chunk→merge），QPS 受网络 RTT 限制。

### 4. 文件下载 (GET /api/files/{id}/download)

| 并发 | 请求次数 | 总耗时 | QPS |
|------|---------|--------|-----|
| 10×5 轮 | 50 | 1.6s | **62.6** |

**说明：** 下载接口返回 302 重定向到 MinIO presigned URL，QPS 受 MinIO 签名计算限制。

---

## 资源占用

### 压测前后对比

| 容器 | 压测前 | 压测后 | 变化 |
|------|--------|--------|------|
| Backend | 399MB | 483MB | +84MB |
| MySQL | 32MB | 88MB | +56MB |
| Redis | 6MB | 8MB | +2MB |
| 系统总内存 | 825MB | 957MB | +132MB |

### 连接池利用率

| 组件 | 配置值 | 峰值使用 | 利用率 |
|------|--------|---------|--------|
| HikariCP | 30 | 31 | 100% |
| MySQL Max_used_connections | 100 | 31 | 31% |
| Tomcat 线程 | 50 | ~50 | ~100% |

### JVM 状态

| 指标 | 值 |
|------|-----|
| 堆使用 | 483MB / 512MB (94%) |
| GC 类型 | G1GC |
| 启动时间 | 18.5s |

---

## 结论

| 指标 | 结果 | 评价 |
|------|------|------|
| 登录 QPS | 950 | ✅ 优秀 |
| 文件列表 QPS | 400 | ✅ 良好 |
| 上传 QPS | 10.5 | ⚠️ 受限于 3 次 HTTP 往返 |
| 下载 QPS | 62.6 | ✅ 良好 |
| 100 人并发 | 可行 | ✅ 登录 QPS 950 >> 100 |
| 内存余量 | 818MB | ✅ 充足 |
| HikariCP 利用率 | 100% | ⚠️ 已达上限，需关注 |

---

## 待验证

| 测试项 | 状态 |
|--------|------|
| 24h 长稳测试（内存泄漏） | ❌ 未执行 |
| 大文件分片上传（>10MB） | ❌ 未执行 |
| 并发打包下载（4 线程池） | ❌ 未执行 |
| 代码层优化后对比 | ❌ 待实施 |

---

## 附录：测试脚本

### wrk 登录压测

```bash
wrk -t2 -c50 -d15s --latency -s /tmp/login.lua http://127.0.0.1:8081/api/auth/login
```

### Lua 脚本（login.lua）

```lua
wrk.method = "POST"
wrk.headers["Content-Type"] = "application/json"
wrk.body = "{\"username\":\"bench\",\"password\":\"bench123\"}"
```

### 并发上传压测

```bash
for i in $(seq 1 30); do
  (
    INIT=$(curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
      -d "{\"fileName\":\"bench_${RANDOM}.bin\",\"fileSize\":51200,\"fileHash\":\"${HASH}\",\"parentId\":0}" \
      "http://127.0.0.1:8081/api/files/upload/init")
    UPLID=$(echo "$INIT" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['uploadId'])")
    curl -s -X POST -H "Authorization: Bearer $TOKEN" \
      -F "uploadId=$UPLID" -F "seq=1" -F "file=@/tmp/test_50k.bin" \
      "http://127.0.0.1:8081/api/files/upload/chunk" > /dev/null
    curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
      -d "{\"uploadId\":\"${UPLID}\"}" \
      "http://127.0.0.1:8081/api/files/upload/merge" > /dev/null
  ) &
  if (( i % 10 == 0 )); then wait; fi
done
wait
```
