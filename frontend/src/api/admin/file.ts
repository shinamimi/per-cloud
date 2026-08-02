/*
 * 管理后台 - 全局文件管控 API（docs/adr/012-admin-file-control.md，仅 ADMIN+）。
 * 覆盖个人+团队文件：列表筛选/详情/禁用启用/删除（进全局回收站）/全局回收站管理。
 */
import request from '@/utils/request'
import type { PageResponse } from '@/types/api'
import type { FilePreviewResponse } from '@/types/file'
import type {
  AdminFileItem,
  AdminFileQueryParams,
  AdminFileStatusRequest,
  AdminRecycleItem,
} from '@/types/admin'

/** 全局文件列表（筛选+分页）—— GET /api/admin/files */
export function getAdminFiles(params: AdminFileQueryParams): Promise<PageResponse<AdminFileItem>> {
  return request.get('/api/admin/files', { params })
}

/** 文件详情 —— GET /api/admin/files/{id} */
export function getAdminFileDetail(id: number): Promise<AdminFileItem> {
  return request.get(`/api/admin/files/${id}`)
}

/** 管理员预览 —— GET /api/admin/files/{id}/preview */
export function adminPreviewFile(id: number): Promise<FilePreviewResponse> {
  return request.get(`/api/admin/files/${id}/preview`)
}

/** 管理员下载 —— GET /api/admin/files/{id}/download（302 到预签名 URL，直接用 window.open 即可） */
export function adminDownloadUrl(id: number): string {
  return `/api/admin/files/${id}/download`
}

/** 禁用/启用 —— PUT /api/admin/files/{id}/status */
export function updateAdminFileStatus(id: number, status: 'NORMAL' | 'DISABLED'): Promise<void> {
  return request.put(`/api/admin/files/${id}/status`, { status })
}

/** 批量禁用/启用 —— POST /api/admin/files/batch-status */
export function batchUpdateAdminFileStatus(data: AdminFileStatusRequest): Promise<void> {
  return request.post('/api/admin/files/batch-status', data)
}

/** 删除（进全局回收站）—— DELETE /api/admin/files（body=ids 批量） */
export function deleteAdminFiles(ids: number[]): Promise<void> {
  return request.delete('/api/admin/files', { data: ids })
}

/** 全局回收站列表 —— GET /api/admin/files/recycle-bin */
export function getAdminRecycleBin(): Promise<AdminRecycleItem[]> {
  return request.get('/api/admin/files/recycle-bin')
}

/** 恢复 —— PUT /api/admin/files/recycle-bin/{id}/restore */
export function restoreAdminRecycle(id: number): Promise<void> {
  return request.put(`/api/admin/files/recycle-bin/${id}/restore`)
}

/** 彻底删除（批量）—— DELETE /api/admin/files/recycle-bin（body=ids） */
export function purgeAdminRecycle(ids: number[]): Promise<void> {
  return request.delete('/api/admin/files/recycle-bin', { data: ids })
}
