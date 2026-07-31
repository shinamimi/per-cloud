/*
 * 管理后台 - 仪表盘 API。
 * 对应后端 AdminDashboardController（/api/admin/dashboard）。
 */
import request from '@/utils/request'
import type { AdminDashboardStats } from '@/types/admin'

/** 获取仪表盘统计数据：用户数、文件数、总存储、总配额、使用率 */
export function getDashboardStats(): Promise<AdminDashboardStats> {
  return request.get('/api/admin/dashboard/stats')
}
