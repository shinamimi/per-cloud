/*
 * Axios 实例封装 —— 统一管理请求/响应拦截器。
 *
 * 设计思路：
 * 1. 响应拦截器自动解包 Result<T>，调用方直接拿到 data 类型，无需重复判断 code。
 * 2. 请求拦截器自动注入 Authorization 头，调用方无需手动携带 Token。
 * 3. 401 响应自动清除登录态并跳转登录页（Token 过期/无效）。
 * 4. 网络异常/业务异常统一通过 ElMessage 提示，调用方可专注数据处理。
 *
 * 为什么不把 ElMessage 放在业务代码里？
 * 集中处理避免每个 API 调用处重复 try-catch + message 弹窗，减少样板代码。
 */

import axios, {
  AxiosError,
  type AxiosInstance,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios'
import { ElMessage } from 'element-plus'
import type { Result } from '@/types/api'

/** 需要忽略自动错误提示的 code 列表 —— 调用方想自己处理错误时使用 */
const IGNORE_ERROR_CODES: number[] = []

const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

/*
 * 请求拦截器 —— 注入 Authorization: Bearer <token>
 *
 * 实现原理：
 * 每次请求前从 localStorage 读取 Token，有则追加到请求头。
 * 后端 JwtAuthenticationFilter 从请求头解析 Token 并设置 SecurityContext。
 */
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    /*
     * FormData 请求（分片上传）必须由浏览器自动生成 multipart boundary，
     * 因此要移除全局默认的 Content-Type: application/json。
     * 否则 axios 的 transformRequest（defaults/index.js）会把 FormData
     * 用 formDataToJSON 序列化成 JSON，后端永远收不到真实的多分片文件，上传必然失败。
     */
    if (config.data instanceof FormData && config.headers) {
      config.headers.delete('Content-Type')
    }
    return config
  },
  (error) => Promise.reject(error),
)

/*
 * 响应拦截器 —— 解包 Result<T> + 统一错误处理
 *
 * 处理逻辑：
 * 1. HTTP 200 + body.code === 200 → 返回 body.data（调用方直接拿到 T）
 * 2. HTTP 200 + body.code !== 200 → 弹错误提示，reject Error(body.message)
 * 3. HTTP 401（Token 无效/过期）→ 清除本地 Token，重定向到 /login
 * 4. HTTP 其他错误 / 网络异常 → 弹通用错误提示
 */
request.interceptors.response.use(
  (response) => {
    const body = response.data as Result<any>

    if (body.code === 200) {
      return body.data
    }

    if (!IGNORE_ERROR_CODES.includes(body.code)) {
      ElMessage.error(body.message || '请求失败')
    }

    // 挂载业务错误码，供调用方区分处理（如上传并发超限 10208 → 排队等待）
    const err = new Error(body.message || '请求失败') as Error & { code?: number }
    err.code = body.code
    return Promise.reject(err)
  },
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      ElMessage.error('登录已过期，请重新登录')
      window.location.href = '/#/login'
      return Promise.reject(error)
    }

    ElMessage.error(error.message || '网络异常，请稍后重试')
    return Promise.reject(error)
  },
)

export default request

/*
 * 文件下载专用实例 —— 用于下载接口（GET /api/files/{id}/download）。
 *
 * 为什么不能用默认 request？
 * 下载接口返回的是文件二进制（Blob），不是 Result<T> JSON 包体，
 * 默认实例的响应拦截器按 Result 解包会误判（Blob 没有 code 字段）。
 * requestBlob 只做鉴权注入与 HTTP 层错误处理，直接返回 Blob。
 *
 * 注意：下载端点会 302 重定向到 MinIO presigned URL，axios 默认跟随重定向；
 * 跨域重定向时 axios 不会转发 Authorization 头（MinIO 凭 presigned URL 鉴权，无需该头）。
 */
export const requestBlob: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 0, // 大文件下载不设超时
  responseType: 'blob',
  headers: {
    'Content-Type': 'application/json',
  },
})

requestBlob.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

requestBlob.interceptors.response.use(
  (response) => {
    // 返回 Blob（类型层面回填 AxiosResponse 以满足拦截器签名，实际运行时返回 data）
    return response.data as unknown as AxiosResponse
  },
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      ElMessage.error('登录已过期，请重新登录')
      window.location.href = '/#/login'
      return Promise.reject(error)
    }

    ElMessage.error(error.message || '下载失败，请稍后重试')
    return Promise.reject(error)
  },
)

/**
 * 文件下载 GET —— 返回 Blob。
 * 类型边界：requestBlob 拦截器已解包 AxiosResponse.data（Blob），
 * 此处收敛类型断言，业务代码无需再处理 AxiosResponse 包装。
 */
export function downloadGet(url: string): Promise<Blob> {
  return requestBlob.get(url) as unknown as Promise<Blob>
}
