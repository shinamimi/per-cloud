# Bug Fix Log 7

## 2026-08-13 — 公网 HTTP 部署后上传报 `undefined is not an object (evaluating 'crypto.subtle.digest')`，本地正常

**现象：** 云服务器部署后（`http://szfanrongkj.com` 公网访问）上传文件报 `undefined is not an object (evaluating 'crypto.subtle.digest')`；本地开发（localhost）完全正常。

**根因：** `frontend/src/utils/upload.ts` 的 `sha256()` 用 **Web Crypto API**（`crypto.subtle.digest('SHA-256', ...)`）计算文件哈希（秒传全站索引）。浏览器规范要求 `crypto.subtle` **仅在安全上下文（HTTPS，或 localhost/file://）下可用**：
- 本地 `http://localhost:5173` → 属安全上下文，`crypto.subtle` 正常
- 公网 `http://101.35.233.30`（HTTP 明文 IP/域名）→ 非安全上下文，`crypto.subtle` 为 `undefined`，调用即抛该错

**修复：** 为 `sha256()` 增加**纯 JS 降级**——有 `crypto.subtle` 用 Web Crypto（性能好），无则降级 `js-sha256`（结果一致的纯 JS 实现），公网 HTTP 也能算哈希：

| 文件 | 变更 |
|---|---|
| `frontend/package.json` | 新增依赖 `js-sha256@^1.0.0`（自带 TypeScript 类型）。`npm install` 同时生成 `package-lock.json` |
| `frontend/src/utils/upload.ts` | `sha256()` 改为：`if (globalThis.crypto?.subtle)` 走 `crypto.subtle.digest`，否则 `jsSha256(new Uint8Array(buffer))` 降级 |

**关键代码（upload.ts）：**

```ts
export async function sha256(file: File): Promise<string> {
  const buffer = await file.arrayBuffer()
  if (globalThis.crypto?.subtle) {
    const digest = await crypto.subtle.digest('SHA-256', buffer)
    return Array.from(new Uint8Array(digest))
      .map((byte) => byte.toString(16).padStart(2, '0'))
      .join('')
  }
  return jsSha256(new Uint8Array(buffer))
}
```

**验证：** 本地 `npm run build` 通过（含 `vue-tsc -b` 类型检查）；构建产物确认降级逻辑被打进 `TransferQueue-*.js` chunk（含 `globalThis.crypto` 分支）；上传 `dist/` 至服务器重建 frontend 容器，容器内 bundle 实测含 `globalThis.crypto` 与两层逻辑，`http://101.35.233.30` 正常提供。

**部署方式说明（后端/前端流水线差异）：** 后端 Dockerfile 在服务器镜像内编译（需要 `src/` 源码）；前端选择**本地 `npm run build` 产出 `dist/`**，Dockerfile 只 `COPY dist` 进 Nginx 镜像，因此服务器**不传 `src/` 源码**、只传构建产物——这是有意的流水线设计（本地构建快、产物小、服务器省去 npm 依赖拉取），非测试期临时做法。改动发布链路：改 `src/` → 本地 build → 上传 `dist/` → `docker compose up -d --build frontend`。

**遗留说明：**
- 根治方案仍是接入 **HTTPS**（`crypto.subtle` 原生可用，降级代码永不触发）：已有域名 `szfanrongkj.com` 解析在，443 无服务，可装 1Panel 或 certbot 签证书；详见 `docs/migrate-1panel-https.md`。
- 纯 JS 降级对超大文件哈希耗时较高（Web Crypto 走原生，JS 逐块算），2GB+ 文件在 HTTP 下降级路径会更慢，属可接受权衡。