# Cloud 云盘 — 代码规范

---

## 1. 后端规范 (Java / Spring Boot)

### 1.1 项目结构

```
com.cloud.backend
├── config/          # 配置类（Security、MinIO、JWT、WebSocket 等）
├── constant/        # 常量接口
├── controller/      # 控制器（用户端）
│   └── admin/       # 管理端控制器
├── dto/             # 请求/响应 DTO
├── entity/          # 数据库实体
├── enums/           # 枚举
├── exception/       # 异常 + 全局处理器
├── handler/         # WebSocket Handler
├── interceptor/     # WebSocket / 请求拦截器
├── mapper/          # MyBatis Mapper 接口
├── security/        # Spring Security 组件
├── service/         # 服务接口
│   └── impl/        # 服务实现
└── utils/           # 工具类
```

### 1.2 命名规范

| 元素 | 规范 | 示例 |
|------|------|------|
| 类名 | PascalCase | `FileService`, `UserController` |
| 方法名 | camelCase | `findById()`, `uploadFile()` |
| 变量名 | camelCase | `userId`, `parentId` |
| 常量 | UPPER_SNAKE | `DEFAULT_QUOTA`, `ROOT_PARENT_ID` |
| 包名 | 全小写 | `com.cloud.backend.service` |
| Mapper XML | 与接口同名 | `FileMapper.xml` |

### 1.3 API 设计

- **基础路径**: `/api/{模块}`，如 `/api/files`、`/api/shares`
- **RESTful 风格**:
  - `GET /api/files` — 列表查询
  - `POST /api/files/directory` — 创建目录
  - `PUT /api/files/{id}/rename` — 重命名
  - `DELETE /api/files/{id}` — 删除
- **管理端**: `/api/admin/{模块}`，如 `/api/admin/users`
- **统一响应**: 全部返回 `Result<T>` 格式

### 1.4 实体设计

- 使用 Lombok `@Data`
- 数据库字段下划线命名 → 实体驼峰命名（MyBatis `map-underscore-to-camel-case: true`）
- 枚举存储为 TINYINT（自定义 `value` 字段，禁用 `ordinal()`），在 `MyBatisTypeHandlerConfig` 中注册

### 1.5 异常处理

- 业务异常抛出 `BusinessException(ErrorCode, message)`
- 参数校验用 `@Valid` + `jakarta.validation` 注解
- 统一由 `GlobalExceptionHandler` 捕获并返回 `Result`

### 1.6 操作日志

- 关键操作通过直接注入 `OperationLogService` 记录（不引入 BaseController 继承体系）
- 包括：登录、上传、下载、删除、分享、创建团队等

---

## 2. 前端规范 (Vue 3 + TypeScript)

### 2.1 项目结构

```
src/
├── api/             # API 调用层（Axios 封装）
├── assets/          # 静态资源
├── components/      # 通用组件
├── composables/     # 组合式函数（useUpload、useWebSocket 等）
├── layout/          # 布局组件（MainLayout.vue）
├── router/          # 路由配置
├── stores/          # Pinia 状态管理
├── types/           # TypeScript 类型定义
├── utils/           # 工具函数
└── views/           # 页面组件
    └── admin/       # 管理端页面
```

### 2.2 命名规范

| 元素 | 规范 | 示例 |
|------|------|------|
| 组件名 | PascalCase | `FileListView.vue` |
| 路由 path | kebab-case | `/file-list` |
| 变量/函数 | camelCase | `isLoading`, `handleLogin()` |
| 常量 | UPPER_SNAKE | `MAX_FILE_SIZE` |
| Pinia Store | camelCase + Store | `useUserStore`, `useFileStore` |
| 目录名 | kebab-case | `admin/`, `components/` |

### 2.3 组件设计

- 页面组件放在 `views/` 下
- 通用可复用组件放在 `components/` 下
- 每个组件一个文件，使用 `<script setup lang="ts">`
- 模板中事件绑定用 `@click`（不用 `v-on:click`）

### 2.4 API 请求

- 统一通过 `src/utils/request.ts` 封装的 Axios 实例
- Token 自动从 localStorage 读取并注入 Authorization 头
- 401 响应自动跳转到登录页

### 2.5 状态管理

- 全局用户状态在 `userStore` 中管理
- 组件内部状态用 `ref` / `reactive`
- 避免在组件中直接修改 localStorage

### 2.6 UI 规范

- UI 组件库使用 Element Plus，中文语言包
- 表格操作列按钮用图标 + 文字组合
- 重要操作（删除等）需二次确认（`ElMessageBox.confirm`）

---

## 3. Git 规范

### 3.1 提交信息

使用中文，格式：`{范围}: {描述}`

| 前缀 | 说明 | 示例 |
|------|------|------|
| 新增 | 新功能 | `新增: 文件分片上传接口` |
| 修改 | 功能变更 | `修改: 文件列表改为分页返回` |
| 修复 | Bug 修复 | `修复: 秒传时文件哈希比对错误` |
| 重构 | 代码重构 | `重构: 提取公共文件处理工具类` |
| 优化 | 性能/体验优化 | `优化: 减少文件列表查询的 join 次数` |
| 文档 | 文档变更 | `文档: 补充 API 接口文档` |
| 配置 | 配置/CI 变更 | `配置: 新增 dev 环境 profile` |

### 3.2 提交原则

- 每个提交只做一件事，粒度适中
- 提交信息说清楚"为什么改"而非"改了哪些文件"
- 不提交未编译通过的代码

---

## 4. 数据库规范

### 4.1 命名

| 元素 | 规范 | 示例 |
|------|------|------|
| 表名 | `t_` 前缀 + 下划线 | `t_user`, `t_file`, `t_team_member` |
| 字段 | 下划线 | `user_id`, `parent_id`, `created_at` |
| 主键 | `id` | `id BIGINT AUTO_INCREMENT` |
| 索引 | `idx_` 前缀 | `idx_user_parent` |
| 唯一约束 | `uk_` 前缀 | `uk_team_user` |

### 4.2 设计原则

- 所有表使用 InnoDB + utf8mb4
- 使用 `DATETIME` 而非 `TIMESTAMP`
- 状态字段用 TINYINT 而非 VARCHAR
- 逻辑删除（status 字段）而非物理删除
