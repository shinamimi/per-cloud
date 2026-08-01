/*
 * 管理后台（M7）类型定义 —— 对应后端 dto/admin/ 下的所有 DTO。
 *
 * 设计思路：
 * - 后端 Spring Boot 默认将 Java 枚举序列化为 JSON 字符串（name），
 *   因此 role / status 在此处声明为字符串字面量联合类型。
 * - 字符串值即字典接口（GET /api/meta/options）中的 value，
 *   label 展示统一从 metaStore 字典组获取（见 docs/frontend-standard.md）。
 */

/** 用户状态字符串值 —— 对应后端 UserStatus 枚举 name */
export type UserStatusKey = 'NORMAL' | 'DISABLED' | 'LOCKED' | 'INACTIVE'

/** 角色字符串值 —— 对应后端 Role 枚举 name */
export type RoleKey = 'USER' | 'OPERATOR' | 'ADMIN' | 'SUPER_ADMIN'

/**
 * 管理员用户管理响应 —— 对应用户列表/管理员列表返回的数据。
 *
 * 字段说明：
 * - totalQuota: base quota + admin bonus quota + reward quota 之和
 * - quota: 用户基础配额
 * - adminBonusQuota: 管理员额外分配的配额（通过此页面设置）
 * - rewardQuota: 奖励配额
 */
export interface AdminUserResponse {
  id: number
  username: string
  email: string
  nickname: string
  avatar: string
  role: RoleKey
  quota: number
  totalQuota: number
  adminBonusQuota: number
  rewardQuota: number
  usedSpace: number
  isVip: boolean
  status: UserStatusKey
  createdAt: string
}

/** 仪表盘统计数据 */
export interface AdminDashboardStats {
  userCount: number
  fileCount: number
  totalSize: number
  totalQuota: number
  usagePercent: number
}

/** 修改用户状态请求体 */
export interface StatusRequest {
  status: UserStatusKey
}

/** 修改配额请求体 —— adminBonusQuota 是管理员额外分配的额度（字节） */
export interface QuotaRequest {
  adminBonusQuota: number
}

/** 重置密码请求体 —— 密码规则：9+ 位，必须包含字母和数字 */
export interface AdminResetPasswordRequest {
  newPassword: string
}

/** 创建管理员请求体 */
export interface CreateAdminRequest {
  username: string
  password: string
  email: string
  nickname?: string
  role: RoleKey
}

/** 修改管理员角色请求体 */
export interface UpdateRoleRequest {
  role: RoleKey
}

/** 候选管理员 —— 穿梭器左侧列表项（对应 GET /api/admin/admins/candidates） */
export interface AdminCandidate {
  id: number
  username: string
  nickname: string | null
}

/** 批量角色变更项 —— 对应 PUT /api/admin/admins/batch 请求体元素 */
export interface AdminRoleChange {
  userId: number
  newRole: RoleKey
}

/**
 * 用户状态 → Tag 类型映射 —— 前端维护的 UI 展示样式。
 * 字典接口只返回 value + label，颜色等样式属于前端职责（见 frontend-standard.md）。
 */
export const USER_STATUS_TAG_TYPE: Record<UserStatusKey, string> = {
  NORMAL: 'success',
  DISABLED: 'danger',
  LOCKED: 'warning',
  INACTIVE: 'info',
}

/* ==================== 系统配置中心（SystemConfigCenterView） ==================== */

/*
 * 保存请求约定：数字/字符串字段为 null = 恢复配置文件默认值（后端删除该配置行），
 * boolean 字段为开关值，始终提交。
 */

/** 上传限制分组（key: upload.*） */
export interface UploadSettings {
  maxSizeUser: number
  maxSizeVip: number
  maxConcurrentUser: number
  maxConcurrentVip: number
}

/** 上传限制保存请求 */
export interface UploadSettingsRequest {
  maxSizeUser: number | null
  maxSizeVip: number | null
  maxConcurrentUser: number | null
  maxConcurrentVip: number | null
}

/** 存储限制分组（key: storage.*） */
export interface StorageSettings {
  defaultQuotaUser: number
  defaultQuotaVip: number
}

/** 存储限制保存请求 */
export interface StorageSettingsRequest {
  defaultQuotaUser: number | null
  defaultQuotaVip: number | null
}

/** 会话安全分组（key: session.*） */
export interface SessionSettings {
  accessTokenTtlMinutes: number
  captchaTtlSeconds: number
  loginLockThreshold: number
  loginLockDurationMinutes: number
  resetPasswordTtlMinutes: number
}

/** 会话安全保存请求 */
export interface SessionSettingsRequest {
  accessTokenTtlMinutes: number | null
  captchaTtlSeconds: number | null
  loginLockThreshold: number | null
  loginLockDurationMinutes: number | null
  resetPasswordTtlMinutes: number | null
}

/** 缓存策略分组（key: cache.*） */
export interface CacheSettings {
  captcha: number
  loginAttempt: number
  blacklist: number
  filePreview: number
  downloadLinkMinutes: number
}

/** 缓存策略保存请求 */
export interface CacheSettingsRequest {
  captcha: number | null
  loginAttempt: number | null
  blacklist: number | null
  filePreview: number | null
  downloadLinkMinutes: number | null
}

/** 系统功能开关分组（key: system.*） */
export interface SystemSettings {
  allowRegister: boolean
  allowGuestShare: boolean
  enableMailVerify: boolean
  enableCaptcha: boolean
  enableOperationLog: boolean
}

/** 系统功能保存请求（ADMIN） */
export type SystemSettingsRequest = SystemSettings

/** 文件管理分组（key: file.* / share.*） */
export interface FileSettings {
  recycleBinDays: number
  shareDefaultValidDays: number
  shareMaxValidDays: number
  shareMaxCountPerFile: number
  shareDefaultRequirePassword: boolean
}

/** 文件管理保存请求 */
export interface FileSettingsRequest {
  recycleBinDays: number | null
  shareDefaultValidDays: number | null
  shareMaxValidDays: number | null
  shareMaxCountPerFile: number | null
  shareDefaultRequirePassword: boolean
}

/** 邮件服务分组（key: mail.*，ADMIN 可改；password 为脱敏占位符或空） */
export interface MailSettings {
  enabled: boolean
  host: string
  port: number
  username: string
  password: string | null
  fromName: string
  frequencyLimit: number
}

/** 邮件服务保存请求（ADMIN；password 留空 = 不修改当前密码） */
export interface MailSettingsRequest {
  enabled: boolean
  host: string | null
  port: number | null
  username: string | null
  password: string | null
  fromName: string | null
  frequencyLimit: number | null
}

/** 日志分组（key: log.*） */
export interface LogSettings {
  operationDays: number
  loginDays: number
}

/** 日志保存请求 */
export interface LogSettingsRequest {
  operationDays: number | null
  loginDays: number | null
}

/** 日志查询结果项（GET /api/admin/settings/logs） */
export interface LogItem {
  id: number
  userId: number
  username: string | null
  operation: string
  detail: string | null
  ip: string | null
  createdAt: string
}

/** 日志分页返回 */
export interface LogPageResponse {
  records: LogItem[]
  total: number
  page: number
  size: number
}

/** GET /api/admin/settings 全量返回（按分组） */
export interface AdminSettingsResponse {
  upload: UploadSettings
  storage: StorageSettings
  session: SessionSettings
  cache: CacheSettings
  system: SystemSettings
  file: FileSettings
  mail: MailSettings
  log: LogSettings
}

/** 老用户配额批量调整请求体（POST /api/admin/settings/users/quota-batch） */
export interface QuotaBatchRequest {
  startDate: string
  endDate: string
  role: 'ALL' | 'USER' | 'VIP'
  status: 'ALL' | UserStatusKey
  targetQuotaUser: number
  targetQuotaVip: number
  preview: boolean
}

/** 老用户配额批量调整结果 */
export interface QuotaBatchResponse {
  count: number
  users: AdminUserResponse[]
}
