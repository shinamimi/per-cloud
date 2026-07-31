/*
 * 管理后台 - 管理员账号管理 API。
 * 对应后端 AdminAccountController（/api/admin/admins）。
 *
 * 权限说明：所有接口仅 SUPER_ADMIN 可调用（后端 SecurityConfig 限制）。
 */
import request from '@/utils/request'
import type {
  AdminUserResponse,
  CreateAdminRequest,
  UpdateRoleRequest,
  AdminCandidate,
  AdminRoleChange,
} from '@/types/admin'

/** 获取所有管理员账户列表（role >= ADMIN） */
export function getAdminAccounts(): Promise<AdminUserResponse[]> {
  return request.get('/api/admin/admins')
}

/** 创建管理员账户（单个创建） */
export function createAdminAccount(data: CreateAdminRequest): Promise<AdminUserResponse> {
  return request.post('/api/admin/admins', data)
}

/** 删除管理员账户 */
export function deleteAdminAccount(id: number): Promise<void> {
  return request.delete(`/api/admin/admins/${id}`)
}

/** 修改单个管理员角色 */
export function updateAdminRole(id: number, data: UpdateRoleRequest): Promise<void> {
  return request.put(`/api/admin/admins/${id}/role`, data)
}

/**
 * 获取候选管理员列表 —— 供穿梭器左侧使用。
 * 返回可被设为管理员的普通用户（排除已是 ADMIN / SUPER_ADMIN 的用户）。
 */
export function getAdminCandidates(): Promise<AdminCandidate[]> {
  return request.get('/api/admin/admins/candidates')
}

/**
 * 批量变更角色 —— 穿梭器确认时调用。
 * 请求体为变更项数组，降级也传目标角色（USER）：
 *   [{ userId: 1, newRole: 'ADMIN' }, { userId: 2, newRole: 'USER' }]
 */
export function updateAdminRolesBatch(changes: AdminRoleChange[]): Promise<void> {
  return request.put('/api/admin/admins/batch', changes)
}
