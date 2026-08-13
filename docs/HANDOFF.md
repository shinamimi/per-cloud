# HANDOFF — 云服务器部署（部署进行中，已可验收）

## Objective
将 Cloud 云盘（Spring Boot 4.0.7/Java21 后端 + Vue3 前端 + MySQL8.4/Redis7.2/MinIO）部署到腾讯云轻量 2C2G 服务器 `101.35.233.30`，并完成基础部署测试与验收。

## 当前状态：✅ 已成功启动，进入验收阶段

5 个容器全部 Up，后端健康：

```
cloud-backend   Up（Tomcat started on port 8081, Started in ~16s）
cloud-frontend  Up（0.0.0.0:80）
cloud-mysql     Up (healthy)
cloud-redis     Up (healthy)
cloud-minio     Up (healthy)
```

## 关键账号/凭证（生产环境）

- 服务器 SSH：`ubuntu@101.35.233.30`（key 免密已配，`BatchMode=yes` 可直连）
- 超管登录：`root` / `K5dRR2FHzUrgXmOb`（登录接口 `POST /api/auth/login`，已验证返回 token，role=100）
- MySQL：cloud / `Gq2lLB5Qjgh7k7JNOWCkv8`；root `Y9dcD1t9NdDCcTSHbeBb0v`
- MinIO：minioadmin / `FowOGFdt5shoS2x7setvVd`；`MINIO_ENDPOINT=http://minio:9000`（内部）、`MINIO_PUBLIC_URL=http://101.35.233.30:9000`（公网）
- JWT_SECRET / SMTP 沿用根 `.env`

## 服务器现状

- 部署根目录：`~/cloud/`（含 docker-compose.prod.yml、docker/、sql/、backend/、frontend/）
- Docker 29.7.2，镜像加速已配（daemon.json），Swap 2.0G
- 端口映射：公网 80（frontend Nginx 反代 /api 与 /ws）、公网 9000（MinIO presigned，**已放行**）、127.0.0.1:8081（backend，仅本机）

## 待办（Next Move）

1. 验收（参照 `docs/DEPLOYMENT.md` §6/§9.3）：浏览器访问 `http://101.35.233.30/` 登录 root、上传/下载文件、建团队、检查安全头与限流；**重点回归图片预览 & 分享链接复制**（本次已修，见下）
2. 将本次修复同步回本地仓库：`sql/init-full.sql` 已同步；服务器 `docker/prod-backend.env` 的 `MINIO_ENDPOINT`/`SERVER_PORT` 改动**未同步**回本地 `docker/prod-backend.env`（该文件 gitignore，但本地仍有副本需对齐）
3. **HTTPS 上线**（默认推荐）：域名 `szfanrongkj.com` 已解析到本机，接入证书（选项：1Panel OpenResty 反代，见 `docs/migrate-1panel-https.md`）后可顺带解除 `crypto.subtle`/`navigator.clipboard` 的降级包袱

## 本次部署排障修复（详见 docs/bugs/）

BUG 6（`docs/bugs/BUG_FIXES_6.md`）：
1. `t_user` 缺 `is_vip`/`admin_bonus_quota`/`reward_quota` 三列 → 线上 ALTER + `sql/init-full.sql` 补齐（曾致后端崩溃重启 93 次）
2. `MINIO_ENDPOINT` 误用公网 IP → 改 `http://minio:9000`（内部网络）
3. 应用监听 8080 vs compose/nginx 用 8081 → 注入 `SERVER_PORT=8081`
4. env 文件末尾 `TZ` 无换行导致追加变量被吞（`TZ=Asia/ShanghaiSERVER_PORT=8081`）

BUG 7（`docs/bugs/BUG_FIXES_7.md`）：公网 HTTP 非安全上下文 `crypto.subtle` undefined → 上传 SHA256 加降级，已部署。

BUG 8（`docs/bugs/BUG_FIXES_8.md`）：
1. prod 配置缺 `file.upload-expire-hours` → 上传 TTL=0 报 `UPLOAD_NOT_FOUND` → 补全 file/quota/mail.from
2. 公网 HTTP 下 `navigator.clipboard` undefined → 复制失效 → 新增 `frontend/src/utils/clipboard.ts`（原生+`execCommand('copy')` 降级），三处调用已切换，前端已重建部署
3. **图片预览空白 → presigned URL host 双重根因**：（a）URL host 是容器内网名 `minio:9000`，不在 CSP 白名单且浏览器不可达；（b）S3 v4 签名把 host 签死（`SignedHeaders=host`），字符串替换 host 必 403 → 根治法：`MinioConfig` 新增 `presignMinioClient`（endpoint=`minio.public-url`）专用于生成 presigned URL，数据面仍走内网 `minioClient`；另确认防火墙 TCP 9000 已放行。公网 presigned 实测 200 并下载到真实 JPEG

## 排障经验（防再踩）

- 后端日志写 `/app/logs/application.log`（文件）而非 stdout → `docker logs` 只有 Banner，务必 `docker exec cloud-backend cat /app/logs/application.log` 看真实错误
- 2C2G 下内存配额已按 800m/200m/640m/700m/128m 分配，无需调整
- 容器间互访用服务名（minio/mysql/redis），公网 IP 只给浏览器侧 URL
