# 文档命名规范

> 生效日期：2026-08-31
> 适用范围：`docs/` 目录下所有文档

---

## 命名规则

### 两级文档体系

```
docs/
├── CATEGORY.md                    # 一级文档：核心架构/全局（全大写 + 连字符）
├── category-topic.md              # 二级文档：模块/功能（全小写 + 连字符）
└── SUBCATEGORY-CATEGORY.md        # 一级文档变体：分类前缀 + 主题（全大写 + 连字符）
```

### 一级文档（全大写 + 连字符）

核心架构、全局性文档：

| 文件名 | 含义 |
|--------|------|
| `API.md` | API 文档 |
| `PRD.md` | 产品需求文档 |
| `DDD.md` | 领域驱动设计 |
| `HLD.md` | 高层设计 |
| `DATABASE.md` | 数据库设计 |
| `DEPLOYMENT.md` | 部署文档 |
| `TEST.md` | 测试文档 |
| `CODING-STANDARDS.md` | 编码规范 |
| `PROJECT-STRUCTURE.md` | 项目结构 |
| `ARCHITECTURE-TECHNOLOGY-MAP.md` | 架构技术图 |
| `COMPONENT-TRANSFER.md` | 组件迁移 |
| `FRONTEND-STANDARD.md` | 前端规范 |
| `HANDOFF.md` | 交接文档 |
| `AI-CODE-QUALITY.md` | AI 代码质量 |
| `MIGRATE-1PANEL-HTTPS.md` | 迁移 1Panel HTTPS |

### 一级文档变体（分类前缀 + 主题）

优化/专项文档：

| 文件名 | 含义 |
|--------|------|
| `OPTIMIZATION.md` | 优化总览 |
| `OPTIMIZATION-INFRA.md` | 基础设施层优化 |
| `OPTIMIZATION-CODE.md` | 代码层优化 |
| `OPTIMIZATION-INDEX.md` | 数据库索引优化 |
| `PERF-TEST-REPORT.md` | 压测报告 |

### 二级文档（全小写 + 连字符）

模块级文档，使用模块前缀：

| 前缀 | 含义 | 示例 |
|------|------|------|
| `admin-` | 管理后台 | `admin-file-management.md`、`admin-role-hierarchy.md`、`admin-user-management.md` |
| `file-` | 文件模块 | `file-module.md`、`file-upload-evolution-spec.md` |
| `team-` | 团队模块 | `team-module.md` |
| `share-` | 分享模块 | `share-module.md` |
| `friend-` | 好友系统 | `friend-system.md` |
| `system-` | 系统级 | `system-config-center.md` |

---

## 最终文件清单

```
docs/
├── DOC-NAMING-CONVENTION.md          # 命名规范
├── API.md                            # API 文档
├── PRD.md                            # 产品需求
├── DDD.md                            # 领域驱动设计
├── HLD.md                            # 高层设计
├── DATABASE.md                       # 数据库设计
├── DEPLOYMENT.md                     # 部署文档
├── TEST.md                           # 测试文档
├── CODING-STANDARDS.md               # 编码规范
├── PROJECT-STRUCTURE.md              # 项目结构
├── ARCHITECTURE-TECHNOLOGY-MAP.md    # 架构技术图
├── COMPONENT-TRANSFER.md             # 组件迁移
├── FRONTEND-STANDARD.md              # 前端规范
├── HANDOFF.md                        # 交接文档
├── AI-CODE-QUALITY.md                # AI 代码质量
├── MIGRATE-1PANEL-HTTPS.md           # 迁移 1Panel HTTPS
├── OPTIMIZATION.md                   # 优化总览
├── OPTIMIZATION-INFRA.md             # 基础设施层优化
├── OPTIMIZATION-CODE.md              # 代码层优化
├── PERF-TEST-REPORT.md               # 压测报告
├── admin-file-management.md          # 管理后台-文件管理
├── admin-role-hierarchy.md           # 管理后台-角色层级
├── admin-user-management.md          # 管理后台-用户管理
├── file-module.md                    # 文件模块
├── file-upload-evolution-spec.md     # 文件上传演进
├── team-module.md                    # 团队模块
├── share-module.md                   # 分享模块
├── friend-system.md                  # 好友系统
├── system-config-center.md           # 系统配置中心
├── adr/                              # 架构决策记录
├── bugs/                             # Bug 修复记录
└── notes/                            # 开发笔记
```

---

## 禁止事项

1. 禁止混合大小写（如 `High_Load_Tuning.md`、`CODING_STANDARDS.md`）
2. 禁止使用空格分隔
3. 禁止使用中文文件名
4. 一级文档禁止下划线（仅允许连字符 `-`）
5. 二级文档必须使用模块前缀（`admin-`、`file-` 等）
