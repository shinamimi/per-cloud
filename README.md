# Cloud — 私有云盘系统

基于 Spring Boot 4 + Vue 3 + Docker 的全栈私有云盘，支持文件管理、分片上传、分享、团队协作、回收站等功能。

## 功能特性

- **文件管理** — 目录树浏览、列表/图标视图、重命名、移动、复制、删除
- **分片上传** — 断点续传、秒传、实时进度（WebSocket）
- **批量下载** — 多文件异步打包下载
- **文件预览** — 图片/视频/音频/PDF/文本在线预览
- **分享系统** — 链接分享 + 提取码 + 有效期控制
- **团队空间** — 多人协作、团队文件隔离、成员角色管理
- **回收站** — 软删除 + 30天自动清理
- **管理后台** — 用户管理、系统配置、操作日志、仪表盘统计
- **好友系统** — 添加好友、好友间文件分享

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 21 / Spring Boot 4.0.7 / Spring Security / MyBatis / Redis / MinIO / WebSocket / JWT |
| 前端 | Vue 3 / TypeScript / Pinia / Vue Router / Element Plus / Axios |
| 部署 | Docker Compose / Nginx / MySQL 8.4 / Redis 7.2 / MinIO |

## 快速开始（开发环境）

### 前置条件

- JDK 21+
- Node.js 18+
- Docker & Docker Compose

### 1. 启动基础服务

```bash
docker compose up -d
```

启动 MySQL 8.4、Redis 7.2、MinIO（首次启动自动建表）。

### 2. 启动后端

```bash
cd backend
./mvnw spring-boot:run
```

后端运行在 `http://localhost:8080`。

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端运行在 `http://localhost:5173`。

### 默认账号

首次启动自动创建超级管理员，凭据见 `docker/prod.env` 中的 `SUPER_ADMIN_USERNAME` 和 `SUPER_ADMIN_PASSWORD`。

## 生产部署

详见 [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md)。

```bash
# 构建后端
cd backend && ./mvnw clean package -DskipTests

# 构建前端
cd frontend && npm run build

# 启动生产环境
docker compose -f docker-compose.prod.yml up -d
```

## 项目结构

```text
cloud/
├── backend/                # Spring Boot 后端
│   ├── src/main/java/com/cloud/backend/
│   │   ├── controller/     # REST API
│   │   ├── service/        # 业务逻辑
│   │   ├── entity/         # 数据库实体
│   │   ├── dto/            # 请求/响应 DTO
│   │   ├── mapper/         # MyBatis Mapper
│   │   ├── config/         # 配置类
│   │   └── security/       # JWT 认证
│   └── src/main/resources/
│       ├── application.yml
│       └── mapper/         # MyBatis XML
├── frontend/               # Vue 3 前端
│   └── src/
│       ├── views/          # 页面组件
│       ├── components/     # 公共组件
│       ├── stores/         # Pinia 状态管理
│       ├── api/            # API 请求封装
│       └── router/         # 路由配置
├── sql/                    # 数据库脚本
├── docker/                 # Docker 配置文件
├── docs/                   # 项目文档
└── scripts/                # 工具脚本
```

## 文档

- [产品需求文档](docs/PRD.md)
- [架构技术全景](docs/ARCHITECTURE-TECHNOLOGY-MAP.md)
- [部署方案](docs/DEPLOYMENT.md)
- [API 文档](docs/API.md)
- [数据库设计](docs/DATABASE.md)
- [编码规范](docs/CODING_STANDARDS.md)
