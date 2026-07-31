# 文件模块功能方案

> 对应 DDD 文档模块 M3 — 文件管理

## 一、功能边界

| 功能 | 接口 | 说明 |
|------|------|------|
| 文件列表 / 目录树 / 创建目录 | FileController | 目录与文件统一表模型 |
| 分片上传（init/chunk/merge） | FileController | 自适应分片 + 断点续传 + WebSocket 进度 |
| 秒传 | FileController | 全站范围 + 引用计数 |
| 下载 / 批量打包下载 | FileController | presigned URL 重定向；打包异步 + WS 通知 |
| 重命名 / 移动 / 复制 | FileController | 移动仅改数据库 |
| 删除 / 回收站 | FileController | 逻辑删除 + 30 天自动物理清理 |
| 搜索 | FileController | 文件名 + 类型过滤 |
| 预览 | FileController | 图片/视频/音频/PDF/文本（Office 本期不做） |

## 二、接口清单

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/files` | 文件列表（分页，按 parentId 过滤） |
| GET | `/api/files/tree` | 目录树 |
| POST | `/api/files/directory` | 创建目录 |
| POST | `/api/files/upload/init` | 初始化分片上传 |
| POST | `/api/files/upload/chunk` | 上传分片 |
| POST | `/api/files/upload/merge` | 合并分片 |
| POST | `/api/files/upload/sec` | 秒传 |
| GET | `/api/files/{id}/download` | 下载（重定向 presigned URL） |
| POST | `/api/files/download/batch` | 批量打包下载（异步） |
| PUT | `/api/files/{id}/rename` | 重命名 |
| POST | `/api/files/{id}/move` | 移动（仅改数据库） |
| POST | `/api/files/{id}/copy` | 复制（同用户） |
| DELETE | `/api/files/{id}` | 移入回收站（递归） |
| GET | `/api/files/search` | 搜索（文件名 + 类型过滤） |
| GET | `/api/files/{id}/preview` | 预览 |

## 三、MinIO 存储

### 3.1 结构（见 ADR-001）

单 bucket + 前缀分区：

| 用途 | 路径 |
|------|------|
| 分片临时 | `uploads/{userId}/{uploadId}/chunk_{seq}` |
| 个人文件 | `files/{userId}/{fileId}/{objectName}` |
| 团队文件 | `files/team/{teamId}/{fileId}/{objectName}` |
| 缩略图 | `thumbnails/{userId}/{fileId}.jpg` |
| 打包 | `packages/{taskId}.zip` |

### 3.2 访问方式

- 下载/预览均通过后端生成的 **presigned URL**（有效期 10 分钟）重定向，前端直连 MinIO，不占后端带宽
- 文件对象私有，不开放公开读

## 四、上传流程

### 4.1 分片策略

- **自适应**：小文件（阈值以下）不分片直接上传；大文件分片
- 分片大小按文件大小选择（如 5MB / 10MB）
- **断点续传**：前端启动时查询已传分片，只传缺失部分

### 4.2 流程

```
1. 前端计算 SHA256 → POST /sec
   2a. 全站 Hash 命中 → 引用计数 +1 → 秒传完成
   2b. 未命中 → 继续

3. POST /upload/init
   - 校验配额（剩余空间不足 → FILE_QUOTA_EXCEEDED）
   - 校验单文件大小上限（管理员配置，VIP 差异化）
   - 校验并发任务数（管理员配置，VIP 差异化）
   - 生成 uploadId，返回 chunkSize
4. POST /upload/chunk → 写入 MinIO 临时目录，WebSocket 推送进度
5. POST /upload/merge
   - Redis 分布式锁 lock:merge:{uploadId}
   - 合并 → 写 t_file → 配额扣减（used_space += size）→ 清理临时分片
```

### 4.3 配额扣减

- **merge 完成后一次性扣减**，分片阶段不扣
- 删除 → 回收站（不扣配额）；恢复 → 重新占用；30 天物理删除 → 释放配额
- 扣减使用 `UPDATE t_user SET used_space = used_space + #{size}` 原子操作

### 4.4 秒传与引用计数

- 全局 SHA256 索引表，记录对象引用
- 命中秒传：引用计数 +1，每人一条记录
- 删除：引用计数 -1，计数归零才物理删除对象（与回收站 30 天清理联动）
- 团队文件也走统一秒传

### 4.5 并发控制

| 场景 | 策略 |
|------|------|
| 分片上传 | 每个 uploadId 独立，Redis 记录分片状态 |
| 合并冲突 | Redis 分布式锁 `lock:merge:{uploadId}` |
| 并发任务数 | Redis 计数，超限拒绝（限额可配置） |

## 五、下载

### 5.1 单文件

后端生成 presigned URL → 302 重定向到 MinIO → 前端直连下载。

### 5.2 批量打包

- 异步任务：创建打包任务 → 后台收集对象 → 生成 `packages/{taskId}.zip`
- 完成通过 WebSocket 通知（统一 `/ws/progress` 通道）
- 打包文件带过期策略，定期清理

## 六、预览

| 格式 | 方案 | 说明 |
|------|------|------|
| 图片（jpg/png/gif/webp） | 直接预览 + 缩略图 | 缩略图后端 Thumbnailator 生成，存 `thumbnails/` 前缀 |
| 视频（mp4/webm） | 浏览器原生播放 | 支持 Range 请求；转码后续扩展 |
| 音频（mp3/flac/wav/m4a） | 浏览器原生播放 | |
| PDF | 浏览器内置 | |
| 文本（txt/md） | 读内容展示 | |
| Office（docx/xlsx/pptx） | 仅下载 | 在线预览/编辑后续扩展 |

- 预览访问同样走 presigned URL

## 七、删除与回收站

```
用户删除 → 逻辑删除 t_file.status = DELETED + 写 t_recycle_bin（含 objectName）→ MinIO 对象保留
恢复     → 恢复状态 + 删回收站记录
30 天    → 定时任务扫描：物理删除 MinIO 对象（引用计数归零时）+ 删除记录 + 释放配额
```

- **递归删除**：删除目录时递归删除目录下所有文件/子目录
- 回收站保留 **30 天**，自动清理为定时任务
- 秒传对象：引用计数归零才物理删除

## 八、文件模型

### 8.1 统一表模型

- 目录与文件同为 `t_file` 记录，`type` 区分（FILE / DIRECTORY）
- 树形结构通过 `parentId` 组织，根目录 `parentId = 0`
- 团队文件通过归属字段区分（teamId），与个人空间隔离

### 8.2 同名策略

- 重名自动加后缀"（2）""（3）"，与主流网盘一致
- （差异说明：DDD 4.4 原定唯一索引 + FILE_NAME_DUPLICATE 拒绝，本次改为自动加后缀；唯一索引仍保留用于防并发竞态）

### 8.3 操作语义

| 操作 | 实现 |
|------|------|
| 重命名 | 仅改数据库 name |
| 移动 | 仅改数据库 parentId，MinIO 对象不动（路径含 fileId 不变） |
| 复制 | 同用户：copyObject 复制对象 + 新增 t_file 记录；跨用户复制本期不做 |

## 九、搜索

- 文件名 LIKE + 类型过滤（图片/文档/视频/音频…）
- 后续扩展：ES 全文检索（扩展预留）

## 十、上传限制（管理员可配置）

新增系统设置项（AdminSettingsController）：

| 配置项 | 说明 | 默认 |
|--------|------|------|
| 单文件大小上限（普通用户） | 超过拒绝 | 待定 |
| 单文件大小上限（VIP） | 超过拒绝 | 待定 |
| 上传并发任务数（普通用户） | 超过拒绝 | 待定 |
| 上传并发任务数（VIP） | 超过拒绝 | 待定 |

- init 时校验，返回明确错误码
- VIP 差异化由 `is_vip` 标记决定

## 十一、扩展预留（本期不做，仅记录）

| 能力 | 说明 | 触发时机 |
|------|------|---------|
| 音乐播放器独立页 | 类网易云：独立入口页面、播放列表、全局播放条 | 预留接口（音频列表/播放地址/播放记录） |
| WS 集群 | 多实例部署时 Redis Pub/Sub 广播进度 | 多实例部署时 |
| ES 搜索 | 文件名全文检索替代 LIKE | 文件量增长后 |
| 视频转码 | HLS 分片转码，支持大视频 | 后续 |
| Office 在线预览/编辑 | OnlyOffice/Collabora | 后续 |
| 缩略图桶公开读 | 多 bucket 改造时可选 | 规模增长后 |

## 十二、需要做的事

1. MinIO 客户端封装（config + 工具类）
2. 上传链路：init / chunk / merge + 断点续传查询接口 + Redis 分片状态
3. 秒传：SHA256 索引表 + 引用计数表 + 团队统一秒传
4. 下载：presigned URL 生成 + 302 重定向 + 打包异步任务 + WS 通知
5. 预览：各格式处理 + Thumbnailator 缩略图 + Range 支持
6. 删除：回收站表 + 30 天清理定时任务 + 递归删除 + 引用计数释放
7. 文件模型：t_file 统一表（type / parentId）+ 同名自动后缀
8. 搜索：文件名 LIKE + 类型过滤
9. 系统设置：单文件大小上限 + 并发任务数（VIP 差异化）
10. 音乐播放器预留接口（音频列表/播放地址）

## 十三、变更范围

### 涉及文件
- `controller/FileController.java`（含回收站相关端点）
- `service/file/`：FileService / UploadService / DownloadService / PreviewService / RecycleBinService / SearchService
- `mapper/FileMapper.java` + `mapper/FileMapper.xml`
- `dao/`：文件搜索、统计查询（如需）
- `entity/File.java`、`entity/FileHash.java`（秒传索引）
- `dto/`：上传/下载/预览/搜索相关 DTO
- `enums/FileType.java`、`enums/FileStatus.java`
- `config/MinioConfig.java`
- `config/`：回收站清理定时任务
- `service/admin/AdminSettingsService.java`（上传限制配置项）
- `resources/application-local.yml`（MinIO 连接、默认限制值）
- `sql/`：t_file 结构调整、t_file_hash 新表、t_recycle_bin 新表

### 禁止修改
- 现有认证/授权体系
- 其他模块控制器
- 现有配额模型字段
