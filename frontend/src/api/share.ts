/*
 * 分享模块 API —— 对应后端 ShareController（/api/shares）与 GuestShareController（/api/shares/access）。
 *
 * 注意：
 * - /api/shares/access/** 为访客接口（SecurityConfig permitAll），无需登录；转存（save）依赖登录态
 *   从 Authorization 头取当前用户。
 * - 访客下载为 302 重定向到 MinIO presigned URL，经 downloadGet 拿 Blob 保存。
 */
import request, { downloadGet } from '@/utils/request'
import { saveBlob } from '@/utils/download'
import type { BatchDownloadResponse } from '@/types/file'
import type {
  GuestShareInfo,
  ShareAccessRequest,
  ShareCreateRequest,
  ShareFileNode,
  ShareItem,
  ShareUpdateRequest,
} from '@/types/share'

/* ==================== 我的分享（登录态） ==================== */

/** 创建分享 —— POST /api/shares */
export function createShare(data: ShareCreateRequest): Promise<ShareItem> {
  return request.post('/api/shares', data)
}

/** 我的分享列表 —— GET /api/shares */
export function listShares(): Promise<ShareItem[]> {
  return request.get('/api/shares')
}

/** 修改有效期 —— PUT /api/shares/{id} */
export function updateShareExpire(id: number, data: ShareUpdateRequest): Promise<void> {
  return request.put(`/api/shares/${id}`, data)
}

/** 取消分享 —— DELETE /api/shares/{id} */
export function cancelShare(id: number): Promise<void> {
  return request.delete(`/api/shares/${id}`)
}

/* ==================== 访客访问（无登录态） ==================== */

/** 分享信息（含是否需提取码/下载策略）—— GET /api/shares/access/{token} */
export function getGuestShareInfo(token: string): Promise<GuestShareInfo> {
  return request.get(`/api/shares/access/${token}`)
}

/** 验证提取码 —— POST /api/shares/access/{token}/verify（错误限次 5，Redis 计数） */
export function verifySharePassword(token: string, password: string): Promise<void> {
  return request.post(`/api/shares/access/${token}/verify`, { password })
}

/** 分享文件树（平铺快照节点，parentId 指向快照父节点）—— GET /api/shares/access/{token}/files */
export function getShareFiles(token: string): Promise<ShareFileNode[]> {
  return request.get(`/api/shares/access/${token}/files`)
}

/** 访客预览 —— GET /api/shares/access/{token}/file/{snapshotId}/preview */
export function previewShareFile(token: string, snapshotId: number): Promise<{
  type: string
  url: string | null
  thumbnailUrl: string | null
  content: string | null
  name: string
  size: number
}> {
  return request.get(`/api/shares/access/${token}/file/${snapshotId}/preview`)
}

/** 访客单文件下载（302 → presigned URL）—— GET /api/shares/access/{token}/file/{snapshotId}/download */
export async function downloadShareFile(token: string, node: ShareFileNode): Promise<void> {
  const blob = await downloadGet(`/api/shares/access/${token}/file/${node.id}/download`)
  saveBlob(blob, node.name)
}

/** 访客批量打包下载（异步任务）—— POST /api/shares/access/{token}/batch-download */
export function batchDownloadShare(token: string, data: ShareAccessRequest): Promise<BatchDownloadResponse> {
  return request.post(`/api/shares/access/${token}/batch-download`, data)
}

/** 访客批量任务进度查询 —— GET /api/shares/access/{token}/batch-task/{taskId} */
export function queryShareBatchTask(token: string, taskId: string): Promise<BatchDownloadResponse> {
  return request.get(`/api/shares/access/${token}/batch-task/${taskId}`)
}

/** 转存到当前登录用户空间（需登录）—— POST /api/shares/access/{token}/save */
export function saveShareFiles(token: string, data: ShareAccessRequest): Promise<void> {
  return request.post(`/api/shares/access/${token}/save`, data)
}
