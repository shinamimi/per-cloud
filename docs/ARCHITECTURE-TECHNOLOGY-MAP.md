# 技术栈架构全景与差距评估（本应如何 → 我们现状 → 接近什么）

> 目的：每个候选技术按架构定位写清「本应如何」「我们项目目前」「接近程度」，用于规划引入顺序与简历表述。实测依据：`backend/pom.xml`、`frontend/package.json`、服务器部署现状（2026-08-13）。

标注：★=学习价值高建议尽早｜◐=单机暂用不上，了解即可

---

## 0. 已落地骨架

| 层 | 技术 | 说明 |
|---|---|---|
| 后端 | Java 21 / Spring Boot / Spring Security / MyBatis / Redis / MinIO / WebSocket / JWT | 生产运行中 |
| 前端 | Vue3 / TS / Pinia / Vue Router / Element Plus / Axios | 生产运行中 |
| 部署 | Docker Compose / Nginx / Linux（腾讯云轻量 2C2G） | 5 容器，已公网验证 |

---

## 1. API 契约层 ★★★（本次独立成层）

**本应如何：** REST API 的接缝不是代码而是 **OpenAPI 契约**（openapi.yaml）。它横跨前后端：后端 springdoc 按注释自动生成契约；前端用 `openapi-typescript` 从契约**生成 TS 类型**，改接口即改类型，接口漂移在**编译期**暴露而非运行期。契约同时可驱动 Mock（Prism）、契约测试（Schemathesis）与文档展示。OpenAPI 不是"Swagger 工具"，是接口治理的第一公民。

**我们项目目前：** springdoc-openapi 2.5.0 已在 pom，SecurityConfig 已放行 `/swagger-ui/**` 与 `/v3/api-docs/**`。但：①多数接口未写 `@Tag/@Operation`（文档质量随注释规范飘移）；②**prod 环境 Swagger UI 公网可达**（未关闭，暴露接口清单）；③前端未消费契约，类型手写，接口变更靠人肉同步。

**接近什么：** 处于"能看文档"阶段，距"契约驱动"还差三步：开 `@Tag/@Operation` 注释 → prod 关闭 Swagger → openapi-typescript 生成前端类型。**单机云盘按此三步补，即可在简历上写"契约驱动开发"而非"接口文档"。**

---

## 2. 数据层

### 2.1 Flyway / Liquibase ★★★
- **本应如何：** Schema 以**版本化迁移脚本**管理（V1__init.sql …），应用启动自动执行未应用迁移，开发/生产库随时间收敛一致，从不手工 ALTER。
- **我们项目目前：** **无**。靠 `sql/init-full.sql` 手工比对，曾因生产库缺 3 列导致后端崩溃重启 93 次（BUG 6）。改表 = 手动 SQL + 手动同步文档。
- **接近什么：** 差距最大、**优先级第一**。接入后根除整类"Schema 不一致"事故，简历可写"数据库版本迁移与 CI 集成"。

### 2.2 Testcontainers ★★★
- **本应如何：** 集成测试用**真实 MySQL/Redis/MinIO 容器**（@Testcontainers），测试环境与生产一致，不依赖本地手动起库。
- **我们项目目前：** pom 已引入 `-test` starter 依赖族（data-redis-test / security-test / webmvc-test / mybatis-test），但 **src/test 只有一个脚手架空的 `CloudBackendApplicationTests`**——测试基建在、测试没写。
- **接近什么：** 依赖就位但零实践。补 JUnit 测试时直接上 Testcontainers 即可，是"后端补测试"这套步骤的天然载体（见 5.3）。

### 2.3 主从复制 / ShardingSphere ◐
- **本应如何：** 读写分离（主写从读）与分库分表，支撑大规模数据。
- **我们项目目前：** 无，单实例 MySQL 直连。
- **接近什么：** 单机 2C2G 硬上纯表演。简历写"了解原理 + 读过方案"即可，不必入项目。

### 2.4 Elasticsearch ◐
- **本应如何：** 文件名的全文搜索（分词、高亮、权重）替代 LIKE 扫表。
- **我们项目目前：** MySQL LIKE 模糊匹配。
- **接近什么：** 数据量到万级之前这是过度设计。跳过，面试提及"了解它解决什么"。

### 2.5 备份恢复 ★★
- **本应如何：** `mysqldump` 定时 + MinIO 数据卷快照，多副本保留，故障可恢复。
- **我们项目目前：** **无任何备份**，全部数据在本地数据卷。
- **接近什么：** 上线后第一风险。DEVELOPMENT.md §8 已列为待办，尽快落地 cron + 快照脚本。

---

## 3. 消息与异步

### 3.1 Redis 队列 / Spring 事件 ★★
- **本应如何：** 异步任务用**进程内事件（ApplicationEvent）+ 线程池 + 任务状态表**编排；跨进程才需要队列。批量压缩下载就是典型场景：任务 ID → 线程池执行 → WebSocket 推进度。
- **我们项目目前：** **有线程池 + WebSocket 进度推送**（大文件批量压缩），但**没有事件抽象与任务队列**，任务编排散在代码里。
- **接近什么：** 已具雏形。把任务封装为 Spring 事件 + 状态表，即可自称"事件驱动异步任务编排"，成本低收益明显。

### 3.2 RabbitMQ / Kafka / RocketMQ ★★★（学）/ ◐（项目用）
- **本应如何：** 解耦、削峰、可靠投递的**跨进程消息总线**。
- **我们项目目前：** 无。
- **接近什么：** **学习价值 > 项目价值**。单机 2C2G 跑 Kafka 是表演；用 Redis Stream 或干脆不引。简历策略：学透原理 + 跑通 demo，项目里写"评估后采用 Redis 队列，MQ 用于更大规模"——这是面试里最诚实的回答。

---

## 4. 可观测性（Ops）

### 4.1 Spring Boot Actuator + Micrometer ★★★
- **本应如何：** 应用自带 health / metrics / info 端点，Prometheus 抓取 JVM、HTTP、DB 指标。
- **我们项目目前：** **pom 无 actuator**（靠 docker healthcheck 判断存活，单薄）。
- **接近什么：** 一行依赖 + 一个端点即达"从 0 到 1"；再接 Prometheus。成本最低的必补项。

### 4.2 Prometheus + Grafana ★★★
- **本应如何：** 指标采集 + 看板（CPU/内存/JVM/接口 QPS/延迟百分位）。
- **我们项目目前：** 无，`docker stats` 裸看。
- **接近什么：** 配合 4.1 组成最小可观测栈，2C2G 下各留 64m 内存足够。简历可写"自建监控看板"。

### 4.3 OpenTelemetry / Jaeger / Zipkin ★★
- **本应如何：** 跨服务请求链路追踪（trace_id 串起调用链）。
- **我们项目目前：** 无。
- **接近什么：** **单服务项目价值低**（链路=应用内 method 调用）。了解概念即可，等真正拆服务/引入 MQ 再上。

### 4.4 Loki / ELK ★★
- **本应如何：** 集中日志检索（grep across 容器），替代 `docker logs` 逐容器翻。
- **我们项目目前：** `docker exec ... cat /app/logs/application.log`。
- **接近什么：** 容器少时投入产出一般。若上了 Prometheus 栈，加 Loki（同生态）比 ELK 轻得多；否则维持现状。

### 4.5 Sentry ★★
- **本应如何：** 前端 JS 运行时错误自动上报（sourcemap 还原），观察真实用户侧崩溃。
- **我们项目目前：** 无，只能靠复现。
- **接近什么：** 前端已踩过运行时错误类 bug（crypto.subtle / clipboard），Sentry 免费额度够单机用。接入成本低、简历好看（"前端错误监控"）。

---

## 5. 质量与工程化

### 5.1 Prettier + ESLint（前端）★★★
- **本应如何：** 格式化 + 静态检查（未使用变量、any 泄漏、响应式陷阱）作为开发常态。
- **我们项目目前：** **无**（只有 vue-tsc 类型检查进 build）。
- **接近什么：** 差距大但成本极低：`prettier --write` 一键格式化 + eslint 规则集。AI 改代码质量也能被兜底。

### 5.2 Spotless / Checkstyle / SpotBugs（后端）★★★
- **本应如何：** 格式统一 + 静态缺陷扫描（空指针路径、资源泄漏）挂在 mvn verify。
- **我们项目目前：** **无**（pom 无任何质量插件）。
- **接近什么：** spotless + google-java-format 一行接入即达 80% 收益；Checkstyle 做规则门禁。

### 5.3 JUnit 5 + Mockito + JaCoCo ★★★
- **本应如何：** 单测覆盖核心逻辑，覆盖率门禁（如 60%）挂在 CI。
- **我们项目目前：** **只有一个空 contextLoads 测试**；pom 已有 -test starter 依赖但完全未用。这是全项目最大短板。
- **接近什么：** 无中生有的状态。建议路径：先给文件上传/分享/配额等核心服务写 JUnit5+Mockito（Mockito 隔离 DB）→ 加 Testcontainers 跑关键集成路径 → JaCoCo 统计。

### 5.4 SonarQube ★★
- **本应如何：** 持续代码质量门禁（bug 味道、重复、坏味道、安全热点）。
- **我们项目目前：** 无。
- **接近什么：** 本地/单机太重（需 JVM 常驻），先不引；用 GitHub Actions 免费跑 sonarcloud 是更轻的替代。

### 5.5 Husky + lint-staged ★★★
- **本应如何：** git commit 前自动 format/lint/test 仅改动文件，坏代码进不了库。
- **我们项目目前：** 无（靠自觉）。
- **接近什么：** 接完 5.1/5.2 后一次性串联，收益即刻释放。

### 5.6 GitHub Actions（CI） ★★★
- **本应如何：** push 自动跑 lint + test + build（CI 门禁），再演进出 CD。
- **我们项目目前：** **完全手工闭环**（本地 build → 打包 → scp → compose 重启）。
- **接近什么：** 把"CI 三件套"（lint/test/build）放进 GitHub Actions 即可先拿 80% 价值；"部署环节自动化"可选自托管 runner，但单机场景手动 3 步也能接受。

---

## 6. 配置与脚手架

### 6.1 Spring Cloud Config / Nacos ★★（了解）
- **本应如何：** 配置集中管理 + 动态刷新，环境差异（dev/prod）随环境文件切换。
- **我们项目目前：** 本地/服务器 env 文件手动同步——已踩坑：prod 配置缺 `file.upload-expire-hours`（BUG 8）、`MINIO_ENDPOINT` 用错（BUG 6）。
- **接近什么：** 单机引入 Nacos 太重；合理的轻量替代是**配置校验 + 启动自检**（缺关键配置即 fail-fast）。了解原理即可，面试能讲"配置漂移"的解法思路。

### 6.2 MapStruct ★★★
- **本应如何：** DTO/VO/Entity 编译期安全映射，pojo 转换零样板。
- **我们项目目前：** 手写 getter/setter 转换。
- **接近什么：** 纯增补：加依赖注解即用，减样板并消除手写映射错漏。

---

## 7. 安全加固（下一层）

### 7.1 OAuth2 / OIDC ◐
- **本应如何：** Spring Authorization Server 提供第三方登录/授权码模式。
- **我们项目目前：** 自建账号体系（用户名+密码+JWT）。
- **接近什么：** 本期该跳过；若产品要微信/企业微信登录再上。简历写"了解 OAuth2 授权流程"即可。

### 7.2 RSA/JWK 替换对称 JWT ◐
- **本应如何：** 非对称签名（RSA/EC）+ JWKS，多服务各自验签不共享密钥。
- **我们项目目前：** **HS256 对称密钥**（单服务够用）。
- **接近什么：** 单服务对称没问题，拆服务时再换；知道"HS vs RS 取舍"这一句话在面试就够。

### 7.3 dependency-check / Trivy ★★
- **本应如何：** 依赖 CVE 扫描（后端 OWASP DC / 镜像 Trivy）作为 CI 一环。
- **我们项目目前：** 无。
- **接近什么：** `mvn dependency-check:check` 一行可接入；Trivy 扫镜像也免费。安全意识加分项。

---

## 8. 部署演进

### 8.1 GitHub Actions + 自托管 runner ★★★
- **本应如何：** push 到 master 自动 build 镜像 → 推 GHCR → 服务器 runner pull 并重启，发布零人工。
- **我们项目目前：** 手工上传 dist/jar + `docker compose up -d --build`。
- **接近什么：** CI（5.6）先行，CD 第二步。自托管 runner（服务器常驻）对单机是轻量的，值得作为最终形态。

### 8.2 Terraform ◐
- **本应如何：** 基础设施即代码（云资源声明式管理）。
- **我们项目目前：** 控制台手动操作。
- **接近什么：** 与单机项目无关；了解概念即可。

### 8.3 K8s / Docker Swarm ◐（只学）
- **本应如何：** 多机容器编排、自愈、扩缩容。
- **我们项目目前：** 单机 Docker Compose。
- **接近什么：** **2C2G 千万别上**；简历口述架构演进路线（Compose → 单节点 K8s → 生产 K8s）比真部署更划算。

### 8.4 Harbor / GHCR ◐
- **本应如何：** 私有镜像仓库，版本化 + 扫描 + 多环境拉取。
- **我们项目目前：** 无（镜像只在服务器本地构建）。
- **接近什么：** 与 8.1 绑定出现：上 CD 时用 GHCR（免费）而非 Harbor（私有化部署才需要）。

---

## 9. 引入顺序建议（务实的三步走）

**第一批（立刻，低风险高收益）**
1. Flyway —— 根除 Schema 事故
2. Actuator + Prometheus/Grafana —— 最小可观测
3. Spotless + Prettier/ESLint + Husky —— 质量底线
4. JUnit5 + Mockito + JaCoCo（先写核心服务测试）

**第二批（CI 与契约）**
5. GitHub Actions：lint + test + build
6. OpenAPI 契约化三步（@Tag/@Operation → prod 关 Swagger → 前端 openapi-typescript）
7. MapStruct、备份 cron

**第三批（演进/了解）**
8. Redis 事件化改造、Trivy
9. 口述层：K8s / MQ / OAuth2 / Terraform（只学不引）

---

## 10. 简历表述对照（本应 → 现在能写什么）

| 能力 | 目前简历可写 | 三批做完后可写 |
|---|---|---|
| 数据 | 手工管理 Schema | Flyway 版本化迁移 |
| 测试 | 无 | JUnit5 + Testcontainers 集成测试，覆盖率 X% |
| 监控 | 无 | Prometheus+Grafana 自建监控；Actuator 健康指标 |
| 接口 | 200+ REST API | OpenAPI 契约驱动，前端类型自动生成 |
| 质量 | 无 | Prettier/ESLint/Spotless + Husky 提交门禁 + CI 自动测试 |
| 部署 | Docker Compose 一键 | GitHub Actions CI/CD，推送即发布 |