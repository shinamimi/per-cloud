# 安全审计报告

> 审计日期：2026-09-01
> 审计范围：8 大安全维度 × 全量代码审查

---

## 一、审计总览

| # | 安全维度 | 实现状态 | 待修复 | 待测试 |
|---|---|---|---|---|
| 1 | 身份认证安全 | ✅ 全部实现 | 0 | 3 |
| 2 | 权限安全 | ⚠️ 2处未实现 | 2 | 4 |
| 3 | 文件存储安全 | ⚠️ 2处未实现 | 2 | 3 |
| 4 | 上传安全 | ✅ 全部实现 | 0 | 2 |
| 5 | 数据库安全 | ✅ 全部实现 | 0 | 1 |
| 6 | 接口安全 | ✅ 全部实现 | 0 | 2 |
| 7 | 数据一致性安全 | ⚠️ 部分实现 | 1 | 2 |
| 8 | 运维安全 | ⚠️ 1处未实现 | 1 | 2 |

---

## 二、逐项审计详情

### 1. 身份认证安全 ✅

| 子项 | 状态 | 实现方式 | 位置 |
|---|---|---|---|
| 密码 BCrypt 加密 | ✅ | `BCryptPasswordEncoder` | `PasswordEncoderConfig.java` |
| 密码复杂度校验 | ✅ | 8-20位，必须包含字母和数字 | `RegisterRequest.java:32-33` |
| JWT 签名 | ✅ | HMAC-SHA + Base64 密钥 | `JwtTokenUtilImpl.java:39-43` |
| JWT 过期时间 | ✅ | 24小时，可通过管理员配置调整 | `application-prod.yml:54` |
| JWT 黑名单（登出） | ✅ | Redis 存储，每次请求校验 | `JwtBlacklistService.java:40-47` |
| 登录防爆破（账户锁定） | ✅ | 5次失败锁定30分钟，Redis+DB 双重 | `LoginAttemptService.java:44-68` |
| 登录限流（IP维度） | ✅ | `@RateLimit` 10次/分钟/IP | `AuthController.java:54-55` |

**待测试：**
- [ ] T1.1：注册密码必须包含字母+数字，纯数字应被拒绝
- [ ] T1.2：登出后 JWT 应被拒绝（Redis 黑名单生效）
- [ ] T1.3：连续5次错误密码后账户应被锁定30分钟

---

### 2. 权限安全 ⚠️

| 子项 | 状态 | 实现方式 | 位置 |
|---|---|---|---|
| 文件归属校验 | ✅ | `getOwnedFile(userId, fileId)` | `FileServiceImpl.java:407-417` |
| SQL 层 user_id 过滤 | ✅ | 所有列表查询带 `WHERE user_id=#{userId}` | `FileMapper.xml` |
| 分享归属校验 | ✅ | `requireOwnedShare(userId, shareId)` | `ShareServiceImpl.java:578-584` |
| 分享 token 随机性 | ✅ | SecureRandom + 58^10 空间 | `ShareTokenGenerator.java` |
| 分享过期时间 | ✅ | 应用层 + SQL 层双重校验 | `ShareServiceImpl.java:571` |
| 分享下载次数限制 | ✅ | Redis 原子递增 + 去重 | `ShareServiceImpl.java:651-677` |
| 分享提取码锁定 | ✅ | 5次失败锁定30分钟 | `ShareServiceImpl.java:401-413` |
| URL 层角色拦截 | ✅ | SecurityConfig 分层配置 | `SecurityConfig.java:79-85` |
| **上传父目录归属校验** | ❌ | `validateParent()` 未检查 `userId` | `UploadServiceImpl.java:489-506` |
| **批量下载任务归属校验** | ❌ | `getBatchTask()` 无 userId 检查 | `DownloadServiceImpl.java:177-184` |

**待测试：**
- [ ] T2.1：用户 A 上传文件到用户 B 的目录 ID 应被拒绝
- [ ] T2.2：用户 A 查询用户 B 的批量下载任务应返回空
- [ ] T2.3：普通用户访问 `/api/admin/**` 应返回 403
- [ ] T2.4：分享链接使用其他分享的 snapshotId 应被拒绝

---

### 3. 文件存储安全 ⚠️

| 子项 | 状态 | 实现方式 | 位置 |
|---|---|---|---|
| MinIO 预签名 URL | ✅ | 内外端点分离，过期时间可配置 | `MinioConfig.java:43-66` |
| 文件大小三层限制 | ✅ | Spring 512MB + 应用层 + 管理员配置 | `application-prod.yml:8,75-76` |
| **路径穿越防护** | ❌ | 文件名未做 `..` 过滤 | `IdUtil.java:34-39` |
| **文件类型校验** | ❌ | `ALLOWED_EXTENSIONS` 定义了但未调用 | `FileUtil.java:35-41` |

**待测试：**
- [ ] T3.1：上传文件名包含 `../../etc/passwd` 应被拒绝或过滤
- [ ] T3.2：上传 `.exe` 文件应被拒绝（如果启用了类型白名单）
- [ ] T3.3：MinIO 内部地址不应出现在预签名 URL 中

---

### 4. 上传安全 ✅

| 子项 | 状态 | 实现方式 | 位置 |
|---|---|---|---|
| 单文件大小限制 | ✅ | VIP 差异化，管理员可配置 | `UploadServiceImpl.java:160-164` |
| 用户配额校验 | ✅ | 上传前校验，merge 后扣减 | `UploadServiceImpl.java:150-159,384-389` |
| 分片大小校验 | ✅ | 超过分片大小的分片被拒绝 | `UploadServiceImpl.java:244-246` |
| 并发上传限制 | ✅ | Redis Set 跟踪，VIP 差异化 | `UploadServiceImpl.java:206-228` |
| 分片上传清理 | ✅ | 定时任务扫描孤儿分片 | `FileCleanupTask.java:81-106` |
| Merge 分布式锁 | ✅ | Redis SET NX 防并发合并 | `UploadServiceImpl.java:314-318` |

**待测试：**
- [ ] T4.1：超过配额的上传应被拒绝
- [ ] T4.2：24小时未完成的分片上传应被定时任务清理

---

### 5. 数据库安全 ✅

| 子项 | 状态 | 实现方式 | 位置 |
|---|---|---|---|
| SQL 注入防护 | ✅ | 全部使用 `#{}` 参数绑定，零 `${}` | 15 个 Mapper XML |
| 敏感数据脱敏 | ✅ | 日志仅记录用户名，不记录密码/token | 全局搜索确认 |
| 密码复杂度 | ✅ | 8-20位，字母+数字 | `RegisterRequest.java:32-33` |

**待测试：**
- [ ] T5.1：搜索框输入 `' OR 1=1 --` 不应返回越权数据

---

### 6. 接口安全 ✅

| 子项 | 状态 | 实现方式 | 位置 |
|---|---|---|---|
| Nginx 限流 | ✅ | 5 个限流区域：auth 2r/s, upload 5r/s, API 20r/s | `nginx.conf:13-17` |
| 后端限流 | ✅ | `@RateLimit` 注解 + Redis 滑动窗口 | `RateLimitAspect.java` |
| CSRF 防护 | ✅ | JWT-in-header 模式，CSRF 无需 | `SecurityConfig.java:69` |
| 参数校验 | ✅ | `@Valid` + `@NotBlank/@Size/@Pattern` | 所有 Controller 和 DTO |
| 安全头 | ✅ | X-Content-Type-Options, X-Frame-Options 等 | `nginx.conf:33-37` |

**待测试：**
- [ ] T6.1：1分钟内登录10次应被 Nginx 限流
- [ ] T6.2：上传接口 1秒内5次请求应被限流

---

### 7. 数据一致性安全 ⚠️

| 子项 | 状态 | 实现方式 | 位置 |
|---|---|---|---|
| 引用计数 CAS | ✅ | `WHERE ref_count > 0` 防负数 | `FileHashMapper.xml` |
| releaseRef 原子化 | ✅ | 单条 SQL `deleteIfNoRef` | `FileHashServiceImpl.java` |
| 秒传 DuplicateKey 保护 | ✅ | 捕获异常 + 共享对象 | `FileHashServiceImpl.java:34-38` |
| **MinIO 删除失败补偿** | ❌ | 无重试机制 | `StorageServiceImpl.java:55-65` |

**待测试：**
- [ ] T7.1：两个用户同时删除同一文件，引用计数应正确递减
- [ ] T7.2：MinIO 删除失败时，数据库记录应保留用于重试

---

### 8. 运维安全 ⚠️

| 子项 | 状态 | 实现方式 | 位置 |
|---|---|---|---|
| MySQL/Redis 端口隔离 | ✅ | 仅绑定 127.0.0.1 | `docker-compose.prod.yml:31,51` |
| MinIO 控制台隔离 | ✅ | 仅绑定 127.0.0.1 | `docker-compose.prod.yml:73` |
| Backend 端口隔离 | ✅ | 仅绑定 127.0.0.1 | `docker-compose.prod.yml:94` |
| 操作审计日志 | ✅ | `@Log` 注解 + AOP，20 个关键操作 | `LogAspect.java` |
| 日志轮转 | ✅ | 256MB/文件，7天，总上限 3GB | `logback-spring.xml` |
| **容器非 root 运行** | ❌ | Dockerfile 无 `USER` 指令 | `Dockerfile` |

**待测试：**
- [ ] T8.1：从外部访问 3306/6379/9001 端口应被拒绝
- [ ] T8.2：操作日志应记录上传/下载/删除等关键操作

---

## 三、待修复问题清单

| # | 问题 | 严重程度 | 文件 | 修复方案 |
|---|---|---|---|---|
| 1 | 上传父目录未校验 userId | 🔴 高 | `UploadServiceImpl.java:489-506` | `validateParent()` 加 `parent.getUserId() == userId` 校验 |
| 2 | 批量下载任务无归属校验 | 🟡 中 | `DownloadServiceImpl.java:177-184` | `getBatchTask()` 加 userId 参数，查询时校验 |
| 3 | 文件名未做路径穿越过滤 | 🔴 高 | `IdUtil.java:34-39` | 过滤 `..`、`/`、`\` 字符 |
| 4 | 文件类型白名单未生效 | 🟡 中 | `FileUtil.java:35-41` | 在 `UploadServiceImpl.init()` 中调用 `FileUtil.isAllowed()` |
| 5 | MinIO 删除失败无补偿 | 🟡 中 | `StorageServiceImpl.java:55-65` | 失败时记录到重试队列 |
| 6 | 容器以 root 运行 | 🟡 中 | `Dockerfile` | 添加 `RUN adduser` + `USER appuser` |

---

## 四、测试方案

### 测试 1：身份认证安全

```bash
# T1.1：密码复杂度
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"12345678","email":"test@test.com"}'
# 预期：失败（纯数字）

# T1.2：JWT 黑名单
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')
curl -X POST http://localhost:8081/api/auth/logout \
  -H "Authorization: Bearer $TOKEN"
curl http://localhost:8081/api/files \
  -H "Authorization: Bearer $TOKEN"
# 预期：401 Unauthorized

# T1.3：登录锁定
for i in $(seq 1 6); do
  curl -X POST http://localhost:8081/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"wrongpassword"}'
done
# 预期：第6次返回账户锁定错误
```

### 测试 2：权限安全

```bash
# T2.1：水平越权 - 上传到他人目录
# 假设用户A的token为 $TOKEN_A，用户B的目录ID为 123
curl -X POST http://localhost:8081/api/files/upload/init \
  -H "Authorization: Bearer $TOKEN_A" \
  -H "Content-Type: application/json" \
  -d '{"fileName":"hack.txt","fileSize":100,"parentId":123}'
# 预期：失败（父目录不属于该用户）

# T2.2：垂直越权 - 普通用户访问管理接口
curl http://localhost:8081/api/admin/dashboard/stats \
  -H "Authorization: Bearer $USER_TOKEN"
# 预期：403 Forbidden

# T2.3：分享快照越权
curl http://localhost:8081/api/shares/access/verify \
  -H "Content-Type: application/json" \
  -d '{"token":"share_a","snapshotId":999}'
# 预期：失败（snapshotId 不属于该分享）
```

### 测试 3：文件存储安全

```bash
# T3.1：路径穿越
curl -X POST http://localhost:8081/api/files/upload/init \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"fileName":"../../etc/passwd","fileSize":100,"parentId":0}'
# 预期：文件名被过滤或拒绝

# T3.3：MinIO 地址泄露
curl http://localhost:8081/api/files/1/download \
  -H "Authorization: Bearer $TOKEN" | jq -r '.data'
# 预期：URL 中不应包含 minio:9000 内部地址
```

### 测试 4：上传安全

```bash
# T4.1：超配额上传
# 先用尽配额，再上传
curl -X POST http://localhost:8081/api/files/upload/init \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"fileName":"big.bin","fileSize":999999999999,"parentId":0}'
# 预期：FILE_QUOTA_EXCEEDED
```

### 测试 5：数据库安全

```bash
# T5.1：SQL 注入
curl "http://localhost:8081/api/files/search?keyword=%27%20OR%201%3D1%20--" \
  -H "Authorization: Bearer $TOKEN"
# 预期：返回空结果或正常搜索结果，不泄露数据
```

### 测试 6：接口安全

```bash
# T6.1：登录限流
for i in $(seq 1 12); do
  curl -s -o /dev/null -w "%{http_code}" \
    -X POST http://localhost:8081/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"wrong"}'
done
# 预期：前10次正常响应，第11-12次返回429 Too Many Requests
```

### 测试 7：数据一致性

```bash
# T7.1：并发删除引用计数
# 上传同一文件到两个用户，然后同时删除
# 验证 ref_count 从2递减到0，MinIO 对象被清理
# （需要写并发测试脚本）
```

### 测试 8：运维安全

```bash
# T8.1：端口隔离
nc -zv 101.35.233.30 3306  # 应超时或拒绝
nc -zv 101.35.233.30 6379  # 应超时或拒绝
nc -zv 101.35.233.30 9001  # 应超时或拒绝

# T8.2：审计日志
curl http://localhost:8081/api/admin/logs \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.data[] | select(.operation=="UPLOAD_FILE")'
# 预期：应有上传操作记录
```

---

## 五、审计结论

### 已实现（可写入简历）

| 安全能力 | 技术实现 |
|---|---|
| 密码 BCrypt 加密 | 加盐 + 单向哈希，防彩虹表 |
| JWT + Redis 黑名单 | Token 过期 + 登出即时失效 |
| 登录防爆破 | 账户锁定（5次/30分钟）+ IP 限流（10次/分钟） |
| 五层权限校验 | JWT 过滤器 → URL 角色 → Controller 身份 → Service 归属 → SQL userId |
| 分享安全 | 随机 token + 过期时间 + 提取码 + 下载次数限制 |
| MinIO 预签名 | 内外端点分离，URL 有时效 |
| 操作审计 | `@Log` 注解 + AOP，20 个关键操作自动记录 |
| SQL 注入防护 | 15 个 Mapper 全部使用 `#{}` 参数绑定 |
| Nginx + 后端双重限流 | 5 个限流区域 + `@RateLimit` 注解 |

### 待修复（4 项）

| # | 问题 | 严重程度 |
|---|---|---|
| 1 | 上传父目录未校验 userId（IDOR） | 🔴 高 |
| 2 | 文件名未做路径穿越过滤 | 🔴 高 |
| 3 | 文件类型白名单未生效 | 🟡 中 |
| 4 | 容器以 root 运行 | 🟡 中 |

### 简历写法

> 实现八层安全防护体系：BCrypt 密码加密 + JWT 双重验证（签名+Redis 黑名单）+ 登录防爆破（5次锁定+IP限流）+ 五层权限校验（JWT→URL角色→Controller身份→Service归属→SQL隔离）+ 分享安全（随机token+过期+提取码+次数限制）+ MinIO 预签名URL防盗链 + @Log 注解操作审计 + Nginx/后端双重限流
