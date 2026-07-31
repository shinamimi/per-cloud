/*
 * 字典 API —— 封装 GET /api/meta/options。
 *
 * 对应后端 MetaController + MetaService。
 * 返回所有业务枚举组，前端统一通过 metaStore.getGroup(name) 访问。
 */
import request from '@/utils/request'
import type { MetaGroups } from '@/types/meta'

/**
 * 拉取全部字典选项。
 * 注意：后端 role 组已做显示层混淆（OPERATOR→管理员，ADMIN→超级管理员），
 * 前端零二次映射，直接展示 label。
 */
export function getMetaOptions(): Promise<MetaGroups> {
  return request.get('/api/meta/options')
}
