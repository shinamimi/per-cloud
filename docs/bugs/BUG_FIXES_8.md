# Bug Fix Log 8

## 2026-08-13 — 部署后上传报「上传任务不存在或已过期」，根因是 prod 配置缺 `file.upload-expire-hours`（部署环境配置不完整）

**现象：** 云服务器部署后，前端上传文件（走分片流程）立即报 `上传任务不存在或已过期`（code=UPLOAD_NOT_FOUND）；本地开发（localhost）上传完全正常。

**根因：** `getMeta()`（UploadServiceImpl.java:477-485）从 Redis 读上传元数据（`upload:meta:{uploadId}`），读不到或 userId 不符即抛 `UPLOAD_NOT_FOUND`。原因在 `init()`（UploadServiceImpl.java:184-188）写入元数据时用的 TTL：

```java
Duration ttl = Duration.ofHours(fileProperties.getUploadExpireHours());
```

`FileProperties.uploadExpireHours` 是 `@ConfigurationProperties(prefix="file")` 绑定的字段。**`application-prod.yml` 的 `file:` 段只写了 3 个键**（storage-path/chunk-size/max-size），而 `uploadExpireHours` 未配置 → 绑定为默认值 `0` → `Duration.ofHours(0)` → **Redis key 创建即过期** → 后续任何 chunk/progress/merge 都读不到元数据 → `UPLOAD_NOT_FOUND`。

本地正常是因为 `application-local.yml` 的 `file:` 段齐全（`upload-expire-hours: 24`）。这是**部署环境配置不完整**——同一份 `FileProperties` 在 prod 下缺了字段，且 `@ConfigurationProperties` 缺键不报错、静默用零值，没有任何启动日志提示。

**修复：** 补全 `application-prod.yml` 的 `file:` 段为与 local 一致（用默认值）：

| 键 | 值 | 作用 |
|---|---|---|
| `upload-expire-hours` | `24` | 上传临时分片/元数据 TTL（本次根因） |
| `max-size-user` | `536870912` | 普通用户单文件上限（512MB） |
| `max-size-vip` | `2147483648` | VIP 单文件上限（2GB） |
| `max-concurrent-user` | `3` | 普通用户并发上传任务上限 |
| `max-concurrent-vip` | `5` | VIP 并发上传任务上限 |
| `small-file-threshold` | `5242880` | 5MB 以下不分片直传 |
| `package-expire-hours` | `24` | 打包下载产物 TTL |
| `recycle-days` | `30` | 回收站保留天数 |
| `preview-text-max-size` | `1048576` | 文本预览上限（1MB） |

**顺带补全的另两处 prod 缺失（全面对照 local 发现）：**

1. **`quota:` 段缺失**（`quota.default-user`/`quota.default-vip`）。local 配了 5GB/100GB；prod 没有此段，但代码侧 `@Value("${quota.default-user:5368709120}")` 有默认兜底且与 local 一致 → 无功能影响，补齐只为显式化、防将来改 local 不同步。
2. **`mail.from` 缺失**。`MailProperties`（prefix=mail）有 `from` 字段，env 也传了 `MAIL_FROM`，但 prod yml 的 `mail:` 段没映射 `from: ${MAIL_FROM}` → `mailProperties.getFrom()` 为 null，发件人地址回落到 `noreply@cloud.local` 兜底（`EmailService.defaultFrom()`）。补齐 `from: ${MAIL_FROM}` 使配置中心之外的发件人可用。

**验证：** 重新 build 上传后，用 curl 完整走上传流程（init→chunk→merge，fileHash 用真实 SHA-256）：init 返回 `uploadId`，chunk `200`，merge `200` 且 `t_file` 落库 `id=3 name=t.txt size=23 status=1`，MinIO 落盘 `cloud-storage/files/1/3/t.txt`。修复前 init 仅 1 秒后 progress/chunk 即报 10210。

**经验沉淀：**
- **`@ConfigurationProperties` 缺键不报错**，绑定为零值（数字 0、String null），最隐蔽——本次 TTL=0 直接让功能失效，无任何启动告警。**部署到新环境前应逐字段核对 prod yml 与 local yml 的差值**。
- 排查 UPLOAD_NOT_FOUND 的点：Redis 里是否真有 `upload:meta:{uploadId}` key、TTL 多少（`TTL key`）、meta 的 userId 是否等于当前登录用户。本次直接是 TTL 为 0。
- 该 bug 与 BUG 6 的 `t_user` 缺列同属"部署环境配置不完整"家族，根因都是 local 与 prod 的**配置/脚本不同步**。建议后续引入对拍脚本：`application-local.yml` vs `application-prod.yml` 顶层键与各前缀键集合 diff；MySQL 建表脚本 vs 实体字段 diff。

**遗留见 BUG 8 排查中另外发现但未改动的现象：**
- 上传并发标记（`upload:uploading:{userId}`）在 merge 失败后不会立即清理（靠 TTL/惰性清理），测试时需手动 `DEL`；并发上限满时报 10208「上传任务数超过限制」，属既有设计（惰性清理在建），非本次引入。

---

## 2026-08-13 — 公网 HTTP 下分享链接复制失效、文件图片预览空白（本地正常）

**现象：** 云服务器部署后（公网 HTTP），①分享链接"复制链接"按钮点击不复制、"生成时自动复制"也无效；②图片文件预览空白。本地开发全部正常。

### 问题 ① 复制失效

**根因：** 三处调用（`FileView.vue:214` 生成分享后自动复制、`ShareManageView.vue:125` 复制按钮）都用 `navigator.clipboard?.writeText(link)`。`navigator.clipboard` 与 `crypto.subtle`（见 BUG 7）一样，**仅在安全上下文（HTTPS/localhost）可用**，公网明文 HTTP 下为 `undefined` → `?.` 静默跳过，复制从未发生；且 `ShareManageView` 无条件弹 `ElMessage.success('链接已复制')`，误导用户以为成功（`FileView` 的提示文案也写"已复制到剪贴板"）。

**修复：**

| 文件 | 变更 |
|---|---|
| `frontend/src/utils/clipboard.ts` | **新增** `copyText(text)`：有 `navigator.clipboard?.writeText` 用之（HTTPS/localhost，原生异步），否则降级 `document.execCommand('copy')`（临时 textarea + select + setSelectionRange，兼容非安全上下文）；返回 `Promise<boolean>` 真实反映是否写入成功 |
| `frontend/src/views/share/ShareManageView.vue` | `handleCopy` 改用 `copyText`，成功弹"链接已复制"，失败弹"复制失败，请手动复制链接：{link}" |
| `frontend/src/views/files/FileView.vue` | `handleShareCreated` 改用 `copyText`（生成时自动复制），成功后再弹"分享成功"对话框 |

**验证：** 本地 `npm run build` 通过，`clipboard.ts` 独立 chunk（约 0.5KB）；上传 `dist/` 重建 frontend 容器，容器内 bundle 实测含 `execCommand`（降级路径）与原生分支。公网 HTTP 下复制按钮与自动复制现在均可用。

### 问题 ② 图片预览空白

**现象：** 预览接口 `GET /api/files/{id}/preview` 正常返回 presigned URL（`PreviewDialog.vue` 图片用 `thumbnailUrl || url`），但图片加载失败。浏览器控制台先报 CSP 拦截（`http://minio:9000/cloud-storage/...` 不在 `img-src` 白名单），放行后仍打不开 / 报 403 / 000。

**根因（两重叠加，均来自"本地起 3 个服务全 localhost"与"服务器多容器同机"的拓扑差异）：**

1. **presigned URL 的 host 是容器内网名 `minio:9000`**：`StorageServiceImpl.generateDownloadUrl()` 直接用 `minioClient`（endpoint=`http://minio:9000`）生成 URL → 返回 `http://minio:9000/...` → 浏览器既无法解析内网名，又被 `frontend/nginx.conf:37` 的 CSP `img-src` 白名单（只含 `http://101.35.233.30:9000`）拦截。
2. **S3 v4 签名会把 host 签死在签名里（`X-Amz-SignedHeaders=host`）**：尝试"生成后把 host 字符串替换成 `101.35.233.30:9000`"（初版 `rewriteHost()`）时，签名仍是基于 `minio:9000` 计算的 → 浏览器访问公网地址签名校验失败 → **403 SignatureDoesNotMatch**。实证：同一签名 URL，容器内以 `minio:9000` 访问 200、公网以 `101.35.233.30:9000` 访问 403。
3. （部署环境层）腾讯云轻量防火墙此前从未放行 TCP 9000 → 即使 host 对了，公网直连依旧超时 000。

**修复（按"让签名 host ≡ 浏览器访问 host"根治，而非替换字符串）：**

| 文件 | 变更 |
|---|---|
| `backend/src/main/java/com/cloud/backend/config/MinioConfig.java` | 新增 `presignMinioClient` bean：用 `minio.public-url`（`http://101.35.233.30:9000`）作 endpoint，构造的 client **专用于生成 presigned URL**；`minioClient` 保持内网 endpoint 不动作数据面 |
| `backend/src/main/java/com/cloud/backend/service/file/impl/StorageServiceImpl.java` | `generateDownloadUrl()` 改用 `presignMinioClient`，签名 host 即为浏览器可达的公网地址；**删除**不可行的 `rewriteHost()` 字符串替换方案 |

- 数据面（putObject/getObject/removeObject 等）仍走内网 `minioClient`（容器内访问 `minio:9000` 更快更稳）。
- **防火墙**：腾讯云控制台 → 轻量应用服务器 → 防火墙 → 放行 TCP 9000（本次排查确认已放行，公网健康检查 200）。

**验证：** 部署新 jar 后，`preview` 接口返回的 URL host 为 `http://101.35.233.30:9000`；公网直接 `curl` 该 presigned URL 返回 **200** 且下载到真实 JPEG（500x282，16919 字节）。此前同 URL 公网 403、容器内 200。图片预览、下载、分享链接至此全链路可用。

**经验沉淀：**
- presigned URL 不是"生成的字符串可以随便改 host"——S3 v4 签名包含 host，改 host 必 403。必须让签名时用的 endpoint 与浏览器访问地址一致。
- 排查顺序建议：先看浏览器控制台报 CSP 还是网络；本地与服务器拓扑差异（容器内网名 vs 公网地址）是这类"本地正常、线上异常"的高发根因。

**经验沉淀（与 BUG 7 同源）：** 公网 HTTP 部署下，浏览器安全上下文限制会命中一组功能：`crypto.subtle`（哈希）、`navigator.clipboard`（剪贴板）、Service Worker、`navigator.geolocation` 等。**根治仍是接入 HTTPS**（域名 `szfanrongkj.com` 已解析在，签证书即可），HTTP 下则需逐一降级（哈希已降级 BUG 7、剪贴板本次降级）。