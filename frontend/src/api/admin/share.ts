/*
 * 管理后台 - 分享管控 API（仅 ADMIN+）。
 * 对应后端 AdminShareController（/api/admin/shares）：列表/取消/下载开关/删除记录。
 */
import request from '@/utils/request'
import type { AdminShareItem } from '@/types/admin'

/** 管理端分享列表 —— GET /api/admin/shares */
export function getAdminShares(): Promise<AdminShareItem[]> {
  return request.get('/api/admin/shares')
}

/** 取消分享 —— POST /api/admin/shares/{id}/cancel */
export function adminCancelShare(id: number): Promise<void> {
  return request.post(`/api/admin/shares/${id}/cancel`)
}

/** 切换下载开关 —— PUT /api/admin/shares/{id}/download */
export function adminSetShareDownload(id: number, allowDownload: boolean): Promise<void> {
  return request.put(`/api/admin/shares/${id}/download`, { allowDownload })
}

/** 删除分享记录 —— DELETE /api/admin/shares/{id}/record */
export function adminDeleteShare(id: number): Promise<void> {
  return request.delete(`/api/admin/shares/${id}/record`)
}
