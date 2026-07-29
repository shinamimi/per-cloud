# Cloud 企业级云盘 — 产品需求文档

> 版本: v0.1 (MVP)
> 更新日期: 2026-07-28
> 状态: Draft

---

## 1. 产品概述

### 1.1 产品定位

企业级私有云盘系统，既作为家庭小范围使用的实际工具，同时作为展示全栈开发能力的求职项目。

### 1.2 核心目标

- **MVP 目标**: 1-2 个月内交付可运行的云盘最小可用版本
- **长期愿景**: 构建功能完善、高可用、可扩展的私有云盘平台

### 1.3 目标用户

| 用户类型 | 描述 | 核心需求 |
|---------|------|---------|
| 普通用户 | 家庭成员、受邀用户 | 文件存储、浏览、上传下载、分享 |
| 团队空间成员 | 隶属于某个团队的用户 | 多人协作的文件共享空间 |
| 管理员 (ADMIN) | 系统维护人员 | 用户管理、系统监控、日志审计 |
| 超级管理员 (SUPER_ADMIN) | 系统拥有者 | 全权管理，包括管理员账户管理 |

---

## 2. 功能范围

### 2.1 MVP 功能清单

#### 2.1.1 用户端 — 文件管理

| 模块 | 功能 | 说明 | 技术亮点 |
|------|------|------|---------|
| 文件浏览 | 目录树浏览 | 按目录层级展示文件列表 | 分页加载、无限滚动 |
| 文件浏览 | 文件视图切换 | 列表视图 / 图标视图 | |
| 文件操作 | 创建目录 | 在当前目录下创建子目录 | |
| 文件操作 | 上传文件 | 单文件/多文件上传 | 分片上传、断点续传、秒传 |
| 文件操作 | 下载文件 | 单文件下载 | 直链下载 / 流式下载 |
| 文件操作 | 批量下载 | 多文件/目录打包下载 | 异步生成 + 进度通知 |
| 文件操作 | 删除文件 | 软删除（标记为删除状态，移入回收站存储记录） | |
| 文件操作 | 重命名 | 文件/目录重命名 | |
| 文件操作 | 移动/复制 | 跨目录移动或复制文件 | |
| 文件预览 | 图片预览 | 缩略图 + 原图查看 | |
| 文件预览 | 文本预览 | 文本/代码文件在线查看 | |
| 搜索 | 文件名搜索 | 按文件名模糊搜索 | |
| 传输管理 | 上传进度 | 实时显示上传进度 | WebSocket/SSE |
| 传输管理 | 传输队列 | 进行中/已完成/失败的传输列表 | |

#### 2.1.2 用户端 — 分享管理

| 功能 | 说明 |
|------|------|
| 创建分享 | 生成文件/目录分享链接 |
| 有效期设置 | 支持限时分享（1天/7天/30天/永久） |
| 提取码 | 可选访问密码 |
| 分享预览 | 分享页面可预览文件内容（图片/文本） |
| 我的分享 | 查看已创建的分享列表，可取消分享 |

#### 2.1.3 团队空间

| 功能 | 说明 |
|------|------|
| 创建团队空间 | 用户可以创建团队，成为团队所有者 |
| 邀请成员 | 通过用户名或邮箱邀请成员加入 |
| 成员管理 | 所有者可移除成员、设置角色（管理员/成员） |
| 团队文件 | 团队成员共享文件空间，文件归团队所有 |
| 团队文件操作 | 同个人文件管理（上传/下载/删除/重命名/移动） |
| 退出团队 | 成员可主动退出团队 |

#### 2.1.4 用户端 — 账户

| 功能 | 说明 |
|------|------|
| 注册 | 邮箱验证码注册 |
| 登录 | 用户名/邮箱 + 密码登录 |
| 忘记密码 | 邮箱验证码重置密码 |
| 个人中心 | 查看/修改个人资料、头像、修改密码 |
| 空间配额 | 显示已用空间/总配额 |

#### 2.1.5 管理端

| 模块 | 功能 |
|------|------|
| 仪表盘 | 系统概览（用户数、文件数、存储使用量、配额使用率） |
| 用户管理 | 用户列表、启用/禁用、修改配额、解锁 |
| 文件管理 | 全局文件浏览、删除 |
| 分享管理 | 分享列表、强制取消分享 |
| 审计日志 | 操作日志查询、过滤 |
| 管理员管理 | 创建/删除 ADMIN/OPERATOR、修改角色 |
| 团队管理 | 全局团队列表、强制解散 |

---

### 2.2 v0.2 规划（MVP 后）

- 回收站管理界面（浏览、恢复、彻底删除）
- Office 文档在线预览（对接 KkFileView 或 OnlyOffice）
- 视频/音频在线播放
- 移动端适配
- 部署文档 + 运维脚本
- 文件版本管理

---

## 3. 技术架构

### 3.1 整体架构

```
┌─────────────────────────────────────────────────┐
│                  前端 (Vue 3 + TS)               │
│  ┌──────────┐ ┌──────────┐ ┌──────────────────┐ │
│  │ 用户页面  │ │ 管理页面  │ │  分享页面(公开)  │ │
│  └──────────┘ └──────────┘ └──────────────────┘ │
└──────────────────────┬──────────────────────────┘
                       │ HTTP / WebSocket
┌──────────────────────┴──────────────────────────┐
│              后端 (Spring Boot)                  │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐  │
│  │ Auth │ │ File │ │Share │ │ Team │ │Admin │  │
│  │ API  │ │ API  │ │ API  │ │ API  │ │ API  │  │
│  └──┬───┘ └──┬───┘ └──┬───┘ └──┬───┘ └──┬───┘  │
│     └────────┴────────┴────────┴────────┘       │
│                   Service Layer                  │
│  ┌─────────────────────────────────────────────┐ │
│  │         MinIO Storage Service               │ │
│  └─────────────────────────────────────────────┘ │
└──────┬──────────────┬──────────────┬─────────────┘
       │              │              │
   ┌───┴───┐     ┌───┴───┐     ┌───┴───┐
   │ MySQL │     │ Redis │     │ MinIO │
   └───────┘     └───────┘     └───────┘
```

### 3.2 技术栈

| 层级 | 技术 | 用途 |
|------|------|------|
| 前端 | Vue 3 + TypeScript | 用户界面 |
| 前端 | Pinia | 状态管理 |
| 前端 | Vue Router | 路由管理 |
| 前端 | Vite | 构建工具 |
| 前端 UI | Element Plus | UI 组件库 |
| 后端 | Java 21 + Spring Boot 4.0.7 | API 服务 |
| 后端 | Spring Security + JWT | 认证授权 |
| 后端 | MyBatis | 数据访问 |
| 后端 | MinIO SDK | 对象存储 |
| 存储 | MySQL 8.4 | 关系数据 |
| 存储 | Redis 7.2 | 缓存/黑名单/进度推送 |
| 存储 | MinIO | 文件存储 |

### 3.3 关键技术亮点

#### 3.3.1 大文件分片上传 + 断点续传

- 前端将文件切分为固定大小分片（默认 5MB）
- 每个分片独立上传，支持并发
- 上传中断后可从已上传的分片继续
- 后端合并分片为完整文件
- 流程图:

```
1. 初始化上传 → 获取 uploadId
2. 逐片上传（携带 uploadId + 分片序号）
3. 合并请求 → 后端合并分片 → 生成完整文件
```

#### 3.3.2 秒传

- 上传前计算文件 SHA-256 Hash
- 后端查询 Hash 是否已存在
- 已存在则复制已有存储对象到当前用户目录（隔离存储，防止用户间误删影响），秒级完成

#### 3.3.3 实时进度推送

- WebSocket 建立持久连接
- 上传/下载进度实时推送至前端
- 传输队列状态变更通知

#### 3.3.4 操作审计

- 所有关键操作（登录、上传、下载、删除、分享等）记录至 t_operation_log
- 记录 IP、User-Agent、操作详情
- 管理员可查询审计日志

#### 3.3.5 图片智能管理

- 上传图片时自动提取 EXIF 信息（拍摄时间、设备、GPS）
- 生成缩略图
- 按日期/设备自动归类（v0.2 规划）

---

## 4. 数据模型变更

### 4.1 新增表

#### t_team（团队空间）

```sql
CREATE TABLE t_team (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(64)    NOT NULL COMMENT '团队名称',
    owner_id    BIGINT         NOT NULL COMMENT '创建者用户ID',
    avatar      VARCHAR(256)   DEFAULT NULL COMMENT '团队头像',
    description VARCHAR(512)   DEFAULT NULL COMMENT '团队描述',
    quota       BIGINT         NOT NULL DEFAULT 10737418240 COMMENT '团队总配额（默认10GB）',
    used_space  BIGINT         NOT NULL DEFAULT 0 COMMENT '团队已用空间',
    status      TINYINT        NOT NULL DEFAULT 1 COMMENT '0-解散 1-正常',
    created_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_owner (owner_id)
) COMMENT '团队空间';
```

#### t_team_member（团队成员）

```sql
CREATE TABLE t_team_member (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_id   BIGINT   NOT NULL COMMENT '团队ID',
    user_id   BIGINT   NOT NULL COMMENT '用户ID',
    role      TINYINT  NOT NULL DEFAULT 0 COMMENT '0-成员 10-管理员 20-所有者',
    status    TINYINT  NOT NULL DEFAULT 1 COMMENT '0-已退出 1-正常',
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_team_user (team_id, user_id)
) COMMENT '团队成员';
```

### 4.2 现有表变更

#### t_file

新增字段:
- `team_id BIGINT DEFAULT NULL COMMENT '所属团队ID，NULL 表示个人文件'`
- 索引: `INDEX idx_team (team_id, parent_id, status)`

#### t_share

新增字段:
- `team_id BIGINT DEFAULT NULL COMMENT '所属团队ID，NULL 表示个人文件分享'`

#### t_operation_log

原有字段已覆盖所需，无需变更。

---

## 5. API 概览

### 5.1 新增用户端 API

#### 文件管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/files` | 获取文件列表（目录下） |
| GET | `/api/files/tree` | 获取目录树 |
| POST | `/api/files/directory` | 创建目录 |
| POST | `/api/files/upload/init` | 初始化上传（分片） |
| POST | `/api/files/upload/chunk` | 上传分片 |
| POST | `/api/files/upload/merge` | 合并分片 |
| POST | `/api/files/upload/sec` | 秒传（按 Hash） |
| GET | `/api/files/download/{id}` | 下载文件 |
| POST | `/api/files/download/batch` | 批量打包下载 |
| PUT | `/api/files/{id}/rename` | 重命名 |
| POST | `/api/files/{id}/move` | 移动文件 |
| POST | `/api/files/{id}/copy` | 复制文件 |
| DELETE | `/api/files/{id}` | 删除（移入回收站） |
| GET | `/api/files/search` | 搜索文件 |
| GET | `/api/files/{id}/preview` | 预览文件（图片/文本） |

#### 分享

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/shares` | 创建分享 |
| GET | `/api/shares` | 我的分享列表 |
| DELETE | `/api/shares/{id}` | 取消分享 |
| GET | `/api/shares/access/{token}` | 访问分享（验证后获取文件信息） |
| POST | `/api/shares/access/{token}/verify` | 验证提取码 |
| GET | `/api/shares/access/{token}/file/{fileId}/preview` | 分享内文件预览 |

#### 团队空间

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/teams` | 创建团队 |
| GET | `/api/teams` | 我的团队列表 |
| GET | `/api/teams/{id}` | 团队详情 |
| PUT | `/api/teams/{id}` | 更新团队信息 |
| DELETE | `/api/teams/{id}` | 解散团队 |
| POST | `/api/teams/{id}/members` | 邀请成员 |
| DELETE | `/api/teams/{id}/members/{userId}` | 移除成员 |
| GET | `/api/teams/{id}/members` | 成员列表 |
| POST | `/api/teams/{id}/leave` | 退出团队 |
| GET | `/api/teams/{id}/files` | 团队文件列表 |
| (其他文件操作同 /api/files 路径，增加 teamId 参数) | | |

#### WebSocket

| 路径 | 说明 |
|------|------|
| `/ws/upload/progress/{uploadId}` | 上传进度订阅 |
| `/ws/package/progress/{taskId}` | 打包下载进度订阅 |

### 5.2 管理端 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/teams` | 全局团队列表 |
| DELETE | `/api/admin/teams/{id}` | 强制解散团队 |

---

## 6. 非功能需求

| 维度 | 要求 |
|------|------|
| 安全性 | JWT 24h 过期，登录失败 5 次锁定 15 分钟，密码 BCrypt 加密 |
| 性能 | 分片上传支持并发 5 片，文件列表响应 < 500ms |
| 可用性 | 上传中断可续传，操作有明确反馈 |
| 兼容性 | Chrome/Firefox/Edge 最新版本，PC 端优先 |
| 国际化 | 首版仅中文，代码中英文命名 |

---

## 7. 里程碑规划

```
Phase 1（第 1-2 周）— 基础设施
├── 新增团队空间数据模型 + 后端 Service/Mapper
├── 用户端文件管理 API 开发（全部 Controller）
├── 前端项目初始化（Vite + Vue 3 + TS + 路由 + 状态管理）
└── 分片上传 + 秒传后端逻辑

Phase 2（第 3-4 周）— 核心功能
├── 前端文件管理页面（列表/图标视图、上传、下载、目录树）
├── 前端注册/登录/个人中心
├── 分享 API + 前端分享管理页面
├── 分享公开访问页面
├── WebSocket 实时进度
└── 图片缩略图生成

Phase 3（第 5-6 周）— 团队空间 + 管理端
├── 团队空间 API + 前端
├── 前端管理端页面
├── 操作审计 + 查询
├── 批量打包下载
└── 端到端联调

Phase 4（第 7-8 周）— 收尾
├── Docker 生产镜像构建
├── 部署文档
├── Bug 修复 + 体验优化
└── v0.1 发布
```

---

## 8. 开放问题 (Open Issues)

1. 服务器配置待确认：部署方式（Docker / 裸机）、可用资源
2. 文件预览是否需要图片缩略图生成服务（如 ImgMagick/Thumbnailator）
3. 批量打包下载选用服务器端打包还是异步任务
4. 团队空间创建是否有数量限制
5. 是否需要文件收藏/星标功能
6. 是否对接第三方登录（OAuth）

---

> **下一阶段**: 确认 PRD → 拆解为开发任务 → Phase 1 开发
