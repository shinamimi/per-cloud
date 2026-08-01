# Bug Fix Log 4

## 2026-08-01 — Safari 进入团队页整页卡死（WebSocket 相对地址被拒）

**现象：** 仅在 Safari 中，点击"进入团队"跳转到 `/teams/:id/files`（`TeamFilesView`）后整个页面卡死：点击任何地方无响应、看不到团队文件、连左侧导航栏都无法跳转。Chrome 完全正常。Safari 控制台报错：

```
[Error] Wrong url scheme for WebSocket http://localhost:5173/ws/progress?token=...
	connectWs (ws.ts:23)
	ensureConnected (upload.ts:32)
	init (upload.ts:66)
	wrappedAction (pinia.js:1107)
	（匿名函数）(TeamFilesView.vue:145)  ← uploadStore.init() 在 onMounted 中被调用
```

**根因（Safari 与 Chrome 对 WebSocket 构造器入参的兼容性差异）：**

1. **相对地址只对 Chrome 宽容。** `utils/ws.ts` 里用 `WS_BASE`（`VITE_WS_URL || '/ws'`）直接拼接出**相对路径** `new WebSocket('/ws/progress?token=...')`。WebSocket 规范要求 `ws://` 或 `wss://` 的绝对地址，Chrome 会自动把相对路径按当前页面 origin 补全后建立连接，因此开发中一直正常；Safari 按规范严格校验 scheme，把 `/ws/...` 补全成 `http://...` 后判定 scheme 非法，同步抛出 `SyntaxError: Wrong url scheme for WebSocket`。

2. **异常沿挂载链上抛，页面初始化中断。** `connectWs()` 抛错 → `uploadStore.ensureConnected()` → `uploadStore.init()` 同步抛出。而 `TeamFilesView.onMounted` 里调用的是 `uploadStore.init()`（同步、未 await 且无 try/catch），其后同一钩子中的 `getTeamDetail(...)` 与 `load()`（拉取团队详情和文件列表）**全部不会执行**。于是团队文件列表接口根本没发出，页面停留在空表/加载态，视觉上就是"卡死"。

3. **为什么 Chrome 没暴露。** Chrome 容忍相对地址 → WS 正常连上 → `init()` 正常走完 → 详情和列表照常加载，功能完好。错误只存在于 Safari，故此前未在 Chrome 上发现。

**影响面：** 所有调用 `uploadStore.init()` 的页面（`FileView`、`TeamFilesView`）在 Safari 下都会触发同样的异常；个人文件页因布局/数据路径不同表现为功能异常，团队页则表现最明显（整页卡死）。

**修复（两层）：**

| 文件 | 变更 |
|---|---|
| `frontend/src/utils/ws.ts` | 新增 `resolveWsUrl(base)`：非 `ws://`/`wss://` 前缀的 base 一律按当前页面补全为绝对地址——`https` 页面派生 `wss://`，否则 `ws://`，host 沿用 `window.location.host`；已配置完整地址（如 `wss://...`）则原样透传。`connectWs()` 用 `resolveWsUrl(WS_BASE)` 拼出 `ws://localhost:5173/ws/progress?token=...`（由 Vite `/ws` 代理转发到后端 `ws://localhost:8081`） |
| `frontend/src/stores/upload.ts` | `ensureConnected()` 用 try/catch 兜底：WS 连接失败仅退化为"无实时进度推送"（上传/页面功能不受影响），避免任何连接异常再阻断页面挂载或上传流程 |

**验证：** `npx vue-tsc --noEmit` 通过；构造后的地址为 `ws://localhost:5173/ws/progress?token=...`，与 `vite.config.ts` 中 `/ws → ws://localhost:8081 (ws: true)` 的代理匹配。生产环境 https 下会自动派生 `wss://<host>/ws/...`，与 Nginx 反代一致。

**遗留说明：** 若后端部署在独立域名/端口（非当前页面同源），需显式配置 `VITE_WS_URL=wss://...`，`resolveWsUrl` 会原样透传。

## 2026-08-01 — 团队文件"创建人"字段返回乱码（t_user.nickname 历史数据编码损坏）

**现象：** 团队文件列表新增 `uploaderName`（创建人）字段后，接口返回的中文昵称显示为乱码（如 `æµ‹è¯•2`），而团队名等其它中文（走同一 JDBC 链路）显示正常。

**根因：** 数据库 `t_user.nickname` 中存储的本身就是乱码——tester1/tester2 的昵称在**写入时**连接字符集不正确，UTF-8 字节被按 latin-1 解释后入库，与本次字段改动无关。JDBC 读取链路（`map-underscore-to-camel-case`、utf8mb4）验证无问题。

**修复：**

| 对象 | 变更 |
|---|---|
| `cloud.t_user`（数据修复） | 用 `mysql --default-character-set=utf8mb4` 将 id=7（tester1）、id=8（tester2）的 `nickname` 修正为「测试1」「测试2」 |

**验证：** 修复后 `GET /api/teams/1/files` 成员视角与 `GET /api/admin/teams/1/files` 管理员视角均返回 `uploaderName=测试2`（UTF-8 正常）。

**遗留说明：** 若后续其它用户昵称/资料仍有乱码，属同类历史写入问题，需按 `utf8mb4` 连接修复数据；代码层无需改动。
