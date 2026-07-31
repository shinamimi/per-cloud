/*
 * 管理后台 - 用户管理 API。
 * 对应后端 AdminUserController（/api/admin/users）。
 */
import request from '@/utils/request'
import type {
  AdminUserResponse,
  StatusRequest,
  QuotaRequest,
  AdminResetPasswordRequest,
} from '@/types/admin'

/** 获取所有非管理员用户列表（排除 ADMIN 和 SUPER_ADMIN） */
export function getAdminUsers(): Promise<AdminUserResponse[]> {
  return request.get('/api/admin/users')
}

/** 修改用户状态（启用/禁用/锁定等） */
export function updateUserStatus(id: number, data: StatusRequest): Promise<void> {
  return request.put(`/api/admin/users/${id}/status`, data)
}

/** 修改用户额外配额（adminBonusQuota） */
export function updateUserQuota(id: number, data: QuotaRequest): Promise<void> {
  return request.put(`/api/admin/users/${id}/quota`, data)
}

/** 解锁被锁定的用户（LOCKED → NORMAL） */
export function unlockUser(id: number): Promise<void> {
  return request.put(`/api/admin/users/${id}/unlock`)
}

/** 重置用户密码 */
export function resetUserPassword(id: number, data: AdminResetPasswordRequest): Promise<void> {
  return request.put(`/api/admin/users/${id}/reset-password`, data)
}
