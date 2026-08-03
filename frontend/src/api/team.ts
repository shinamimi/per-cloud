/*
 * 团队模块 API —— 对应后端 TeamController（/api/teams）与 TeamFileController（/api/teams/{id}/files）。
 *
 * 接口划分：
 * - 团队 CRUD 与成员管理走 /api/teams
 * - 团队文件走 /api/teams/{id}/files（上传/秒传复用 /api/files/upload/* 带 teamId）
 * - 团队回收站独立于个人回收站，文件删除进团队回收站（30 天）
 */
import request, { downloadGet } from '@/utils/request'
import { saveBlob } from '@/utils/download'
import type { PageResponse } from '@/types/api'
import type {
  Team,
  TeamCreateRequest,
  TeamInviteRequest,
  TeamMember,
  TeamUpdateRequest,
} from '@/types/team'
import type {
  CreateDirectoryRequest,
  FileItem,
  FilePreviewResponse,
  FileTreeItem,
  MoveRequest,
  RecycleBinItem,
  RenameRequest,
} from '@/types/file'

/* ==================== 团队 CRUD / 成员 ==================== */

/** 创建团队 —— POST /api/teams */
export function createTeam(data: TeamCreateRequest): Promise<Team> {
  return request.post('/api/teams', data)
}

/** 我的团队列表 —— GET /api/teams */
export function getMyTeams(): Promise<Team[]> {
  return request.get('/api/teams')
}

/** 团队详情 —— GET /api/teams/{id} */
export function getTeamDetail(id: number): Promise<Team> {
  return request.get(`/api/teams/${id}`)
}

/** 更新团队 —— PUT /api/teams/{id} */
export function updateTeam(id: number, data: TeamUpdateRequest): Promise<Team> {
  return request.put(`/api/teams/${id}`, data)
}

/** 解散团队（仅 OWNER）—— DELETE /api/teams/{id} */
export function dissolveTeam(id: number): Promise<void> {
  return request.delete(`/api/teams/${id}`)
}

/** 邀请成员 —— POST /api/teams/{id}/members */
export function inviteMembers(id: number, data: TeamInviteRequest): Promise<void> {
  return request.post(`/api/teams/${id}/members`, data)
}

/** 成员列表 —— GET /api/teams/{id}/members */
export function getTeamMembers(id: number): Promise<TeamMember[]> {
  return request.get(`/api/teams/${id}/members`)
}

/** 移除成员 —— DELETE /api/teams/{id}/members/{userId} */
export function removeTeamMember(id: number, userId: number): Promise<void> {
  return request.delete(`/api/teams/${id}/members/${userId}`)
}

/** 退出团队 —— POST /api/teams/{id}/leave */
export function leaveTeam(id: number): Promise<void> {
  return request.post(`/api/teams/${id}/leave`)
}

/* ==================== 团队文件 ==================== */

/** 团队文件列表 —— GET /api/teams/{id}/files */
export function getTeamFileList(
  teamId: number,
  parentId: number,
  page: number,
  size: number,
): Promise<PageResponse<FileItem>> {
  return request.get(`/api/teams/${teamId}/files`, { params: { parentId, page, size } })
}

/** 团队目录树 —— GET /api/teams/{id}/files/tree */
export function getTeamFileTree(teamId: number): Promise<FileTreeItem[]> {
  return request.get(`/api/teams/${teamId}/files/tree`)
}

/** 创建团队目录 —— POST /api/teams/{id}/files/directory */
export function createTeamDirectory(teamId: number, data: CreateDirectoryRequest): Promise<FileItem> {
  return request.post(`/api/teams/${teamId}/files/directory`, data)
}

/** 重命名团队文件 —— PUT /api/teams/{id}/files/{fileId}/rename */
export function renameTeamFile(teamId: number, fileId: number, data: RenameRequest): Promise<FileItem> {
  return request.put(`/api/teams/${teamId}/files/${fileId}/rename`, data)
}

/** 移动团队文件 —— POST /api/teams/{id}/files/{fileId}/move */
export function moveTeamFile(teamId: number, fileId: number, data: MoveRequest): Promise<FileItem> {
  return request.post(`/api/teams/${teamId}/files/${fileId}/move`, data)
}

/** 复制团队文件 —— POST /api/teams/{id}/files/{fileId}/copy */
export function copyTeamFile(teamId: number, fileId: number, data: MoveRequest): Promise<FileItem> {
  return request.post(`/api/teams/${teamId}/files/${fileId}/copy`, data)
}

/** 删除团队文件（进团队回收站）—— DELETE /api/teams/{id}/files/{fileId} */
export function deleteTeamFile(teamId: number, fileId: number): Promise<void> {
  return request.delete(`/api/teams/${teamId}/files/${fileId}`)
}

/** 团队文件下载 —— GET /api/teams/{id}/files/{fileId}/download（302 → presigned URL） */
export async function downloadTeamFile(teamId: number, file: FileItem): Promise<void> {
  const blob = await downloadGet(`/api/teams/${teamId}/files/${file.id}/download`)
  saveBlob(blob, file.name)
}

/** 团队文件预览 —— GET /api/teams/{id}/files/{fileId}/preview */
export function previewTeamFile(teamId: number, fileId: number): Promise<FilePreviewResponse> {
  return request.get(`/api/teams/${teamId}/files/${fileId}/preview`)
}

/* ==================== 团队回收站 ==================== */

/** 团队回收站列表 —— GET /api/teams/{id}/files/recycle-bin */
export function getTeamRecycleBin(teamId: number): Promise<RecycleBinItem[]> {
  return request.get(`/api/teams/${teamId}/files/recycle-bin`)
}

/** 恢复团队回收站记录 —— POST /api/teams/{id}/files/recycle-bin/{recycleId}/restore */
export function restoreTeamRecycle(teamId: number, recycleId: number): Promise<void> {
  return request.post(`/api/teams/${teamId}/files/recycle-bin/${recycleId}/restore`)
}

/** 彻底删除团队回收站记录 —— DELETE /api/teams/{id}/files/recycle-bin/{recycleId} */
export function purgeTeamRecycle(teamId: number, recycleId: number): Promise<void> {
  return request.delete(`/api/teams/${teamId}/files/recycle-bin/${recycleId}`)
}
