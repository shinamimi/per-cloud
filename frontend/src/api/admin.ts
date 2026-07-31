/*
 * 管理后台（M7）API 调用层 —— 封装所有管理端接口。
 *
 * 设计思路：
 * 所有方法返回 Promise<T>（已被 request.ts 拦截器解包 Result<T>），
 * 调用方直接使用数据，无需处理 code/message。
 */

import request from '@/utils/request'
import type {
  AdminUserResponse,
  AdminDashboardStats,
  StatusRequest,
  QuotaRequest,
  AdminResetPasswordRequest,
  CreateAdminRequest,
  UpdateRoleRequest,
} from '@/types/admin'

/* ==================== 仪表盘 ==================== */

/** 获取仪表盘统计数据：用户数、文件数、总存储、总配额、使用率 */
export function getDashboardStats(): Promise<AdminDashboardStats> {
  return request.get('/api/admin/dashboard/stats')
}

/* ==================== 用户管理 ==================== */

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

/** 解锁被锁定的用户（USER LOCKED → NORMAL） */
export function unlockUser(id: number): Promise<void> {
  return request.put(`/api/admin/users/${id}/unlock`)
}

/** 重置用户密码 */
export function resetUserPassword(id: number, data: AdminResetPasswordRequest): Promise<void> {
  return request.put(`/api/admin/users/${id}/reset-password`, data)
}

/* ==================== 管理员账户管理（SUPER_ADMIN only） ==================== */

/** 获取所有管理员账户列表（role >= ADMIN） */
export function getAdminAccounts(): Promise<AdminUserResponse[]> {
  return request.get('/api/admin/admins')
}

/** 创建管理员账户 */
export function createAdminAccount(data: CreateAdminRequest): Promise<AdminUserResponse> {
  return request.post('/api/admin/admins', data)
}

/** 删除管理员账户 */
export function deleteAdminAccount(id: number): Promise<void> {
  return request.delete(`/api/admin/admins/${id}`)
}

/** 修改管理员角色 */
export function updateAdminRole(id: number, data: UpdateRoleRequest): Promise<void> {
  return request.put(`/api/admin/admins/${id}/role`, data)
}
