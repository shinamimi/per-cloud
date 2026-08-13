# 迁移到 1Panel + 域名 HTTPS 反代方案（2026-08-13）

> 目标：在腾讯云轻量（101.35.233.30）上安装 1Panel，把已在跑的 Cloud 云盘（5 容器 Compose）纳入面板管理，并用 1Panel 的 OpenResty 接域名 + 自动 HTTPS 证书，解决 `crypto.subtle is undefined`（HTTP 非安全上下文禁用）等问题。
>
> 关键事实：**1Panel 本身就是 Docker 面板，不放弃 Docker、不动现有容器**，只是换管理入口与出口反代。

---

## 0. 现状盘点

| 项 | 当前 | 说明 |
|---|---|---|
| 部署方式 | 裸 Docker Compose | `~/cloud/docker-compose.prod.yml`，5 容器（mysql/redis/minio/backend/frontend） |
| 入口 | frontend 容器 Nginx :80 | 托管静态 + 反代 /api、/ws 到 backend |
| 域名 | 无 | 直接用 IP `http://101.35.233.30` |
| HTTPS | 无 | → 浏览器禁用 `crypto.subtle`（SHA256 秒传哈希挂掉） |
| MySQL 3306 | 127.0.0.1 映射 | 与 1Panel 自带 MySQL 冲突点 |
| 80/443 | frontend 占 80；443 空 | 1Panel OpenResty 要占 80/443 |

---

## 1. 方案总览（两条路线二选一）

### 路线 A：仅反代上 1Panel（推荐，改造最小）
- 1Panel 只用来做「域名 + HTTPS 反代 + 证书」
- Cloud 的 5 容器**保持裸 Compose 不动**，`frontend` 从 80 挪到内网端口（如 127.0.0.1:8080），OpenResty 对域名 80/443 → 127.0.0.1:8080
- 优点：Cloud 部署文件零迁移、不动 MySQL/Redis/MinIO；缺点：Compose 不在面板 UI 里看
- 之后若想用面板管理容器，再走路线 B 导入 Compose

### 路线 B：Compose 整体导入 1Panel
- 在 1Panel GUI「容器 → Compose」新建，粘贴 `docker-compose.prod.yml` 内容，由面板接管启停/日志/监控
- 需处理**端口冲突**：先停掉裸 Compose 的 5 容器、释放 3306/80/9000，或给 Cloud 的 MySQL 改映射（见 §5）
- 优点：面板统一管理 + 反代；缺点：迁移时要中断访问、多一轮验证

> 下文按**路线 A** 为主编写（改动小），路线 B 的差异点在 §5 说明。

---

## 2. 前置准备：域名

1. 购买域名（腾讯云/阿里云均可，`.com` 约 ¥60+/年，顺手用腾讯云")
2. 在域名服务商 DNS 添加解析记录到 `101.35.233.30`：
   - `A   @       101.35.233.30`
   - `A   www     101.35.233.30`
3. 若想用 MinIO 子域，再加：`A   minio   101.35.233.30`

---

## 3. 安装 1Panel

```bash
# SSH 登录服务器（授权 1Panel 需 root，普通 SSH 用户执行不了）；腾讯云轻量默认 ubuntu 无 root 密码，先 sudo su -
curl -sSL https://install.1panel.cn | sh
```

1. 安装脚本会给出面板地址与管理入口端口（**默认 8090**，与以前一致）
2. 初始化设置：admin 密码、安全入口（建议随机路径）
3. 面板防火墙页放行：80、443、8090（若 1Panel 的 ufw 默认关闭则跳过）
4. **注意**：1Panel 默认动作不会动 3306/8081（那些是 Docker 端口，非面板端口）

---

## 4. 1Panel 反向代理 + HTTPS 证书（核心）

### 4.1 把 Cloud frontend 从 80 挪到内网端口（先做，否则端口被抢占）

编辑服务器 `~/cloud/docker-compose.prod.yml`：

```yaml
  frontend:
    ports:
      - "127.0.0.1:8080:80"   # 由公网 80 改为仅本机 8080，反代由 1Panel OpenResty 出口
```

重建生效：

```bash
cd ~/cloud
docker compose -f docker-compose.prod.yml up -d --force-recreate frontend
# 验证：本机 curl http://127.0.0.1:8080/ 返回 200
```

> 此时公网 80 暂时空出（curl http://101.35.233.30/ 会失败），完成 §4.3 前别访问。

### 4.2 1Panel 建网站反代

1Panel 操作：**网站 → 网站 → 创建网站 → 反向代理**
- 域名：`szfanrongkj.com`，勾选 `www`（或你的新域名）
- 反代地址：`http://127.0.0.1:8080`
- 伪静态：默认即可
- 创建后面板自动生成 Nginx 配置段

### 4.3 自动 HTTPS 证书

1Panel：**证书 → Let's Encrypt（内置 ACME）→ 申请**
- 选域名，验证方式 DNS 或 Web（1Panel 自动在站点目录放验证文件，选 Web 即可）
- 申请成功后在「网站 → 反代站 → HTTPS」勾选强制跳转，443 启用

验证：

```bash
curl -sI https://szfanrongkj.com/ | head -3        # 应返回 200 与 HTTPS
curl -s -o /dev/null -w "%{http_code}" https://szfanrongkj.com/api/auth/login
```

### 4.4 MinIO 9000 公网处理（二选一）

- **保持直连公网 IP:9000**：腾讯云安全组放行 9000（已在做）。但页面已 HTTPS，浏览器从 HTTPS 页面连 `http://101.35.233.30:9000` 属 **mixed content 会被拦**（除非 MinIO 也走 HTTPS）→ 不推荐与 HTTPS 并存。
- **MinIO 也走域名子域**（推荐）：配置一个 `minio.szfanrongkj.com` 反代指向 `http://127.0.0.1:9000`（OpenResty 加一份同款证书），改后端 `MINIO_PUBLIC_URL=https://minio.szfanrongkj.com`（`MINIO_ENDPOINT` 保持内网 `http://minio:9000` 不变）。

### 4.5 后端可信代理头（HTTPS 后必须）

Nginx`proxy_set_header X-Forwarded-Proto $scheme` 已存在，确认后端能读到；若后端有判断 `secure`（cookie 的 secure 属性 / OAuth 回调），需在 `application-prod.yml` 开启 proxy headers 信任。**检查点见 §6 验收 ④。**

---

## 5. 路线 B 差异点（整体导入面板时）

1. 停现有 Compose：`docker compose -f docker-compose.prod.yml down`（数据在 named volume，不会丢）
2. 1Panel「容器 → Compose → 新建」，粘贴 `docker-compose.prod.yml` 内容（编译型 backend/frontend 用 `build`，面板需能访问构建上下文，见 3）
3. **端口冲突**（1Panel 自带 mysql、openresty 会预占）：
   - 3306：Cloud MySQL 改为 `127.0.0.1:3307:3306`，同步改 `MYSQL_URL` 中端口？——**不行**，容器间走内网 `mysql:3306`，改主机映射即可，内网名不变，无需改 backend env
   - 80：Cloud frontend 改 `127.0.0.1:8080:80`（同路线 A §4.1）
   - 9000：由 1Panel OpenResty 反代 minio 子域后，可把 minio 改 `127.0.0.1:9000:9000` 收内网
   - 8090：1Panel 面板端口，无需动
4. 面板会建独立网络，与裸 Compose 的 `cloud-network` 不冲突；注意面板重命名网络的坑：容器名 `container_name` 已有显式名字，保持即可

---

## 6. 验收清单

① 浏览器 `https://szfanrongkj.com/` 打开，控制台无 mixed content 报错
② `crypto.subtle` 报错消失 → 上传一个文件看进度条走完（哈希阶段即用 SHA256）
③ `POST https://szfanrongkj.com/api/auth/login`（root）返回 token
④ 行为不变：WS 进度推送（HTTPS 下自动 wss，见 `ws.ts resolveWsUrl` 已做派生）、上传大文件（此前 520M body 限制经 OpenResty 需确认其 `client_max_body_size` 或从前端 body 限制兜底）
⑤ `curl -sI` 检查安全头仍在（OpenResty 默认不含你的 CSP，需把 `frontend/nginx.conf` 的 add_header 段复制到 1Panel 反代配置或保留部分在 frontend 层）
⑥ 限流仍生效：OpenResty 不继承你 frontend 的 limit_req，需决定限流放哪一层（保留 frontend 层最稳）

---

## 7. 遗留风险与待办

| 项 | 状态 |
|---|---|
| 腾讯云安全组放行 9000 | 若走 MinIO 子域反代则不必放行（收内网更安全） |
| `docker/`、`sql/`、`docker-compose.prod.yml` 已 gitignore | 新域名/证书不进仓库 |
| 服务器 `docker/prod-backend.env` 已改 `MINIO_ENDPOINT=http://minio:9000` + `SERVER_PORT=8081` | **本地 `docker/prod-backend.env` 副本未同步**，需在仓库里对齐（gitignore 文件但本地副本差异，注意别提交） |
| `t_user` 三列修复 | 已同步 `sql/init-full.sql`，见 `docs/bugs/BUG_FIXES_6.md` |

---

## 8. 参考

- 1Panel 官网：https://www.1panel.cn
- 本次部署排障：`docs/bugs/BUG_FIXES_6.md`
- 部署基线命令与安全现状：`docs/DEPLOYMENT.md`