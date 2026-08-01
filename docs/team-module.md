# 团队模块功能方案

> 对应 DDD 文档模块 M6 — 团队空间

## 一、功能边界

| 功能 | 说明 | 本次 |
|------|------|:----:|
| 团队 CRUD / 成员管理 | 创建/解散/邀请/移除/退出/成员列表 | ✅ |
| 团队文件 | 独立目录树，复用个人上传/下载/预览/秒传逻辑 | ✅ |
| 团队配额 | 独立配额（t_team.quota），管理员后台分配 | ✅ |
| 团队回收站 | 删除进团队回收站，30 天可恢复 | ✅ |
| 管理员团队管理页 | M7 管理后台，ADMIN+ 可访问 | ✅ |
| 团队默认值配置 | 入系统配置中心"团队"分组 | ✅ |
| 好友拉人 | 建团队/邀请成员时从好友列表勾选 | ✅ |

## 二、接口清单

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/teams` | 创建团队 |
| GET | `/api/teams` | 我的团队列表 |
| GET | `/api/teams/{id}` | 团队详情 |
| PUT | `/api/teams/{id}` | 更新信息 |
| DELETE | `/api/teams/{id}` | 解散（仅 OWNER） |
| POST | `/api/teams/{id}/members` | 邀请成员（可从好友列表选） |
| DELETE | `/api/teams/{id}/members/{userId}` | 移除成员 |
| GET | `/api/teams/{id}/members` | 成员列表 |
| POST | `/api/teams/{id}/leave` | 退出团队 |
| GET | `/api/teams/{id}/files` | 团队文件列表 |

## 三、权限矩阵

| 操作 | OWNER | ADMIN | MEMBER |
|------|:-----:|:-----:|:------:|
| 上传文件 | ✅ | ✅ | ✅ |
| 下载/预览 | ✅ | ✅ | ✅ |
| 修改自己的文件 | ✅ | ✅ | ✅ |
| 修改他人文件 | ✅ | ✅ | — |
| 删除任意文件 | ✅ | ✅ | — |
| 创建分享 | ✅ | ✅ | ✅ |
| 取消他人分享 | ✅ | ✅ | — |
| 成员管理 | ✅ | ✅ | — |
| 解散团队 | ✅ | — | — |
| 退出团队 | — | ✅ | ✅ |

- MEMBER 只能修改/删除**自己上传的**文件
- ADMIN 可修改/删除**团队所有文件**
- 创建分享所有成员可，但分享的管理权限与文件权限一致

## 四、文件模型

### 4.1 归属

- 团队文件与个人文件**同表（t_file）+ teamId 字段**区分
- 个人空间查询 `team_id IS NULL`；团队空间查询 `team_id = {当前团队}`
- 团队文件**记录上传者**（userId），用于"只能改自己的"权限判断

### 4.2 目录

- 团队独立目录树（parentId 在 teamId 范围内）
- 复用个人文件的创建目录/重命名/移动/复制逻辑，仅带 teamId

### 4.3 秒传

- 团队文件统一走全局秒传（引用计数 +1），与个人文件共用索引

## 五、团队配额

- 独立配额：`t_team.quota` + `used_space`
- 成员上传占用**团队配额**，不计入个人
- 配额由**管理员后台分配**（AdminTeamController）
- 超出配额返回 TEAM_QUOTA_EXCEEDED

## 六、团队回收站

- 团队文件删除进**团队回收站**（t_recycle_bin 带 team_id）
- 保留时长默认可配置（团队回收站时长），到期自动物理删除
- 恢复/删除权限同文件管理权限

## 七、管理员团队管理页面（M7）

### 7.1 定位

ADMIN+ 可访问，管理后台"团队管理"入口。

### 7.2 功能

| 模块 | 功能 |
|------|------|
| 团队列表 | 所有团队：名称/成员数/配额/已用空间/创建时间 |
| 团队详情 | 成员列表（可移除）、配额调整、解散 |
| 团队文件 | 浏览/删除团队内任意文件 |
| 团队回收站 | 浏览/恢复/物理删除/清理 |

### 7.3 接口

- `GET /api/admin/teams` 团队列表
- `GET /api/admin/teams/{id}` 团队详情（含成员）
- `PUT /api/admin/teams/{id}/quota` 调整配额
- `DELETE /api/admin/teams/{id}` 解散团队
- `GET /api/admin/teams/{id}/files` 团队文件列表
- `GET/DELETE /api/admin/teams/{id}/recycle-bin` 回收站管理

## 八、团队默认值（系统配置中心）

新增"团队"分组：

| key | 说明 |
|-----|------|
| team.max-per-user | 每人团队数上限 |
| team.default-quota | 新团队默认配额 |
| team.recycle-bin-days | 团队回收站保留时长 |
| team.max-members | 团队最大成员数 |

- OPERATOR 可改（普通配置）
- 详见 `docs/system-config-center.md`

## 九、需要做的事

### 后端
1. TeamService 补齐：创建/更新/解散/成员管理/退出，校验权限矩阵
2. Team 文件服务：复用 FileService 增加 teamId 维度
3. TeamFile 权限校验：MEMBER 只能改自己的，ADMIN 可改所有
4. 团队配额：创建时用默认值，上传扣团队配额
5. 团队回收站：删除进回收站（带 teamId）、定时清理
6. AdminTeamController：团队列表/详情/配额/解散/文件/回收站
7. 系统配置中心新增"团队"分组
8. 邀请成员支持从好友列表选（复用好友关系）
9. SecurityConfig：团队管理路由 ADMIN+

### 前端
10. `/teams`、`/teams/{id}`、`/teams/{id}/files` 页面
11. 建团队/邀请弹窗支持好友列表勾选
12. 管理后台团队管理页
13. 系统配置中心新增"团队"分组 Tab

## 十、变更范围

### 涉及文件
- `controller/TeamController.java`（补全）、`controller/admin/AdminTeamController.java`
- `service/team/`：TeamService 补齐 + TeamFileService
- `mapper/`：TeamMapper、TeamMemberMapper（补查询）
- `entity/File.java`（teamId）、`entity/Team.java`（配额定值）
- `dto/`：团队相关 DTO
- `authorization/AuthorizationPolicy.java`：团队文件权限校验
- 系统配置中心：新增团队分组配置
- 前端：团队相关页面 + 管理后台团队管理页

### 禁止修改
- 个人文件上传/秒传核心逻辑（复用不重写）
- 现有配额模型
- 好友系统结构
