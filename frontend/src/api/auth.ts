/*
 * 认证模块（M1）API 调用层 —— 封装所有公开认证接口。
 *
 * 设计思路：
 * 每个方法对应一个后端接口，返回 Promise<T> 而非 Promise<Result<T>>。
 * 这是因为 request.ts 的响应拦截器已自动解包 Result<T>，
 * 调用方（store / view）直接使用数据，无需重复处理 code/message。
 *
 * 使用示例：
 *   const res = await loginApi({ username: 'admin', password: '123456' })
 *   // res 类型为 LoginResponse，包含 { token, userId, username, role }
 */

import request from '@/utils/request'
import type { LoginRequest, LoginResponse, RegisterRequest, SendCodeRequest, ResetPasswordRequest } from '@/types/auth'

/** 登录 —— 返回 token 和用户基本信息 */
export function loginApi(data: LoginRequest): Promise<LoginResponse> {
  return request.post('/api/auth/login', data)
}

/**
 * 注册 —— 需要邮箱验证码，注册成功后自动返回 token（免登录体验）。
 *
 * 注册流程：
 * 1. 用户填写邮箱 → 调用 sendCodeApi 发送验证码
 * 2. 用户填写完整注册信息（含验证码）→ 调用此接口
 * 3. 注册成功直接返回 token，前端自动登录
 */
export function registerApi(data: RegisterRequest): Promise<LoginResponse> {
  return request.post('/api/auth/register', data)
}

/** 登出 —— 将当前 Token 加入 Redis 黑名单 */
export function logoutApi(): Promise<void> {
  return request.post('/api/auth/logout')
}

/**
 * 发送邮箱验证码。
 *
 * 60 秒冷却机制：
 * 后端 Redis 记录 cooldown:email，60 秒内重复调用返回 CAPTCHA_COOLDOWN，
 * 前端按钮禁用 60 秒，防止频繁请求。
 */
export function sendCodeApi(data: SendCodeRequest): Promise<void> {
  return request.post('/api/auth/send-code', data)
}

/**
 * 忘记密码 —— 发送重置密码验证码。
 * 与 send-code 的区别：此接口会先校验邮箱是否已注册，未注册则返回错误。
 */
export function forgotPasswordApi(data: SendCodeRequest): Promise<void> {
  return request.post('/api/auth/forgot-password', data)
}

/** 重置密码 —— 验证码校验通过后更新密码 */
export function resetPasswordApi(data: ResetPasswordRequest): Promise<void> {
  return request.post('/api/auth/reset-password', data)
}
