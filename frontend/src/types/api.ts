/*
 * 通用 API 类型 —— 与后端 Result<T> / PageResponse<T> 一一对应。
 *
 * 设计思路：
 * 后端所有接口统一返回 Result<T> 格式，code 为业务状态码（200=成功）。
 * 前端通过 Axios 响应拦截器自动解包：code=200 时返回 data，否则 reject。
 * 分页接口返回 PageResponse<T>，包含 records/total/page/size。
 */

/** 后端统一响应体 —— 对应 com.cloud.backend.dto.Result */
export interface Result<T> {
  code: number
  message: string
  data: T
}

/** 后端分页响应体 —— 对应 DDD 文档中的 PageResponse<T> */
export interface PageResponse<T> {
  records: T[]
  total: number
  page: number
  size: number
}
