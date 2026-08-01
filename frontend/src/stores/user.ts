/*
 * 用户状态管理（Pinia Store）—— 管理登录态、用户信息、角色。
 *
 * 设计思路：
 * - Token 和用户基本信息持久化在 localStorage，刷新页面时从 localStorage 恢复
 *   （Pinia 状态在页面刷新后丢失，但 localStorage 持久存在）。
 * - getter isLoggedIn 判断是否已登录（用于路由守卫）。
 * - getter roleLevel 将角色枚举映射为数字等级，便于做 >= ADMIN 这样的比较。
 * - login 成功后自动持久化到 localStorage；logout 时清理。
 *
 * Route guard 协作模式：
 * router.beforeEach -> userStore.isLoggedIn -> 未登录则重定向 /login
 *
 * 为什么用 localStorage 而非 cookie？
 * - JWT 无状态服务端不存 session，前端用 localStorage 最轻量。
 * - 不受 HttpOnly 限制，JS 可读写，方便 Axios 拦截器注入 Authorization 头。
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { LoginResponse } from '@/types/auth'
import type { UserProfile } from '@/types/user'
import { Role } from '@/types/user'
import { loginApi, logoutApi, registerApi } from '@/api/auth'
import type { LoginRequest, RegisterRequest } from '@/types/auth'

export const useUserStore = defineStore('user', () => {
  /* ========== State ========== */

  /** JWT Token —— 用于所有需认证的接口 */
  const token = ref<string | null>(null)

  /** 用户 ID */
  const userId = ref<number | null>(null)

  /** 用户名 */
  const username = ref<string>('')

  /** 用户角色等级 */
  const role = ref<Role>(Role.USER)

  /** 用户详细信息（个人中心展示） */
  const userInfo = ref<UserProfile | null>(null)

  /* ========== Getters ========== */

  /** 是否已登录 —— 通过 token 是否存在判断 */
  const isLoggedIn = computed(() => !!token.value)

  /** 角色数值等级（方便比较） */
  const roleLevel = computed(() => role.value)

  /** 是否有运营（OPERATOR）及以上权限 —— 可进入后台管理 */
  const isOperator = computed(() => role.value >= Role.OPERATOR)

  /** 是否有管理员及以上权限 —— 可进入管理员管理页面 */
  const isAdmin = computed(() => role.value >= Role.ADMIN)

  /** 是否超级管理员 —— 唯一可管理 ADMIN（超级管理员）角色的权限档 */
  const isSuperAdmin = computed(() => role.value >= Role.SUPER_ADMIN)

  /* ========== Actions ========== */

  /**
   * 登录 —— 调用登录 API 后持久化到 localStorage。
   *
   * 流程：
   * 1. 调用 loginApi 获取 token + 用户信息
   * 2. 更新 Pinia state
   * 3. 持久化 token 和用户信息到 localStorage（刷新恢复用）
   */
  async function login(loginRequest: LoginRequest): Promise<void> {
    const res: LoginResponse = await loginApi(loginRequest)
    setLoginState(res)
  }

  /**
   * 注册 —— 注册成功后自动登录（返回 token）。
   *
   * 实现原理：
   * 后端 register 接口在注册成功后直接生成 token 返回 LoginResponse，
   * 前端复用 login 的逻辑即可自动登录，用户体验更流畅。
   */
  async function register(registerRequest: RegisterRequest): Promise<void> {
    const res: LoginResponse = await registerApi(registerRequest)
    setLoginState(res)
    ElMessage.success('注册成功')
  }

  /**
   * 登出 —— 清理登录态，调用后端登出接口将 Token 加入黑名单。
   *
   * 登出流程：
   * 1. 调用 logoutApi 将当前 Token 加入 Redis 黑名单
   * 2. 清理 Pinia state
   * 3. 清理 localStorage
   * 4. 路由守卫检测到 isLoggedIn=false 自动重定向到 /login
   */
  async function logout(): Promise<void> {
    try {
      await logoutApi()
    } catch {
      // 即使后端登出失败也要清理本地状态
    } finally {
      clearLoginState()
    }
  }

  /**
   * 从 localStorage 恢复登录态 —— 在 App.vue onMounted 时调用。
   *
   * 为什么刷新页面后需要恢复？
   * Pinia 是内存状态，刷新后全部丢失。但 token 和用户信息已持久化到 localStorage，
   * 恢复后 Axios 拦截器又能正常注入 Authorization 头，用户无需重新登录。
   */
  function loadFromStorage(): void {
    const savedToken = localStorage.getItem('token')
    if (!savedToken) return

    token.value = savedToken

    try {
      const savedUser = localStorage.getItem('userInfo')
      if (savedUser) {
        const parsed = JSON.parse(savedUser)
        userId.value = parsed.userId ?? null
        username.value = parsed.username ?? ''
        role.value = parsed.role ?? Role.USER
      }
    } catch {
      clearLoginState()
    }
  }

  /**
   * 设置用户详细信息 —— 登录后调用 /api/users/me 获取完整用户信息。
   * 当前仅在需要时调用，非登录必调用（减少初始化时的请求数）。
   */
  function setUserInfo(info: UserProfile): void {
    userInfo.value = info
  }

  /* ========== Private Helpers ========== */

  function setLoginState(res: LoginResponse): void {
    token.value = res.token
    userId.value = res.userId
    username.value = res.username
    role.value = res.role as Role

    localStorage.setItem('token', res.token)
    localStorage.setItem(
      'userInfo',
      JSON.stringify({
        userId: res.userId,
        username: res.username,
        role: res.role,
      }),
    )
  }

  function clearLoginState(): void {
    token.value = null
    userId.value = null
    username.value = ''
    role.value = Role.USER
    userInfo.value = null

    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  return {
    token,
    userId,
    username,
    role,
    userInfo,
    isLoggedIn,
    roleLevel,
    isOperator,
    isAdmin,
    isSuperAdmin,
    login,
    register,
    logout,
    loadFromStorage,
    setUserInfo,
  }
})
