/*
 * 分享模块（M4）类型定义 —— 对应后端 dto/share/*。
 *
 * 设计思路：
 * - 分享分为"我的分享"（登录态管理）与"访客访问"（无登录态）两个视角。
 * - 访客下载/转存均针对分享快照节点（t_share_file），id 为快照 id。
 * - 枚举序列化为 JSON 字符串（后端枚举 name）。
 */

/** 分享有效期类型 —— PERMANENT=永久 / DAYS=按天数（validDays 生效） */
export type ShareValidType = 'PERMANENT' | 'DAYS'

/** 分享状态 —— 对应后端 ShareStatus 枚举 name */
export type ShareStatusKey = 'NORMAL' | 'EXPIRED' | 'CANCELED' | 'EXHAUSTED'

/** 分享下载策略（管理端配置 + 创建时覆盖） */
export type ShareDownloadPolicy = 'ALLOW' | 'DENY'

/** 分享项（我的分享列表 / 创建返回）—— 对应后端 ShareResponse */
export interface ShareItem {
  id: number
  fileId: number
  isDir: boolean
  name: string | null
  status: ShareStatusKey
  shareToken: string
  requirePassword: boolean
  expireTime: string | null
  allowDownload: boolean
  maxDownload: number
  downloadCount: number
  allowSave: boolean
  createdAt: string | null
}

/** 创建分享请求体 —— POST /api/shares */
export interface ShareCreateRequest {
  fileId: number
  validType: ShareValidType
  validDays?: number
  requirePassword?: boolean
  accessPassword?: string
  allowDownload?: boolean
  maxDownload?: number
  allowSave?: boolean
}

/** 修改有效期请求体 —— PUT /api/shares/{id} */
export interface ShareUpdateRequest {
  validType: ShareValidType
  validDays?: number
}

/** 访客分享信息 —— 对应后端 GuestShareInfoResponse */
export interface GuestShareInfo {
  shareToken: string
  isDir: boolean
  name: string | null
  ownerName: string
  status: ShareStatusKey
  requirePassword: boolean
  allowDownload: boolean
  allowSave: boolean
  maxDownload: number
  downloadCount: number
  fileCount: number | null
}

/** 分享快照节点（访客浏览树）—— 对应后端 ShareFileNodeResponse */
export interface ShareFileNode {
  id: number
  parentId: number
  name: string
  isDir: boolean
  size: number
  mimeType: string | null
  extension: string | null
}

/** 访客下载/转存请求体 —— 快照节点 id 列表 */
export interface ShareAccessRequest {
  snapshotIds: number[]
}
