/*
 * 认证模块（M1）类型定义 —— 对应后端 com.cloud.backend.dto 下的请求/响应 DTO。
 *
 * 接口清单（全部公开，无需 Token）：
 *   POST /api/auth/login           → LoginRequest  → LoginResponse
 *   POST /api/auth/register        → RegisterRequest → LoginResponse
 *   POST /api/auth/logout          → (空) → Void
 *   POST /api/auth/send-code       → SendCodeRequest → Void
 *   POST /api/auth/forgot-password → SendCodeRequest → Void
 *   POST /api/auth/reset-password  → ResetPasswordRequest → Void
 */

/** 登录请求体 */
export interface LoginRequest {
  username: string
  password: string
}

/**
 * 登录响应体 —— 后端 LoginResponse 包含 token + 用户基本信息。
 *
 * 实现原理：
 * 前端拿到 token 后存入 localStorage，后续所有请求通过 Axios 拦截器自动注入 Authorization 头。
 * userId / username / role 存入 Pinia userStore，用于路由鉴权和 UI 展示。
 */
export interface LoginResponse {
  token: string
  userId: number
  username: string
  role: number
}

/** 注册请求体 —— 密码规则：8-20 位，必须包含字母和数字 */
export interface RegisterRequest {
  username: string
  password: string
  email: string
  nickname?: string
  code: string
}

/** 发送验证码请求体 */
export interface SendCodeRequest {
  email: string
  captchaType: CaptchaType
}

/** 重置密码请求体 */
export interface ResetPasswordRequest {
  email: string
  code: string
  newPassword: string
}

/**
 * 验证码用途类型 —— 对应后端 com.cloud.backend.enums.CaptchaType。
 *
 * 为什么需要区分类型？
 * 防止用注册验证码来重置密码（串用攻击），
 * 后端存储时 key 为 captcha:{type}:{email}，两类验证码互不影响。
 */
export enum CaptchaType {
  REGISTER = 'REGISTER',
  RESET_PASSWORD = 'RESET_PASSWORD',
}
