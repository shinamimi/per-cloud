# Cloud 云盘 — 腾讯云轻量应用服务器部署方案

> 版本: v1.0
> 更新日期: 2026-08-11
> 目标服务器: Ubuntu 22.04 LTS（可重装系统），2 核 2G，50G 磁盘

---

## 1. 服务器现状与用途判断

对现有服务器巡检结果的判断：

| 项 | 现状 | 判断 |
| --- | --- | --- |
| 系统 | Ubuntu 22.04.5 LTS, 5.15 内核 | 达标，可作为部署基座 |
| CPU/内存 | 2C；总内存 1.9Gi，**可用仅 458Mi**（无 Swap） | 内存紧张，但容器化限流后可容纳 |
| 磁盘 | 50G，已用 14G，可用 34G | 满足（后端 jar ~100MB + 数据卷） |
| 已装软件 | **1Panel 面板** + OpenResty + WordPress(kian) + MySQL8.4容器 + Bark 推送 + 宿主机 mysqld + Apache | 当前是**个人博客/推送站**（`szfanrongkj.com`），与云盘无关 |
| 开放端口 | 22/80/443/3306/8080/8088/8090 | 端口被博客占用，需清理后交给云盘 |
| Nginx | OpenResty（非系统 nginx），反代 80/443 → WordPress | 托管 `szfanrongkj.com` 域名与 SSL |

**结论**：这台服务器目前承载 WordPress 博客 + Bark，不承载云盘。用户已确认**可以重装系统**，因此按"重装为干净 Ubuntu 22.04 → 只跑云盘"规划。

---

## 2. 部署条件评估

### 2.1 已具备的条件（无需改动）

| 项 | 现状 |
| --- | --- |
| 生产 profile | `application-prod.yml` 已将所有敏感配置切换为环境变量 `${VAR}`，无硬编码密钥 |
| 三件套编排 | 开发环境 `docker-compose.yml` 已有 MySQL8.4/Redis7.2/MinIO + healthcheck + 持久化卷 |
| 后端构建 | `./mvnw package` 已产出可执行 jar（`backend/target/*.jar`，Spring Boot 4.0.7/JDK21） |
| 前端构建 | `npm run build` 已产出 `frontend/dist`，API 走相对路径（`VITE_API_BASE_URL` 留空），**同域反代即可，无需跨域与改码** |
| WebSocket | 前端 `ws.ts` 连接相对地址 `/ws/progress`，同域反代可通 |
| 超级管理员 | `SuperAdminInitializer` 首次启动自动创建，凭据走环境变量 |

### 2.2 部署前必须修复的阻塞项

| # | 阻塞项 | 说明 | 修复 |
| --- | --- | --- | --- |
| 1 | **`t_team`/`t_team_member` 无建表语句** | 全库脚本只有 5 基础表 + 6 张新表；migration 只对 t_team 做 `ALTER`（假设已存在），团队模块全新部署必失败 | **新增 `sql/init-full.sql`**：合并 schema+migrations 为最终结构并补齐这两张表 |
| 2 | **migration 顺序不可全新执行** | `docker-entrypoint-initdb.d` 按文件名排序：`migration-*.sql` 在 `schema.sql` 前执行，先 `ALTER` 不存在的列/表直接崩 | 生产中**只挂 `init-full.sql`**，不再执行散装 migration |
| 3 | **MinIO presigned URL 直连问题** | 下载/预览走 `getPresignedObjectUrl` 生成的内部 URL（`http://minio:9000/...`），公网浏览器无法访问 | `MINIO_ENDPOINT`/`MINIO_PUBLIC_URL` 配成浏览器可达的公网地址，且腾讯云安全组放行 TCP 9000 |

### 2.3 判定

**补齐上面 3 项后即达到可部署条件**，本次配套文件：`sql/init-full.sql`、`docker-compose.prod.yml`、`backend/Dockerfile`、`frontend/Dockerfile`、`frontend/nginx.conf`、`docker/prod.env`、`docker/prod-backend.env`、`docs/DEPLOYMENT.md`。

---

## 3. 资源规划（2C2G 适配）

| 服务 | 内存限制 | CPU 限制 | 说明 |
| --- | --- | --- | --- |
| MySQL 8.4 | 800m | 1.0 | `innodb-buffer-pool-size=256M`，性能模式关闭，max_connections=100 |
| Redis 7.2 | 200m | 0.2 | AOF 开启，`maxmemory 128mb allkeys-lru` |
| MinIO | 640m | 0.8 | 单机模式，9000 API 公网 / 9001 Console 内网 |
| Backend | 700m | 1.0 | JVM `-Xms256m -Xmx512m -XX:+UseSerialGC` |
| Frontend(Nginx) | 128m | 0.2 | 静态托管 + `/api` + `/ws` 反代 |
| **合计** | **~2.5G（限制值，实际用量更小）** | 3.2 芯 | 2C2G 可承载，建议另加 **2G Swap 兜底** |

> 内存限制值之和略超 2G 是"上限"而非"常驻"，实际各容器按需占用；若担心 OOM，优先 `swapoff` 前先 `fallocate -l 2G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile`。

---

## 4. 部署拓扑

```
                           ┌─────────────────────────────┐
                           │      云服务器 2C2G           │
 │ :80 ──▶ cloud-frontend(Nginx)                         │
 │                         ├── /         → dist 静态      │
 │       前端 docker       ├── /api/     → backend:8081   │
 │                         └── /ws/      → backend:8081   │
 │                                                        │
 │       后端 docker       ├── backend (Spring Boot :8081)│
 │                        ├── mysql:3306（127.0.0.1 映射）│
 │                        ├── redis:6379（127.0.0.1 映射）│
 │   :9000 ──▶ minio     ← 浏览器直连 presigned URL        │
 └─────────────────────────────┘
```

- 唯一公网入口：**80**（前端）；**9000**（MinIO API），腾讯云安全组放行这两个
- MySQL/Redis/MinIO Console/Backend 仅映射 127.0.0.1，不对外
- 浏览器访问下载/预览：前端拿到后端返回的 `http://公网IP:9000/...` presigned URL 直连 MinIO

---

## 5. 部署步骤（重装后执行）

> 下述命令在**干净 Ubuntu 22.04 + 已装 Docker/Docker Compose** 前提下执行。本地先改两处占位再打包上传。

### 5.0 本地准备

```bash
# 1) 替换占位（必做）
#   docker/prod-backend.env：
#     MINIO_ENDPOINT / MINIO_PUBLIC_URL  ← 公网IP 或 你的域名
#     JWT_SECRET                          ← openssl rand -base64 64
#     MAIL_PASSWORD                       ← SMTP 应用授权码
#     SUPER_ADMIN_PASSWORD                ← 强密码
#   docker/prod.env：MySQL/MinIO 强密码

# 2) 本地构建产物
cd backend && ./mvnw -DskipTests package && cd ..
cd frontend && npm run build && cd ..

# 3) 打包上传（排除 .git / node_modules / target 里的过程文件）
tar czf cloud-deploy.tgz \
    docker-compose.prod.yml docker sql backend/Dockerfile \
    frontend/Dockerfile frontend/nginx.conf frontend/dist \
    backend/target/cloud-backend-0.0.1-SNAPSHOT.jar \
    docker/prod.env docker/prod-backend.env
scp cloud-deploy.tgz root@<公网IP>:/opt/
```

### 5.1 服务器初始化

```bash
# 解包到 /opt/cloud
cd /opt && tar xzf cloud-deploy.tgz -C /cloud

# 确认 Docker 就绪（重装后可手动 `apt install docker.io docker-compose-v2` 或一键脚本）
docker --version && docker compose version

# 建议加 2G Swap 兜底
fallocate -l 2G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab
```

### 5.2 一键构建启动

```bash
cd /cloud
docker compose -f docker-compose.prod.yml up -d --build

# 查看健康状态
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f backend
```

首次启动流程：MySQL 容器初始化自动执行 `init-full.sql`（建库 cloud + 13 表）→ healthcheck 通过 → 后端连接三件套 → `SuperAdminInitializer` 建超管 → 前端静态可访问。

### 5.3 腾讯云控制台

- 安全组放行：**80**、**9000**（与本地端口探查一致；如 5.2 前需临时 22）
- 若以后要 HTTPS：绑定域名 + 443，把证书挂到 frontend 容器（本期不做）

---

## 6. 验收清单

| # | 验收项 | 命令/动作 | 期望 |
| --- | --- | --- | --- |
| 1 | MySQL 就绪且含 13 表 | `docker exec cloud-mysql mysql -uroot -p -e 'USE cloud; SHOW TABLES;'` | 13 张表齐全（含 t_team、t_team_member） |
| 2 | 后端健康 | `curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8081/api/auth/login` | 返回 4xx（登录 POST，验证服务存活）而非连接失败 |
| 3 | 前端可访问 | 浏览器 `http://公网IP/` | 登录页渲染 |
| 4 | 注册登录 | 页面注册（邮件验证码若 SMTP 正常）→ 登录 | 成功进入主界面 |
| 5 | 文件上传/下载/预览 | 上传小文件 → 列表可见 → 下载/预览 | MinIO 9000 直链可开 |
| 6 | 团队功能 | 创建团队 → 建目录上传 → 成员管理 | 正常（验证 t_team/t_team_member） |
| 7 | 重启持久化 | `docker compose -f docker-compose.prod.yml restart` | 数据仍在（Volume 持久化） |
| 8 | 管理后台 | 超管登录 → `/api/admin/**` | 仪表盘/用户/文件管控可进 |

---

## 7. 常见问题与回退

| 问题 | 处理 |
| --- | --- |
| 后端起不来 / 连不上 MySQL | `docker compose logs mysql` 看是否报 13 表缺失；`logs backend` 看连接串 |
| MinIO 下载链接打开 404 | 安全组未放行 9000，或 `MINIO_ENDPOINT` 不是公网可达地址 |
| 内存 OOM | 看 `docker stats`，按 §3 收紧 `mem_limit` 或加 Swap |
| 想改 Host 端口 | 编辑 `docker-compose.prod.yml` 映射段后 `docker compose -f ... up -d` |
| 回滚到旧版 | 数据在 volume（mysql-data/redis-data/minio-data），重建镜像不丢数据 |

---

## 8. 后续方向（本期不做）

- HTTPS：绑定域名 + Let's Encrypt 证书，frontend 容器挂 443
- 备份：`mysqldump` 定时 + MinIO 数据卷快照
- 灰度：把 `frontend` 从 80 拆到 443，保留 80 跳转
- 监控：`docker stats` + 日志轮转（logback-spring.xml 已按日滚动）

---

## 9. 安全防护现状（单机架构）

### 9.1 已覆盖

| 层面 | 防护 | 实现 |
| --- | --- | --- |
| 认证 | 登录失败锁定 | `LoginAttemptService`：Redis 计数，5 次失败锁 30 分钟（可配置） |
| 认证 | 验证码防轰炸 | `CaptchaService`：验证码 60 秒冷却、5 分钟有效、一次性 |
| 认证 | 密码存储 | BCrypt 加密 |
| 授权 | 角色分级 | Spring Security：USER/OPERATOR/ADMIN/SUPER_ADMIN，路径前缀收敛 |
| 上传 | 并发限制 | `UploadServiceImpl` 按用户并发任务上限（普通 3 / VIP 5） |
| 上传 | 大小限制 | 后端 multipart 512M（prod 已补配置），单文件上限可配置 |
| 内容 | 违规文件拦截 | `t_disabled_object` 按内容 hash 全站/单用户禁用 |
| 网络 | 端口收敛 | MySQL/Redis/MinIO Console 只映射 127.0.0.1，仅 80/9000 对外 |
| 网络 | 内网密码 | prod.env/prod-backend.env 强随机密码（已替换占位） |
| 边缘 | 限流 | Nginx `limit_req`：API 20r/s、登录/验证码 2r/s、上传 5r/s、WS 2r/s、静态 50r/s（均按 IP） |
| 边缘 | 请求体限制 | Nginx `client_max_body_size 520m`，超限 413 |
| 边缘 | 超时兜底 | Nginx proxy/client/send 超时，防慢速连接拖资源 |
| 边缘 | 隐藏版本 | `server_tokens off` |
| 边缘 | 安全响应头 | X-Frame-Options / CSP / X-Content-Type-Options / Referrer-Policy |
| 边缘 | 爬虫基础拦截 | 已知采集 UA 返回 403 + robots.txt 禁收录 |
| 边缘 | 静态缓存/压缩 | assets 强缓存一年 + gzip |

### 9.2 已知局限（本期接受）

| 项 | 说明 | 建议 |
| --- | --- | --- |
| HTTPS 缺失 | 80 明文 + CSP 无 HSTS；浏览器提示"不安全" | 上域名后启用 443（见 §8） |
| 网络层 DDoS | 2C2G 扛不住大流量 SYN/UDP flood | 依赖腾讯云安全组 + 轻量自带 DDoS 基础防护，必要时开大禹 |
| 应用层 DoS | 后端无熔断/线程池隔离，恶意并发可能打满 CPU | 单实例够用，扛不住再横向扩容 |
| 登录验证码开关 | 登录验证码由管理端配置，默认可能关闭 | 部署后管理端确认开启 |
| CORS 全放行 | 允许任意来源（Token 认证，风险低） | 有域名后收紧为白名单 |
| MinIO 9000 公网 | 直接暴露对象存储 API | 有域名后改 Nginx 反代 `/minio` |
| 无备份 | 数据卷在本地磁盘 | 上线前配 mysqldump 定时任务 |

### 9.3 部署验收时顺手确认

```bash
# 1. 端口暴露面：应只有 80 / 9000 / 22（22 可关或改高端口）
ss -ltn | grep -E ':(80|443|9000|9001|8081|3306|6379)'

# 2. 限流生效（连续打登录接口，应出现 503）
for i in $(seq 1 10); do curl -s -o /dev/null -w "%{http_code} " -X POST http://101.35.233.30/api/auth/login; done; echo

# 3. 安全头存在
curl -sI http://101.35.233.30/ | grep -iE "x-frame|content-security|x-content"
```