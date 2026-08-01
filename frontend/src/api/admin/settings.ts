/*
 * 管理后台 - 系统配置中心 API。
 * 对应后端 AdminSettingsController（/api/admin/settings）。
 * 权限：/system 与 /mail 仅 ADMIN+ 可保存（OPERATOR 只读展示，后端 SecurityConfig 双重拦截）。
 */
import request from '@/utils/request'
import type {
  AdminSettingsResponse,
  QuotaBatchRequest,
  QuotaBatchResponse,
  UploadSettingsRequest,
  StorageSettingsRequest,
  SessionSettingsRequest,
  CacheSettingsRequest,
  SystemSettingsRequest,
  FileSettingsRequest,
  MailSettingsRequest,
  LogSettingsRequest,
} from '@/types/admin'

/** 获取全部分组配置（SMTP 密码已脱敏） */
export function getAdminSettings(): Promise<AdminSettingsResponse> {
  return request.get('/api/admin/settings')
}

/** 保存上传限制（null 字段恢复配置文件默认值） */
export function updateUploadSettings(data: UploadSettingsRequest): Promise<void> {
  return request.put('/api/admin/settings/upload', data)
}

/** 保存存储限制（新用户默认配额） */
export function updateStorageSettings(data: StorageSettingsRequest): Promise<void> {
  return request.put('/api/admin/settings/storage', data)
}

/** 保存会话安全配置 */
export function updateSessionSettings(data: SessionSettingsRequest): Promise<void> {
  return request.put('/api/admin/settings/session', data)
}

/** 保存缓存策略 */
export function updateCacheSettings(data: CacheSettingsRequest): Promise<void> {
  return request.put('/api/admin/settings/cache', data)
}

/** 保存系统功能开关（ADMIN） */
export function updateSystemSettings(data: SystemSettingsRequest): Promise<void> {
  return request.put('/api/admin/settings/system', data)
}

/** 保存文件管理配置 */
export function updateFileSettings(data: FileSettingsRequest): Promise<void> {
  return request.put('/api/admin/settings/file', data)
}

/** 保存邮件服务配置（ADMIN，密码留空 = 不修改） */
export function updateMailSettings(data: MailSettingsRequest): Promise<void> {
  return request.put('/api/admin/settings/mail', data)
}

/** 保存日志配置 */
export function updateLogSettings(data: LogSettingsRequest): Promise<void> {
  return request.put('/api/admin/settings/log', data)
}

/** 老用户配额批量调整（preview=true 仅预览明细，false 执行） */
export function quotaBatchUsers(data: QuotaBatchRequest): Promise<QuotaBatchResponse> {
  return request.post('/api/admin/settings/users/quota-batch', data)
}
