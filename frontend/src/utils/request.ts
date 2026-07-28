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

import axios, { AxiosError, type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'
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

    return Promise.reject(new Error(body.message || '请求失败'))
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
