# Bug Fix Log 5

## 2026-08-02 — 浏览器仍可"下载"被管理员禁用的文件，且操作日志缺失

**现象：** 管理员禁用文件后，用户端浏览器（Safari/Chrome）点击下载似乎仍能"下载"到文件（下载管理器出现文件），但管理员后台的操作日志中看不到这次下载记录（"日志无法记录"）。

**根因（两个环节叠加）：**

1. **后端被禁响应不是 302 而是 200+JSON。** 下载成功时接口返回 302 → MinIO presigned URL；被禁时全局异常处理器返回 `HTTP 200 + {"code":10216,"message":"文件已被管理员禁用"}`。前端 `utils/request.ts` 的 `requestBlob` 响应拦截器**不检查业务码**，把错误 JSON 当作文件内容交给 `saveBlob(blob, file.name)`——浏览器于是"下载"成功（实测落盘文件内容就是 10216 的 JSON，用户误以为文件能下载）。真正的内容并未泄露（MinIO 直连无签名返回 403，无法 URL 绕过）。

2. **日志只在成功路径记录。** `LogAspect` 的 `@Around` 仅 `proceed()` 成功后才记操作日志；业务异常（10216）时日志代码被跳过 → 被拒的下载尝试无记录。

**修复：**

| 文件 | 变更 |
|---|---|
| `frontend/src/utils/request.ts` | `requestBlob` 拦截器识别 `content-type: application/json` 的错误响应：解析 body，业务码非 200 时 `ElMessage.error(message)` 并 reject，不再保存为文件 |

**验证：** 被禁文件下载接口实测返回 `{"code":10216,...}`；恢复启用后返回 302 presigned URL 正常下载；`npx vue-tsc --noEmit` 与 `npm run build` 通过。浏览器点击下载现在会弹出"文件已被管理员禁用"提示，不再落盘 JSON。

**遗留说明：** 若需"被拒下载也记录操作日志"，需调整 `LogAspect`（try/catch 或前置记录），当前保持仅成功路径记日志。

## 2026-08-02 — 文件管理列表混入文件夹

**现象：** 管理后台"文件管理"列表里出现文件夹（如 `docs` 目录），文件夹并非管理目标（不可下载/预览），造成列表噪声。

**根因：** `FileMapper.xml` 的 `adminWhere`（`adminPage`/`adminCount` 共用）只过滤了 `status != 0`，未排除目录。

**修复：**

| 文件 | 变更 |
|---|---|
| `backend/src/main/resources/mapper/FileMapper.xml` | `adminWhere` 追加 `AND is_directory = 0` |

**验证：** `GET /api/admin/files` 不再返回任何目录（实测 tester1 列表从含目录变为仅 3 个文件）。

## 2026-08-02 — 全站禁用（GLOBAL）后无法恢复启用

**现象：** 管理员对文件执行"全站禁用"后再点"启用"，文件仍处于禁用状态，无法恢复（对同一 hash 的其它用户文件也持续被禁）。

**根因：** 对象级禁用按内容 hash 记录（`t_disabled_object`：GLOBAL=全站 / USER=仅该用户）。启用的 `enableObject(file, scope)` 依赖前端传入的 scope，前端"启用"按钮不传 scope（默认 USER），于是只删除了"该用户 USER"记录；GLOBAL 记录仍在，重放逻辑会再次禁用全站同名 hash 文件。

**修复：**

| 文件 | 变更 |
|---|---|
| `backend/.../service/admin/impl/AdminFileServiceImpl.java` | `enableObject(file)` 重构：不再依赖 scope——启用时同时删除该 hash 的 **GLOBAL 记录 + 该用户 USER 记录**，`restoreByHash` 恢复全部后重放剩余记录 |
| `backend/.../dto/admin/AdminFileResponse.java` | 新增 `fileHash`、`disabledScope`（GLOBAL/USER/null），后台列表/详情可区分禁用来源 |

**验证：** 实测链路：161 禁用（GLOBAL）→ 用户下载 10216 被拒 → 管理员启用（不带 scope）→ 下载恢复 302 → 列表状态 NORMAL、`disabledScope=null`、`t_disabled_object` 清空 ✓。

**说明：** "前端红色『全站禁用』区分展示"为功能优化（`disabledScope` 字段 + 状态标签），随本次修复一并落地，未单独记录。
