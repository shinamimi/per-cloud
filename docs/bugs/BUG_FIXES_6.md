# Bug Fix Log 6

## 2026-08-13 — 云服务器部署后后端容器崩溃循环重启（93 次），产物含 3 个叠加根因

**现象：** 腾讯云轻量 2C2G 部署（Docker Compose，`docker-compose.prod.yml`）`up -d` 后，5 个容器中仅 `cloud-backend` 反复崩溃重启（`RestartCount=93`，`ps` 显示 `Up 20 秒`），其余 4 个容器均 healthy。`docker logs` 只有 Spring Boot Banner、无任何异常堆栈，易误判为 OOM。

**排查路径（关键）：** ① `docker inspect` 确认 `OOMKilled=false`、`ExitCode=0`+`dmesg` 无 OOM → 排除内存问题；② **日志实际写入了 `/app/logs/application.log` 文件而非 stdout**，`docker logs` 看不到真实错误 → 改 `docker exec cloud-backend cat /app/logs/application.log` 才暴露堆栈。

**根因（三个叠加，按暴露顺序）：**

1. **`t_user` 表缺 `is_vip`/`admin_bonus_quota`/`reward_quota` 三列。** `SuperAdminInitializer` 启动时调用 `UserServiceImpl.register` 种超管，INSERT 命中 mapper 里的三列，报 `java.sql.SQLSyntaxErrorException: Unknown column 'is_vip'` → 初始化失败 → 应用退出。**建设 localStorage 的 `sql/init-full.sql` 建表脚本比代码 Schema 落后：`User.java`(entity) 与 `UserMapper.xml` 已含三列，而 `init-full.sql` 的 `CREATE TABLE t_user` 未包含。**
2. **`MINIO_ENDPOINT` 配置成公网 IP。** 后端容器内访问 `101.35.233.30:9000` 超时（`ConnectException: Connection timed out`）；容器间本应走 Docker 内部网络 `http://minio:9000`（实测内部 `minio:9000` 返回 200 正常）。`MINIO_PUBLIC_URL`（给浏览器生成 presigned URL）才应填公网地址。
3. **端口不匹配：应用监听 8080，compose 映射与 nginx 反代均写 8081。** `application-prod.yml` 没有显式设置 `server.port`（默认 8080），而 `docker-compose.prod.yml` 映射 `127.0.0.1:8081:8081`、`frontend/nginx.conf` 的 `proxy_pass` 全部指向 `backend:8081` → 后端 8081 无监听。中途还暴露第 4 个小坑：往 `prod-backend.env` 追加 `SERVER_PORT=8081` 时，因文件末尾 `TZ=Asia/Shanghai` 无换行，变量被拼接成 `TZ=Asia/ShanghaiSERVER_PORT=8081`，`SERVER_PORT` 未生效。

**修复：**

| 文件 | 变更 |
|---|---|
| `sql/init-full.sql` | `t_user` 建表补齐 `is_vip TINYINT NOT NULL DEFAULT 0`、`admin_bonus_quota BIGINT NOT NULL DEFAULT 0`、`reward_quota BIGINT NOT NULL DEFAULT 0` |
| 线上库（ALTER TABLE） | `docker exec cloud-mysql` 对 `t_user` 执行 `ALTER TABLE ... ADD COLUMN` 补上同三列（本地 SQL 文件未改前的兜底） |
| `docker/prod-backend.env`（服务器） | `MINIO_ENDPOINT` 由 `http://101.35.233.30:9000` 改为 `http://minio:9000`；`MINIO_PUBLIC_URL` 保持公网地址 |
| `docker/prod-backend.env`（服务器） | 追加 `SERVER_PORT=8081`（注意先确保 `TZ=Asia/Shanghai` 行尾有换行，否则变量被吞） |

> 说明：`docker-compose.prod.yml`、`frontend/nginx.conf`、`local 的 prod-backend.env` 用 8081 是部署约定；修复方向是让应用监听 8081 对齐约定，而非把三处改成 8080。`application-prod.yml` 未显式写 `server.port`，靠环境变量 `SERVER_PORT=8081` 注入，与本地开发 8080 解耦。

**验证：** 全部修复后 `docker compose -f docker-compose.prod.yml up -d --force-recreate backend`，日志出现 `Tomcat started on port 8081` 与 `Started CloudBackendApplication in 16.168 seconds`；`MinIO bucket [cloud-storage] created` 无报错；`curl` 验证 backend:8081 / frontend:80 / minio:9000 均 200；公网 `POST http://101.35.233.30/api/auth/login`（root）返回 `{"code":200,"token":"..."}`，JWT 签发正常。

**遗留说明：**
- 腾讯云控制台防火墙需放行 **9000** 端口，公网 presigned URL（`101.35.233.30:9000`）才能直连（当前公网 9000 仍超时，80 已通）。
- 根因 1 的隐性风险仍在：`sql/init-full.sql` 属"人工维护的建表脚本"，与 MyBatis 实体/Mapper 无编译期校验，后续 Schema 变更需在文档标注同步点（可考虑引入 Flyway/Liquibase 管理迁移）。
- `docker logs` 看不到后端日志是因为应用日志写文件不写 stdout；排障时直接读容器内 `/app/logs/application.log`。