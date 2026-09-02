# Per-Cloud 私有云盘

基于 Spring Boot 4 + Vue 3 + Docker 构建的全栈私有云盘系统，支持分片上传、断点续传、秒传、文件分享、RBAC 权限控制、对象存储等功能。

## 功能特性

| 模块 | 功能说明 |
|------|---------|
| 文件管理 | 目录树浏览、列表/图标视图、搜索、重命名、移动、复制、删除 |
| 上传下载 | 分片上传、断点续传、秒传、批量 ZIP 打包下载 |
| 文件预览 | 图片/视频/音频/PDF/文本在线预览 |
| 分享系统 | 链接分享 + 提取码 + 有效期 + 防盗链 |
| 团队空间 | 多人协作、团队文件隔离、成员角色管理 |
| 回收站 | 软删除 + 30 天自动清理 |
| 管理后台 | 仪表盘统计、用户管理、系统配置、操作日志 |
| 好友系统 | 添加好友、好友间文件分享 |

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 21 / Spring Boot 4.0.7 / Spring Security / MyBatis / Redis / MinIO / JWT |
| 前端 | Vue 3 / TypeScript / Pinia / Element Plus |
| 部署 | Docker Compose / Nginx / MySQL 8.4 / Redis 7.2 / MinIO |

## 快速开始

### 前置条件

- JDK 21+
- Node.js 18+
- Docker & Docker Compose

### 开发环境

```bash
# 克隆仓库
git clone https://github.com/shinamimi/per-cloud.git
cd per-cloud

# 启动基础设施（MySQL、Redis、MinIO）
docker compose up -d

# 启动后端
cd backend && ./mvnw spring-boot:run

# 启动前端
cd frontend && npm install && npm run dev
```

访问 http://localhost:5173

### 生产部署

```bash
# 构建后端
cd backend && ./mvnw clean package -DskipTests

# 构建前端
cd frontend && npm run build

# 启动生产环境
docker compose -f docker-compose.prod.yml up -d
```

> 生产环境针对 2C2G 低配服务器优化，详见 [性能优化文档](docs/public/PERFORMANCE.md)

## 性能指标

| 场景 | QPS | 延迟 |
|------|-----|------|
| 登录接口 | 950+ | < 50ms |
| 文件列表 | 400+ | < 100ms |
| 目录树查询 | 120+ | < 100ms |

针对 2C2G 云服务器优化：G1GC 调优、复合索引、连接池配置。

## 项目结构

```
per-cloud/
├── backend/                # Spring Boot 后端
│   └── src/main/java/com/cloud/backend/
│       ├── controller/     # REST API
│       ├── service/        # 业务逻辑
│       ├── entity/         # 数据库实体
│       ├── dto/            # 请求/响应 DTO
│       ├── mapper/         # MyBatis Mapper
│       ├── config/         # 配置类
│       └── security/       # JWT 认证
├── frontend/               # Vue 3 前端
│   └── src/
│       ├── views/          # 页面组件
│       ├── components/     # 公共组件
│       ├── stores/         # Pinia 状态管理
│       ├── api/            # API 请求封装
│       └── router/         # 路由配置
├── sql/                    # 数据库脚本
├── docker/                 # Docker 配置文件
└── docs/public/            # 公开技术文档
```

## 文档

- [架构设计](docs/public/ARCHITECTURE.md)
- [性能优化](docs/public/PERFORMANCE.md)
- [安全设计](docs/public/SECURITY.md)
- [上传机制](docs/public/UPLOAD-MECHANISM.md)
- [部署指南](docs/public/DEPLOYMENT.md)

## License

[MIT](LICENSE)
