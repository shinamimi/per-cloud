# 前端统一化规范

## 一、职责边界

| 职责 | 归属 |
|------|------|
| 页面布局、UI 设计、交互位置 | 前端 |
| 业务数据（枚举选项、标签、广告页、VIP 套餐、系统配置） | 后端 |
| UI 展示样式（颜色、图标、Tag 类型） | 前端 |

## 二、变动内容来源分类

| 数据类型 | 方案 | 原因 |
|----------|------|------|
| 用户状态、角色、分享状态、操作类型等业务枚举 | 统一字典接口 | 统一管理、统一缓存、避免重复下发 |
| 标签颜色、图标、Badge、UI 样式 | 前端维护 | 属于展示逻辑，便于主题和国际化 |
| 广告、公告、VIP 套餐、系统配置等数据库数据 | 独立业务接口 | 动态业务数据，不是枚举 |
| 权限能力（`canDelete`、`canShare`） | 业务接口直接返回 | 与具体资源和当前用户相关，无法做成统一字典 |

## 三、字典接口契约

### 3.1 接口

```
GET /api/meta/options
```

### 3.2 返回结构

按组（group）组织，每组是 `{ value, label }` 数组：

```json
{
  "groups": {
    "userStatus": [
      { "value": "NORMAL", "label": "正常" },
      { "value": "DISABLED", "label": "禁用" }
    ],
    "role": [
      { "value": "USER", "label": "普通用户" },
      { "value": "OPERATOR", "label": "管理员" },
      { "value": "ADMIN", "label": "超级管理员" }
    ],
    "shareStatus": [
      { "value": "ACTIVE", "label": "有效" },
      { "value": "EXPIRED", "label": "已过期" }
    ],
    "operationType": [
      { "value": "UPLOAD", "label": "上传" },
      { "value": "DOWNLOAD", "label": "下载" }
    ]
  }
}
```

### 3.3 规则

- 只返回业务语义（value + label），**不返回颜色、图标、Tag 类型**（UI 归前端）
- 新增枚举组无需修改接口定义，前端统一通过 `groups.xxx` 获取
- 后端由 MetaService 负责组装

### 3.4 拉取时机

- 登录管理后台后拉取一次，缓存到 Pinia
- **不持久化到 localStorage**，页面刷新后重新请求
- 枚举来自 Java Enum，运行时不变，无需缓存失效机制
- 真正动态可配置的数据（如管理员可新增文件分类）不是枚举，应提供独立接口，不进字典

## 四、显示层混淆规则

仅用于防止非技术人员查看前端页面时产生误解，**后端角色定义不变**。

| 后端角色 | 前端显示名 | 说明 |
|---------|-----------|------|
| OPERATOR (10) | 管理员 | |
| ADMIN (20) | 超级管理员 | 仅超级管理员可操作 |
| SUPER_ADMIN (100) | — | 不暴露在此页 UI 中 |

- 混淆由**后端**在字典接口 role 组中直接返回混淆后的 label，前端零二次映射
- 穿梭器可选的"管理员/超级管理员"两档即来自字典 role 组过滤（排除 USER 和 SUPER_ADMIN）

## 五、前端操作规则表

### 5.1 权限能力来源（区分两类）

| 场景 | 由谁定显隐 |
|------|-----------|
| 基础操作（用户状态、配额、密码重置等），可由当前角色 + 目标状态推导 | 前端规则表推导 |
| 资源相关操作（文件归属、分享权、`canDelete`、`canShare`），无法由角色+状态推导 | 后端业务接口返回能力字段 |

### 5.2 目录结构

```
src/
├── permissions/
│   ├── role.ts                  # ROLE_LEVEL 角色等级
│   ├── admin-operations.ts      # 管理员操作规则
│   ├── file-operations.ts       # 文件操作规则
│   └── share-operations.ts      # 分享操作规则
│
├── utils/
│   └── permission.ts            # can() 推导函数
│
└── views/
    └── admin/
        └── UserManage.vue
```

### 5.3 角色等级（ROLE_LEVEL）

不直接比较字符串，使用数值等级：

```ts
export const ROLE_LEVEL = {
  USER: 1,
  OPERATOR: 2,
  ADMIN: 3,
  SUPER_ADMIN: 4,
}
```

### 5.4 操作规则配置示例

```ts
export const USER_OPERATIONS = {
  disable:   { minRole: 'OPERATOR',    targetStatuses: ['NORMAL'],   type: 'default' },
  enable:    { minRole: 'OPERATOR',    targetStatuses: ['DISABLED'], type: 'default' },
  unlock:    { minRole: 'OPERATOR',    targetStatuses: ['LOCKED'],   type: 'default' },
  quota:     { minRole: 'OPERATOR',    targetStatuses: ['*'],        type: 'default' },
  resetPwd:  { minRole: 'OPERATOR',    targetStatuses: ['*'],        type: 'default' },
  promoteOp: { minRole: 'ADMIN',       targetStatuses: ['NORMAL'],   type: 'primary' },
  setAdmin:  { minRole: 'SUPER_ADMIN', targetStatuses: ['NORMAL'],   type: 'primary' },
}
```

字段含义：`minRole` 当前登录者最低角色；`targetStatuses` 目标行必须满足的状态（`*` 不限制）；`type` 按钮类型。

### 5.5 推导函数

页面只调用 `can()`，不关心规则：

```ts
can('disable', currentUser.role, row.status, USER_OPERATIONS)  // true/false
```

## 六、axios 分类

按后端控制器分类，各模块一个文件：

| 文件 | 内容 | 对应后端 |
|------|------|---------|
| `api/meta.ts` | 字典接口 | MetaController |
| `api/admin/user.ts` | 用户管理（列表/状态/配额/解锁/密码） | AdminUserController |
| `api/admin/admin.ts` | 管理员账号（创建/删除/改角色/设管理员） | AdminAccountController |
| `api/admin/dashboard.ts` | 仪表盘 | AdminDashboardController |
| `api/admin/file.ts` | 文件管理 | AdminFileController |
| `api/admin/share.ts` | 分享管理 | AdminShareController |
| `api/admin/log.ts` | 操作日志 | AdminLogController |
| `api/admin/settings.ts` | 系统设置 | AdminSettingsController |

### 通用组件

穿梭器等通用组件放 `src/components/common/`，不属于任何页面功能，数据由父组件传入。详见 `component-transfer.md`。

## 七、需要做的事

### 前端
1. 新建 `src/api/meta.ts`：封装 `GET /api/meta/options`
2. 新建字典 Pinia store：登录后台后拉取一次，提供 `getGroup(name)` 访问
3. 新建 `src/permissions/role.ts` + `admin-operations.ts` + `utils/permission.ts`
4. 拆分 axios：`admin.ts` → 按模块拆成 `admin/user.ts` 等
5. AdminUserView 状态筛选下拉改为从字典 userStatus 组渲染
6. AdminUserView 操作按钮改为调用 `can()` 推导
7. 新建 `src/components/common/Transfer.vue`（通用穿梭器）
8. AdminAdminView 新增"添加管理员"入口（弹穿梭器），与"创建管理员"对话框并存

### 后端
9. 新增 MetaController + MetaService：`GET /api/meta/options`，role 组返回混淆后的 label
10. 新增 `GET /api/admin/admins/candidates`：候选用户列表（排除已管理员）
11. 新增 `PUT /api/admin/admins/batch`：批量变更角色，请求体 `[{ userId, newRole }]`
