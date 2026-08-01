/*
 * 管理后台 - 团队管理 API。
 * 对应后端 AdminTeamController（/api/admin/teams）。
 */
import request from '@/utils/request'
import type { PageResponse } from '@/types/api'
import type { QuotaRequest } from '@/types/admin'
import type { FileItem, RecycleBinItem } from '@/types/file'
import type { Team, TeamMember } from '@/types/team'

/** 团队列表（含真实成员数）—— GET /api/admin/teams */
export function getAdminTeams(): Promise<Team[]> {
  return request.get('/api/admin/teams')
}

/** 团队详情（基本信息 + 成员）—— GET /api/admin/teams/{id} */
export function getAdminTeamDetail(id: number): Promise<{ team: Team; members: TeamMember[] }> {
  return request.get(`/api/admin/teams/${id}`)
}

/** 调整团队配额 —— PUT /api/admin/teams/{id}/quota（不能小于已用空间） */
export function updateAdminTeamQuota(id: number, data: QuotaRequest): Promise<void> {
  return request.put(`/api/admin/teams/${id}/quota`, data)
}

/** 团队文件列表 —— GET /api/admin/teams/{id}/files */
export function getAdminTeamFiles(
  id: number,
  parentId: number,
  page: number,
  size: number,
): Promise<PageResponse<FileItem>> {
  return request.get(`/api/admin/teams/${id}/files`, { params: { parentId, page, size } })
}

/** 团队回收站 —— GET /api/admin/teams/{id}/recycle-bin */
export function getAdminTeamRecycleBin(id: number): Promise<RecycleBinItem[]> {
  return request.get(`/api/admin/teams/${id}/recycle-bin`)
}

/** 物理清除团队回收站记录 —— DELETE /api/admin/teams/{id}/recycle-bin/{recycleId} */
export function purgeAdminTeamRecycle(id: number, recycleId: number): Promise<void> {
  return request.delete(`/api/admin/teams/${id}/recycle-bin/${recycleId}`)
}

/** 强制解散团队 —— DELETE /api/admin/teams/{id} */
export function dissolveAdminTeam(id: number): Promise<void> {
  return request.delete(`/api/admin/teams/${id}`)
}
