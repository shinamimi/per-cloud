/*
 * 文件模块 API —— 对应后端 FileController（/api/files）。
 *
 * 注意：上传相关接口携带文件二进制，需传较长超时；默认 request 超时 30s 仅适用于常规接口。
 */
import request, { downloadGet } from '@/utils/request'
import type { PageResponse } from '@/types/api'
import { saveBlob } from '@/utils/download'
import type {
  BatchDownloadRequest,
  BatchDownloadResponse,
  CopyRequest,
  CreateDirectoryRequest,
  FileItem,
  FileListParams,
  FilePreviewResponse,
  FileSearchParams,
  FileTreeItem,
  MoveRequest,
  RecycleBinItem,
  RenameRequest,
  SecUploadRequest,
  SecUploadResponse,
  UploadInitRequest,
  UploadInitResponse,
  UploadMergeRequest,
  UploadPolicy,
  UploadProgressResponse,
} from '@/types/file'

/** 文件列表（分页，按 parentId 过滤）—— GET /api/files */
export function getFileList(params: FileListParams): Promise<PageResponse<FileItem>> {
  return request.get('/api/files', { params })
}

/** 目录树 —— GET /api/files/tree */
export function getFileTree(): Promise<FileTreeItem[]> {
  return request.get('/api/files/tree')
}

/** 创建目录 —— POST /api/files/directory */
export function createDirectory(data: CreateDirectoryRequest): Promise<FileItem> {
  return request.post('/api/files/directory', data)
}

/** 初始化分片上传 —— POST /api/files/upload/init（校验配额/单文件上限/并发任务数） */
export function uploadInit(data: UploadInitRequest): Promise<UploadInitResponse> {
  return request.post('/api/files/upload/init', data)
}

/** 上传策略 —— GET /api/files/upload/policy（单文件上限 + 并发数，VIP 差异化） */
export function getUploadPolicy(): Promise<UploadPolicy> {
  return request.get('/api/files/upload/policy')
}

/**
 * 上传分片 —— POST /api/files/upload/chunk（multipart）。
 * 后端参数：uploadId + seq（分片序号，从 1 开始）+ file（二进制）。
 * 单分片上传可能耗时较长，单独放大超时（10 分钟）。
 */
export function uploadChunk(uploadId: string, seq: number, blob: Blob): Promise<void> {
  const formData = new FormData()
  formData.append('uploadId', uploadId)
  formData.append('seq', String(seq))
  formData.append('file', blob)
  return request.post('/api/files/upload/chunk', formData, { timeout: 600000 })
}

/** 合并分片 —— POST /api/files/upload/merge（Redis 分布式锁 + 配额一次性扣减） */
export function uploadMerge(data: UploadMergeRequest): Promise<FileItem> {
  return request.post('/api/files/upload/merge', data)
}

/** 秒传 —— POST /api/files/upload/sec（全站 Hash 命中则引用计数 +1） */
export function secUpload(data: SecUploadRequest): Promise<SecUploadResponse> {
  return request.post('/api/files/upload/sec', data)
}

/** 上传进度（断点续传）—— GET /api/files/upload/progress/{uploadId} */
export function uploadProgress(uploadId: string): Promise<UploadProgressResponse> {
  return request.get(`/api/files/upload/progress/${uploadId}`)
}

/**
 * 单文件下载 —— GET /api/files/{id}/download。
 * 后端 302 重定向到 MinIO presigned URL，前端经 downloadGet 拿 Blob 后保存为本地文件。
 */
export async function downloadFile(file: FileItem): Promise<void> {
  const blob = await downloadGet(`/api/files/${file.id}/download`)
  saveBlob(blob, file.name)
}

/** 批量打包下载（异步）—— POST /api/files/download/batch，进度经 WebSocket 通知 */
export function batchDownload(data: BatchDownloadRequest): Promise<BatchDownloadResponse> {
  return request.post('/api/files/download/batch', data)
}

/** 重命名 —— PUT /api/files/{id}/rename（仅改数据库 name） */
export function renameFile(id: number, data: RenameRequest): Promise<FileItem> {
  return request.put(`/api/files/${id}/rename`, data)
}

/** 移动 —— POST /api/files/{id}/move（仅改数据库 parentId） */
export function moveFile(id: number, data: MoveRequest): Promise<FileItem> {
  return request.post(`/api/files/${id}/move`, data)
}

/** 复制（同用户）—— POST /api/files/{id}/copy */
export function copyFile(id: number, data: CopyRequest): Promise<FileItem> {
  return request.post(`/api/files/${id}/copy`, data)
}

/** 移入回收站（递归）—— DELETE /api/files/{id}（逻辑删除，30 天后物理清理） */
export function deleteFile(id: number): Promise<void> {
  return request.delete(`/api/files/${id}`)
}

/** 搜索（文件名 LIKE + 类型过滤）—— GET /api/files/search（category 为整数编码） */
export function searchFiles(params: FileSearchParams): Promise<PageResponse<FileItem>> {
  return request.get('/api/files/search', { params })
}

/** 预览 —— GET /api/files/{id}/preview（presigned URL + 文本内容） */
export function previewFile(id: number): Promise<FilePreviewResponse> {
  return request.get(`/api/files/${id}/preview`)
}

/** 回收站列表（无分页）—— GET /api/files/recycle-bin */
export function getRecycleBinList(): Promise<RecycleBinItem[]> {
  return request.get('/api/files/recycle-bin')
}

/** 从回收站恢复 —— POST /api/files/recycle-bin/{id}/restore */
export function restoreRecycle(id: number): Promise<void> {
  return request.post(`/api/files/recycle-bin/${id}/restore`)
}

/** 彻底删除回收站记录 —— DELETE /api/files/recycle-bin/{id} */
export function purgeRecycle(id: number): Promise<void> {
  return request.delete(`/api/files/recycle-bin/${id}`)
}
