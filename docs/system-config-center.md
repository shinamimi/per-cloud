# 系统配置中心功能方案

## 一、功能边界

系统配置中心是管理端的一个独立页面，集中管理所有系统级配置项，按功能分组，各组独立保存。

| 分组 | 配置项 | 可改角色 | 状态 |
|------|--------|---------|:----:|
| 上传限制 | 单文件大小上限、同时上传任务数（普通/VIP） | OPERATOR | ✅ 已有接口 |
| 存储限制 | 新用户默认配额（普通/VIP）、老用户配额调整 | OPERATOR | 新增 |
| 会话安全 | Access Token 有效期、验证码有效期、登录失败锁定、重置密码有效期 | OPERATOR | 新增 |
| 缓存策略 | 验证码/登录失败/黑名单/预览/下载链接 TTL | OPERATOR | 新增 |
| 系统功能 | 开放注册/游客分享/邮件验证/验证码/操作日志开关 | ADMIN | 新增 |
| 文件管理 | 回收站保留天数、分享有效期、分享次数、提取码 | OPERATOR | 新增 |
| 邮件服务 | SMTP 开关、连接信息、发件人名称、频率限制 | ADMIN | 新增 |
| 日志 | 操作/登录日志保存天数 | OPERATOR | 新增 |

## 二、权限分级

- **两级权限**：OPERATOR+ 可访问配置中心
  - 普通配置（上传/存储/会话/缓存/文件/日志）：OPERATOR 可改
  - 敏感配置（SMTP 连接信息、系统功能开关）：**仅 ADMIN** 可改
- OPERATOR 对敏感分组**只读展示**（禁用输入框），ADMIN 可编辑
- 后端接口同样校验角色，前端只读展示 + 后端拦截双保险

## 三、接口清单

| 方法 | 路径 | 角色 | 说明 |
|------|------|------|------|
| GET | `/api/admin/settings` | OPERATOR | 返回全部分组配置 |
| PUT | `/api/admin/settings/upload` | OPERATOR | 保存上传限制 |
| PUT | `/api/admin/settings/storage` | OPERATOR | 保存新用户默认配额 |
| PUT | `/api/admin/settings/session` | OPERATOR | 保存会话安全 |
| PUT | `/api/admin/settings/cache` | OPERATOR | 保存缓存策略 |
| PUT | `/api/admin/settings/system` | ADMIN | 保存系统功能开关 |
| PUT | `/api/admin/settings/file` | OPERATOR | 保存文件管理 |
| PUT | `/api/admin/settings/mail` | ADMIN | 保存邮件服务 |
| PUT | `/api/admin/settings/log` | OPERATOR | 保存日志 |
| POST | `/api/admin/settings/users/quota-batch` | OPERATOR | 老用户配额批量调整 |

## 四、分组配置明细

### 4.1 上传限制

| key | 说明 | 输入 |
|-----|------|------|
| upload.max-size-user | 普通用户单文件大小上限 | 可读单位（MB/GB 选择器） |
| upload.max-size-vip | VIP 单文件大小上限 | 可读单位 |
| upload.max-concurrent-user | 普通用户并发上传数 | 数字 |
| upload.max-concurrent-vip | VIP 并发上传数 | 数字 |

- 留空 = 恢复配置文件默认值（删除 t_setting 行）

### 4.2 存储限制

**新用户默认配额：**

| key | 说明 |
|-----|------|
| storage.default-quota-user | 普通用户新注册默认配额 |
| storage.default-quota-vip | VIP 新注册默认配额 |

- 保存后只影响**以后新注册**用户（注册时写入 quota 字段）

**老用户配额调整：**

- 通过日期范围 + 角色/状态过滤圈定用户，批量修改其 quota（基础值，非 adminBonusQuota）
- 见 §五 单独说明

### 4.3 会话安全

| key | 说明 |
|-----|------|
| session.access-token-ttl | Access Token 有效期（如 2 小时） |
| session.captcha-ttl | 验证码有效期 |
| session.login-lock-threshold | 登录失败锁定次数 |
| session.login-lock-duration | 锁定时间 |
| session.reset-password-ttl | 密码重置链接有效期 |

- 本期**不引入 Refresh Token**，会话到期需重新登录（扩展预留）

### 4.4 缓存策略

| key | 说明 |
|-----|------|
| cache.captcha | 验证码缓存 TTL |
| cache.login-attempt | 登录失败计数 TTL |
| cache.blacklist-token | 黑名单 Token TTL（默认取 Access Token 有效期兜底） |
| cache.file-preview | 文件预览缓存 TTL |
| cache.download-link | 下载链接 TTL（presigned URL 有效期） |

- 全部走 t_setting，管理员可配置
- **秒传索引缓存本期不做**（扩展预留）

### 4.5 系统功能（ADMIN）

| key | 说明 |
|-----|------|
| system.allow-register | 是否开放注册 |
| system.allow-guest-share | 是否允许游客分享 |
| system.enable-mail-verify | 是否开启邮件验证 |
| system.enable-captcha | 是否开启登录验证码 |
| system.enable-operation-log | 是否开启操作日志 |

- 开关**保存即生效**，下次请求按新配置处理

### 4.6 文件管理

| key | 说明 |
|-----|------|
| file.recycle-bin-days | 回收站保留天数（ADR-004 定 30，改可配置） |
| share.default-valid-days | 分享默认有效期 |
| share.max-valid-days | 分享最长有效期 |
| share.max-count-per-file | 同一文件最大分享次数 |
| share.default-require-password | 分享默认是否要求提取码 |

### 4.7 邮件服务（ADMIN）

| key | 说明 |
|-----|------|
| mail.enabled | SMTP 开关 |
| mail.host / mail.port / mail.username / mail.password | SMTP 连接信息（敏感，ADMIN） |
| mail.from-name | 发件人显示名 |
| mail.frequency-limit | 邮件频率限制 |

- SMTP 密码保存时**二次确认**
- OPERATOR 只读展示此分组

### 4.8 日志

| key | 说明 |
|-----|------|
| log.operation-days | 操作日志保存天数 |
| log.login-days | 登录日志保存天数 |

## 五、老用户配额批量调整

### 5.1 交互流程

```
[存储限制分组]

新用户默认配额（普通/VIP）── 保存影响新用户

────── 分隔 ──────

老用户配额调整:
  注册日期: 从 [____] 到 [____]
  角色:     [全部/普通/VIP]
  状态:     [全部/正常/禁用/锁定/未活跃]
  新配额:   普通 [____]   VIP [____]
  [预览受影响用户] → [执行批量调整]
```

### 5.2 预览

- 点击"预览"后调用接口，返回**受影响用户数量**
- 点击"查看明细"弹出小窗，展示只读表格：ID/用户名/邮箱/角色/状态/注册时间/当前配额/新配额
- 预览表格使用独立轻量组件（只读，不含操作列），不直接复用 AdminUserView 的完整表格（避免耦合操作逻辑）

### 5.3 过滤条件

| 条件 | 说明 |
|------|------|
| 注册日期范围 | 必选（起止日期） |
| 角色 | 全部/普通/VIP |
| 状态 | 全部/正常/禁用/锁定/未活跃 |

### 5.4 接口

```
POST /api/admin/settings/users/quota-batch
{
  "startDate": "2026-01-01",
  "endDate": "2026-06-30",
  "role": "USER|VIP|ALL",
  "status": "ALL|NORMAL|DISABLED|LOCKED|INACTIVE",
  "targetQuotaUser": 21474836480,
  "targetQuotaVip": 214748364800
}
```

- 返回受影响用户数量
- 执行前前端二次确认，后端幂等（多次执行结果一致）
- **只改 quota 基础字段**，不触碰 adminBonusQuota / rewardQuota

## 六、前端页面

### 6.1 路由

```
/admin/settings  →  SystemConfigCenterView.vue   （OPERATOR+）
```

### 6.2 布局

- 左侧分组 Tab（上传/存储/会话/缓存/系统/文件/邮件/日志）+ 右侧当前分组表单
- 各分组**独立保存**按钮
- 敏感分组（邮件、系统）按当前角色决定只读/可编辑

### 6.3 axios 分类

- `src/api/admin/settings.ts`：新增各分组保存方法 + quota-batch

## 七、需要做的事

### 后端
1. **SettingMapper 扩展**：批量查询/写入配置（现有 findByKey/upsert/deleteByKey 基础上加）
2. **AdminSettingsService 扩展**：新增存储/会话/缓存/系统/文件/邮件/日志各分组读写
3. **配置读取接入点**：上传限制、token 有效期、验证码、登录锁定、黑名单、回收站天数、分享有效期等读取处改为走 t_setting（无配置用 yml 默认）
4. **AdminSettingsController 扩展**：新增各分组 PUT + quota-batch 接口
5. **quota-batch 接口**：日期范围 + 角色/状态过滤查询 + 批量更新 + 返回数量
6. **权限**：PUT /system 和 /mail 要求 ADMIN（SecurityConfig 细化）
7. **敏感配置保护**：SMTP password 等敏感值返回时脱敏，保存时校验
8. **UserPreviewTable** 数据接口复用：quota-batch 预览可复用用户查询能力

### 前端
9. **SystemConfigCenterView.vue**：Tab 分组布局 + 各分组独立保存
10. **api/admin/settings.ts**：各分组请求方法
11. **老用户配额调整区**：日期范围 + 角色/状态过滤 + 预览/明细弹窗
12. **UserPreviewTable.vue**：只读预览表格组件（可复用）
13. **敏感项二次确认**：SMTP 密码保存时确认弹窗

## 八、变更范围

### 涉及文件
- `controller/admin/AdminSettingsController.java`
- `service/admin/AdminSettingsService.java` + `impl`
- `mapper/SettingMapper.java` + `SettingMapper.xml`
- `dto/admin/`：各分组请求 DTO、QuotaBatchRequest
- `config/SecurityConfig.java`（/settings/system、/settings/mail 限 ADMIN）
- `config/FileProperties.java`（新增默认值）
- 各服务读取处（JwtTokenUtil、CaptchaService、LoginAttemptService、JwtBlacklistService、RecycleBinService、ShareService）接入 t_setting
- `resources/application-local.yml`（新默认值）
- 前端新增 `views/admin/SystemConfigCenterView.vue`、`api/admin/settings.ts`、`components/common/UserPreviewTable.vue`

### 禁止修改
- 现有用户管理/管理员管理接口
- 配额模型字段（adminBonusQuota/rewardQuota 语义不变）
- 其他 Admin 控制器
