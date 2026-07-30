# Cloud 云盘 — 代码规范

> 版本: v0.1
> 更新日期: 2026-07-28

---

## 1. 后端规范 (Java / Spring Boot)

### 1.1 项目结构

```
com.cloud.backend
backend/src/main/java/com/cloud/backend/
├── annotation/          # 自定义标记注解（横切关注点）
├── aspect/              # AOP 切面实现（日志、权限拦截等）
├── authorization/       # 业务权限校验规则
├── bo/                  # 业务对象（多表组合查询结果）
├── config/              # 应用配置（装配 Bean、设定框架行为）
├── constant/            # 常量
├── controller/          # HTTP 控制器（请求入口）
│   └── admin/           # 管理后台控制器
├── dao/                 # 复杂 SQL 查询接口（多表 join）
├── dto/                 # 请求/响应数据传输对象（前后端传递）
│   └── admin/           # 管理后台 DTO
├── entity/              # 数据库实体映射（字段一一对应表）
├── enums/               # 枚举集中管理
├── event/               # 领域事件与监听器
├── exception/           # 异常体系
├── mapper/              # MyBatis Mapper（单表 CRUD）
├── security/            # Spring Security 框架集成
├── service/             # 业务服务
│   ├── file/            # 文件领域
│   │   └── impl/
│   ├── share/           # 分享领域
│   │   └── impl/
│   ├── system/          # 系统服务（认证、邮件、验证码等）
│   │   └── impl/
│   └── user/            # 用户领域
│       └── impl/
└── utils/               # 工具类
```

### 1.2 命名规范

#### 1.2.1 通用规则

| 元素 | 规范 | 示例 |
|------|------|------|
| 类名 | PascalCase | `FileService`, `UserController` |
| 方法名 | camelCase，动词开头 | `findById()`, `uploadFile()` |
| 变量名 | camelCase | `userId`, `parentId` |
| 常量 | UPPER_SNAKE_CASE | `DEFAULT_QUOTA`, `ROOT_PARENT_ID` |
| 包名 | 全小写，按领域划分 | `service.file`, `controller.admin` |
| 配置文件 | kebab-case | `application-dev.yml`, `logback-spring.xml` |

#### 1.2.2 子域词表

| 子域 | 范围 |
|------|------|
| `Auth` | 认证（登录/注册/登出/验证码） |
| `User` | 用户自身管理 |
| `File` | 文件操作 |
| `Share` | 分享 |
| `RecycleBin` | 回收站 |
| `Team` | 团队空间 |
| `System` | 系统服务（邮件、验证码、日志等） |
| `Dashboard` | 仪表盘 |
| `Log` | 操作日志 |
| `Settings` | 系统设置 |
| `Account` | 管理员账号管理 |
| `Admin` | 后台管理前缀（与子域组合，如 `AdminUser`） |

#### 1.2.3 各层命名

| 包 | 模式 | 示例 |
|----|------|------|
| `entity/` | `{表名单词}` | `User`、`File`、`Share`、`Team` |
| `mapper/` | `{Entity}Mapper` | `UserMapper`、`FileMapper`、`ShareMapper` |
| `mapper/xml` | `{Entity}Mapper.xml` | `UserMapper.xml`、`FileMapper.xml` |
| `controller/` | `{子域}Controller` | `AuthController`、`FileController`、`UserController` |
| `controller/admin/` | `Admin{子域}Controller` | `AdminUserController`、`AdminFileController` |
| `service/` | `{子域}Service` | `AuthService`、`FileService`、`UserService` |
| `service/impl/` | `{子域}ServiceImpl` | `AuthServiceImpl`、`FileServiceImpl` |
| `dao/` | `{子域}{用途}Dao` | `FileStatsDao`、`DashboardStatsDao` |
| `bo/` | `{子域}{用途}BO` | `FileDetailBO`、`UserQuotaBO`、`DashboardStatsBO` |
| `dto/` | `PageQuery` / `PageResult`（分页固定命名） | `PageQuery`、`PageResult` |
| `dto/request/` | `{动作}Request` 或 `{子域}{动作}Request` | `LoginRequest`、`UploadInitRequest` |
| `dto/response/` | `{子域}{描述}Response` | `UserProfileResponse`、`QuotaResponse` |
| `dto/admin/` | `Admin{动作}Request` / `Admin{动作}Response` | `AdminCreateUserRequest` |
| `enums/` | `{领域}Enum` | `RoleEnum`、`UserStatusEnum`、`FileStatusEnum` |
| `constant/` | `{领域}Constants` | `FileConstants`、`RedisConstants`、`SecurityConstants` |
| `config/` (配置类) | `{技术/领域}Config` | `SecurityConfig`、`RedisConfig`、`WebMvcConfig` |
| `config/` (属性类) | `{技术/领域}Properties` | `JwtProperties`、`MinioProperties`、`MailProperties` |
| `event/` (事件) | `{子域}{动作}Event` | `UserRegisterEvent`、`FileUploadEvent`、`ShareCreateEvent` |
| `event/` (监听器) | `{子域}{动作}Listener` | `OperationLogListener`、`QuotaListener` |
| `annotation/` | `{用途}`（无前缀后缀，纯业务含义） | `OperationLog`、`RequirePermission`、`CurrentUser` |
| `aspect/` | `{用途}Aspect` | `OperationLogAspect`、`PermissionAspect`、`RepeatSubmitAspect` |
| `authorization/` | `{子域}Authorization` | `FileAuthorization`、`ShareAuthorization`、`TeamAuthorization` |
| `exception/` | `{异常类型}Exception` | `BusinessException`、`NotFoundException`、`ForbiddenException` |
| `exception/` (全局处理) | `GlobalExceptionHandler` | `GlobalExceptionHandler` |
| `utils/` | `{技术/领域}Util` | `JwtUtil`、`IpUtil`、`DateUtil`、`HashUtil`、`FileUtil` |

#### 1.2.4 方法命名（全项目统一）

| 操作 | 方法名 |
|------|--------|
| 查询单个 | `findById()`、`findByXxx()` |
| 查询列表 | `list()`、`listByXxx()` |
| 分页查询 | `page()` |
| 新增 | `create()` |
| 保存 | `save()` |
| 修改 | `update()` |
| 物理删除 | `delete()` |
| 逻辑删除 | `remove()` |
| 恢复 | `restore()` |
| 上传 | `upload()` |
| 下载 | `download()` |
| 校验 | `validate()` |
| 检查 | `check()` |
| 判断 | `isXxx()`、`hasXxx()` |

#### 1.2.4 API 路径

| 规则 | 说明 |
|------|------|
| 基础路径 | `/api/{模块}`，如 `/api/files` |
| 风格 | RESTful：`GET /api/files`, `POST /api/files/directory`, `PUT /api/files/{id}/rename`, `DELETE /api/files/{id}` |
| 管理端 | `/api/admin/{模块}`，如 `/api/admin/users` |
| 响应 | 统一返回 `Result<T>` |

### 1.3 实体设计

- 使用 Lombok `@Data`
- 数据库字段下划线 → 实体驼峰（MyBatis `map-underscore-to-camel-case: true`）
- 枚举存储为 TINYINT（自定义 `value` 字段，禁用 `ordinal()`），在 `MyBatisTypeHandlerConfig` 中注册

### 1.4 异常处理

| 机制 | 说明 |
|------|------|
| 业务异常 | 抛出 `BusinessException(ErrorCode, message)` |
| 参数校验 | `@Valid` + `jakarta.validation` 注解 |
| 全局处理 | `GlobalExceptionHandler` 统一捕获返回 `Result` |

### 1.5 操作日志

- 关键操作直接注入 `OperationLogService` 记录，不引入 BaseController 继承
- 记录范围：登录、上传、下载、删除、分享、创建团队等

---

## 2. 前端规范 (Vue 3 + TypeScript)

### 2.1 项目结构

```
src/
├── api/             # API 调用层（Axios 封装）
├── assets/          # 静态资源
├── components/      # 通用组件
├── composables/     # 组合式函数（useUpload, useWebSocket）
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

- 页面组件放 `views/`，通用组件放 `components/`
- 每个组件一个文件，使用 `<script setup lang="ts">`
- 事件绑定用 `@click`（不用 `v-on:click`）

### 2.4 API 请求

- 统一通过 `src/utils/request.ts` 封装的 Axios 实例
- Token 自动从 localStorage 读取并注入 Authorization 头
- 401 响应自动跳转登录页

### 2.5 状态管理

| 规则 | 说明 |
|------|------|
| 全局状态 | Pinia Store 管理（userStore, fileStore 等） |
| 组件状态 | `ref` / `reactive` |
| 持久化 | 避免直接操作 localStorage |

### 2.6 UI 规范

- 组件库使用 Element Plus，中文语言包
- 表格操作列用图标 + 文字组合
- 重要操作（删除等）需 `ElMessageBox.confirm` 二次确认

---

## 3. Git 规范

### 3.1 提交信息

格式：`{类型}: {描述}`

| 类型 | 说明 | 示例 |
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
- 提交信息说清"为什么改"而非"改了哪些文件"
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
