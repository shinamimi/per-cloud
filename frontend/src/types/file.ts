/*
 * 文件模块（M3）类型定义 —— 对应后端 FileController 的 DTO 与 WebSocket 消息。
 *
 * 设计思路（依据 docs/file-module.md + docs/DDD.md M3/M9）：
 * - 目录与文件统一为 t_file 模型，type 字段区分 FILE / DIRECTORY，parentId 组织树形结构。
 * - 枚举序列化为 JSON 字符串（后端 enums/FileType 的 name）。
 * - 文件分类（category）为后端整数编码：IMAGE=0 DOCUMENT=1 VIDEO=2 AUDIO=3 ARCHIVE=4 OTHER=5
 *   （constant/FileConstants），搜索过滤时直接传编码；前端仅用字符串分类做图标/样式映射。
 * - 操作显隐由 can() 规则表推导（frontend-standard.md 5.x）。
 */

/** 文件类型字符串值 —— 对应后端 FileType 枚举 name */
export type FileTypeKey = 'FILE' | 'DIRECTORY'

/** 文件状态字符串值 —— 对应后端 FileStatus 枚举 name */
export type FileStatusKey = 'NORMAL' | 'DELETED'

/** 文件分类（UI 用字符串，与后端 Integer 编码经 FILE_CATEGORY_CODE 互转） */
export type FileCategory = 'IMAGE' | 'DOCUMENT' | 'VIDEO' | 'AUDIO' | 'ARCHIVE' | 'OTHER'

/** 分类 → 后端编码（constant/FileConstants） */
export const FILE_CATEGORY_CODE: Record<FileCategory, number> = {
  IMAGE: 0,
  DOCUMENT: 1,
  VIDEO: 2,
  AUDIO: 3,
  ARCHIVE: 4,
  OTHER: 5,
}

/** 后端编码 → 分类（未知编码归为 OTHER） */
export function fileCategoryFromCode(code: number | null | undefined): FileCategory {
  const entry = Object.entries(FILE_CATEGORY_CODE).find(([, v]) => v === code)
  return (entry?.[0] as FileCategory) ?? 'OTHER'
}

/** 预览类型 —— 对应后端 FilePreviewResponse.type 字符串 */
export type PreviewType = 'IMAGE' | 'VIDEO' | 'AUDIO' | 'PDF' | 'TEXT' | 'UNSUPPORTED'

/**
 * 文件/目录节点 —— 对应后端 FileNodeResponse（GET /api/files 列表 / search / 上传返回）。
 *
 * 字段说明：
 * - size: 字节数；目录为 0
 * - isDirectory: 后端冗余布尔，便于模板判断
 * - category: 后端整数分类编码（见 FILE_CATEGORY_CODE）
 * - type: FileType 枚举名（"FILE" / "DIRECTORY"）
 * - parentId: 父目录 ID，根目录为 0
 */
export interface FileItem {
  id: number
  parentId: number
  name: string
  size: number
  mimeType: string | null
  extension: string | null
  isDirectory: boolean
  type: FileTypeKey
  category: number
  createdAt: string
  updatedAt: string
}

/** 目录树节点 —— 对应 GET /api/files/tree 返回结构 */
export interface FileTreeItem {
  id: number
  parentId?: number
  name: string
  isDirectory: boolean
  children: FileTreeItem[]
}

/** 文件列表查询参数 —— GET /api/files（分页，按 parentId 过滤） */
export interface FileListParams {
  parentId: number
  page: number
  size: number
}

/** 创建目录请求体 —— POST /api/files/directory */
export interface CreateDirectoryRequest {
  parentId: number
  name: string
}

/** 分片上传初始化请求体 —— POST /api/files/upload/init（teamId 缺省为个人空间） */
export interface UploadInitRequest {
  fileName: string
  fileSize: number
  fileHash: string
  parentId: number
  teamId?: number
}

/**
 * 分片上传初始化响应。
 * - chunkSize: 分片大小（字节）
 * - totalChunks: 分片总数（后端按文件大小自适应，小文件为 1）
 */
export interface UploadInitResponse {
  uploadId: string
  chunkSize: number
  totalChunks: number
}

/**
 * 上传策略响应 —— GET /api/files/upload/policy（VIP 差异化）。
 * - maxSize: 单文件大小上限（字节），0 表示不限制
 * - maxConcurrent: 上传并发任务数上限，0 表示不限制
 */
export interface UploadPolicy {
  maxSize: number
  maxConcurrent: number
}

/** 上传进度响应 —— GET /api/files/upload/progress/{uploadId}（断点续传） */
export interface UploadProgressResponse {
  uploadId: string
  fileName: string
  fileSize: number
  mimeType: string | null
  /** 已上传完成的分片序号（1 起） */
  uploadedChunks: number[]
  parentId: number | null
  teamId: number | null
}

/** 秒传请求体 —— POST /api/files/upload/sec（全站 SHA256 索引 + 引用计数；teamId 缺省为个人空间） */
export interface SecUploadRequest {
  fileHash: string
  fileName: string
  fileSize: number
  parentId: number
  teamId?: number
}

/** 秒传响应 —— instant=true 表示命中全站 Hash，秒传完成（file 为新记录节点） */
export interface SecUploadResponse {
  instant: boolean
  file: FileItem | null
}

/** 合并分片请求体 —— POST /api/files/upload/merge */
export interface UploadMergeRequest {
  uploadId: string
}

/** 重命名请求体 —— PUT /api/files/{id}/rename（仅改数据库 name） */
export interface RenameRequest {
  name: string
}

/** 移动请求体 —— POST /api/files/{id}/move（仅改数据库 parentId） */
export interface MoveRequest {
  targetParentId: number
}

/** 复制请求体 —— POST /api/files/{id}/copy（同用户） */
export interface CopyRequest {
  targetParentId: number
}

/** 批量打包下载请求体 —— POST /api/files/download/batch（异步任务），字段与后端 BatchDownloadRequest 对齐 */
export interface BatchDownloadRequest {
  fileIds: number[]
}

/** 批量下载任务响应 —— taskId 用于匹配 WebSocket 进度通知，status/total/done/url 为轮询快照 */
export interface BatchDownloadResponse {
  taskId: string
  status: 'PACKING' | 'DONE' | 'FAILED'
  total: number
  done: number
  url: string | null
}

/** 搜索查询参数 —— GET /api/files/search（文件名 LIKE + 类型过滤；category 为整数编码） */
export interface FileSearchParams {
  keyword: string
  category?: number
  page: number
  size: number
}

/**
 * 预览响应 —— GET /api/files/{id}/preview。
 * - type: TEXT/IMAGE/VIDEO/AUDIO/PDF/UNSUPPORTED
 * - url/thumbnailUrl: MinIO presigned URL（文本类 url 为空，内容直接由 content 返回）
 * - content: 文本类预览内容
 */
export interface FilePreviewResponse {
  type: PreviewType
  url: string | null
  thumbnailUrl: string | null
  content: string | null
  name: string
  size: number
}

/** 回收站记录 —— GET /api/files/recycle-bin（List，非分页）；type: 1=目录 0=文件 */
export interface RecycleBinItem {
  id: number
  fileId: number
  originalName: string
  type: number
  size: number
  deletedTime: string
  expireTime: string
}

/**
 * WebSocket 统一进度消息 —— 通道 /ws/progress（匿名，业务字段见下）。
 * - type=upload:   { type, uploadId, uploaded, total }         分片上传进度（已传片数/总片数）
 * - type=download: { type, taskId, status, total, done, url }  批量打包（PACKING/DONE/FAILED）
 */
export type ProgressMessage =
  | {
      type: 'upload'
      uploadId: string
      uploaded: number
      total: number
    }
  | {
      type: 'download'
      taskId: string
      status: 'PACKING' | 'DONE' | 'FAILED'
      total: number
      done: number
      url?: string
    }

/** 传输任务状态 —— 上传队列面板展示用 */
export type TransferTaskStatus =
  | 'pending' // 等待开始（队列中）
  | 'hashing' // 计算 SHA256（秒传校验前置步骤）
  | 'uploading' // 分片上传中
  | 'merging' // 合并分片
  | 'packing' // 批量打包中（下载任务）
  | 'completed'
  | 'failed'

/** 传输任务 —— 上传/下载队列项 */
export interface TransferTask {
  id: string
  name: string
  size: number
  kind: 'upload' | 'download'
  status: TransferTaskStatus
  progress: number
  error?: string
  /** 上传任务的后端 uploadId（init 后回填，用于匹配 WS 进度消息） */
  uploadId?: string
}

/*
 * 以下为前端维护的 UI 展示映射（frontend-standard.md：UI 样式归前端）。
 */

/** 扩展名 → 文件分类（用于搜索类型过滤与图标展示） */
export const FILE_EXT_CATEGORY: Record<string, FileCategory> = {
  // 图片
  jpg: 'IMAGE', jpeg: 'IMAGE', png: 'IMAGE', gif: 'IMAGE', webp: 'IMAGE', bmp: 'IMAGE', svg: 'IMAGE',
  // 视频
  mp4: 'VIDEO', webm: 'VIDEO', avi: 'VIDEO', mov: 'VIDEO', mkv: 'VIDEO',
  // 音频
  mp3: 'AUDIO', flac: 'AUDIO', wav: 'AUDIO', m4a: 'AUDIO', ogg: 'AUDIO', aac: 'AUDIO',
  // 文档
  doc: 'DOCUMENT', docx: 'DOCUMENT', xls: 'DOCUMENT', xlsx: 'DOCUMENT',
  ppt: 'DOCUMENT', pptx: 'DOCUMENT', pdf: 'DOCUMENT', txt: 'DOCUMENT', md: 'DOCUMENT',
  // 压缩包
  zip: 'ARCHIVE', rar: 'ARCHIVE', '7z': 'ARCHIVE', tar: 'ARCHIVE', gz: 'ARCHIVE',
}

/** 可预览的扩展名 → 预览类型（Office 本期仅下载，见 file-module.md 六） */
export const FILE_PREVIEW_TYPE: Record<string, PreviewType> = {
  jpg: 'IMAGE', jpeg: 'IMAGE', png: 'IMAGE', gif: 'IMAGE', webp: 'IMAGE', bmp: 'IMAGE',
  mp4: 'VIDEO', webm: 'VIDEO',
  mp3: 'AUDIO', flac: 'AUDIO', wav: 'AUDIO', m4a: 'AUDIO',
  pdf: 'PDF',
  txt: 'TEXT', md: 'TEXT',
}

/** 从文件名解析扩展名（小写，无点号）；目录返回 null */
export function fileExt(name: string, type: FileTypeKey): string | null {
  if (type === 'DIRECTORY') return null
  const idx = name.lastIndexOf('.')
  if (idx <= 0 || idx === name.length - 1) return null
  return name.slice(idx + 1).toLowerCase()
}

/** 文件分类（目录返回 null） */
export function fileCategory(name: string, type: FileTypeKey): FileCategory | null {
  if (type === 'DIRECTORY') return null
  const ext = fileExt(name, type)
  if (!ext) return 'OTHER'
  return FILE_EXT_CATEGORY[ext] ?? 'OTHER'
}

/** 是否支持预览（Office 等仅下载，见 file-module.md 六） */
export function isPreviewable(name: string, type: FileTypeKey): boolean {
  if (type === 'DIRECTORY') return false
  const ext = fileExt(name, type)
  return !!ext && ext in FILE_PREVIEW_TYPE
}
