# Bug Fix Log 3

## 2026-08-01 — 用户管理页不显示 ADMIN/OPERATOR，管理员管理页不显示 OPERATOR

**现象：** 用户管理页面只显示 USER 和 OPERATOR（看不到 ADMIN）；管理员管理页面只显示 ADMIN（看不到 OPERATOR，如 `op1`）。

**根因：** 两个列表接口的过滤条件与预期不符：

- `AdminUserController.listUsers()` 用 `filter(u -> u.getRole() != Role.ADMIN && u.getRole() != Role.SUPER_ADMIN)`，把 ADMIN 也排除了；
- `AdminAccountController.listAdmins()` 用 `filter(u -> u.getRole().getValue() >= Role.ADMIN.getValue())`，`Role.ADMIN.getValue() = 20`，而 OPERATOR 的 value = 10，被过滤掉。

**修复：**

| 文件 | 变更 |
|---|---|
| `backend/src/main/java/.../controller/admin/AdminUserController.java:28` | 过滤条件改为只排除 `Role.SUPER_ADMIN`（用户管理页显示 USER/OPERATOR/ADMIN） |
| `backend/src/main/java/.../controller/admin/AdminAccountController.java:30` | 过滤条件改为 `u.getRole() == Role.OPERATOR || u.getRole() == Role.ADMIN`（管理员管理页显示 OPERATOR/ADMIN） |
| `frontend/src/views/admin/AdminUserView.vue` | 用户管理表格新增"权限等级"列（角色 Tag，label 从字典 role 组读取，颜色前端维护） |

---

## 2026-08-01 — 上传第二个文件时永远被拒（并发限制判断错误）

**现象：** 上传并发限制（`maxConcurrent = 1`）下，第一次上传完成后，再传任何文件都失败：任务在传输队列里显示"失败"，提示"上传任务数超过限制"。期望行为是剩余任务显示"等待中"，同时仅传一个，而不是直接失败。

**根因（两条链路叠加）：**

1. **后端残留任务占坑：** 并发限制用 Redis Set `upload:uploading:{userId}` 记录进行中任务（`UploadServiceImpl.checkConcurrentTasks`）。某次测试中断（浏览器关闭/未 merge）后 uploadId 残留在此 Set 中，meta TTL 长达 `uploadExpireHours`（实测约 14 小时）。此后每次 `upload/init` 都会先 `add` 再计数，count=2 > limit=1 → 抛 `UPLOAD_TASK_EXCEEDED(10208)` → **所有新上传全部被拒**，且无任何清理机制。
2. **前端无排队策略：** `uploadOneFile` 抛错后 store 直接 `task.status = 'failed'`，任务以"失败"展示在传输队列；且 `request.ts` 拦截器 reject 时只传 `Error(message)`，**业务错误码被丢弃**，前端无法区分 10208 与其他错误。

**修复（三层）：**

| 文件 | 变更 |
|---|---|
| `backend/.../service/file/impl/UploadServiceImpl.java:145` | `checkConcurrentTasks` 超限时先惰性清理：移除 Set 中"元数据已不存在"（meta TTL 过期/已 merge 清理）的残留任务，再重新计数，仍超限才拒绝 |
| `frontend/src/utils/request.ts:82` | 拦截器 reject 时将业务 `code` 挂到 Error 对象上（`err.code = body.code`） |
| `frontend/src/stores/upload.ts` | 捕获 10208 时不标 failed：任务保持 `pending`（队列显示"等待中"），3 秒后放回队尾重试；其他错误仍标 failed |

**数据修复：** 测试产生的孤儿任务 `upload:uploading:1` 中的残留 uploadId（`99dcd9...`）已从 Redis 手动移除（其 meta 仍存在，惰性清理无法清除；属中断的测试产物，放弃断点续传）。

**遗留说明：** 中断未 merge 的任务会保留在并发 Set 中直到 meta TTL 过期，期间会占并发名额（前端表现为等待重试）。若需要支持"断点续传复用 uploadId"或更短占坑时间，需后续设计。

---

## 2026-08-01 — 角色枚举存储机制澄清（排查误判记录）

**背景：** 排查 `op1` 用户 `role = 1` 时曾误判为"非法值"（因为枚举 value 为 USER=0/OPERATOR=10/ADMIN=20/SUPER_ADMIN=100）。

**澄清：** `MyBatisTypeHandlerConfig` 为 `Role.class` 注册了 `EnumOrdinalTypeHandler`，数据库存的是**枚举声明顺序 ordinal（0/1/2/3）**，不是 `value` 字段。因此：

| 数据库值 | ordinal | 枚举 |
|---|---|---|
| 0 | USER | USER |
| 1 | OPERATOR | OPERATOR |
| 2 | ADMIN | ADMIN |
| 3 | SUPER_ADMIN | SUPER_ADMIN |

`op1` 的 `role=1` 即 OPERATOR，合法。另外注意：**数据库存 ordinal（0/1/2/3），API 返回 value（0/10/20/100）**，两层数值不同但映射同一角色。后端 `LoginUser.getAuthorities()` 再映射为 `ROLE_OPERATOR` 等 Spring Security 权限串。
