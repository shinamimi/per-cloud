/*
 * 路由配置 —— 管理所有前端的页面路由和导航守卫。
 *
 * 设计思路：
 * - 采用路由元信息 meta.layout 区分布局：auth 页面使用 AuthLayout（居中卡片），
 *   业务页面使用 MainLayout（侧边栏 + 顶栏 + 内容区）。
 * - 导航守卫 beforeEach 实现登录校验：
 *   - 未登录访问受保护页面 → 重定向到 /login（携带 redirect 参数，登录后跳回原页面）
 *   - 已登录访问登录/注册/找回密码页 → 重定向到 /files
 *
 * Hash 模式 vs History 模式：
 * 使用 Hash 模式（/#/login），因为 History 模式需要 Nginx 配置 fallback，
 * 而我们的部署方案中生产环境使用 Nginx 统一入口，Hash 模式更简单可靠。
 */

import { createRouter, createWebHashHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  /*
   * 认证页面组 —— 使用 AuthLayout（居中卡片式布局）
   */
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { layout: 'auth', title: '登录' },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/register/RegisterView.vue'),
    meta: { layout: 'auth', title: '注册' },
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('@/views/forgot/ForgotPasswordView.vue'),
    meta: { layout: 'auth', title: '找回密码' },
  },

  /*
   * 业务页面组 —— 使用 MainLayout（侧边栏 + 顶栏 + 内容区）
   */
  {
    path: '/files',
    name: 'Files',
    component: () => import('@/views/welcome/WelcomeView.vue'),
    meta: { layout: 'main', title: '我的文件', requiresAuth: true },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/welcome/WelcomeView.vue'),
    meta: { layout: 'main', title: '404', requiresAuth: true },
  },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

/*
 * 导航守卫 —— 登录校验 + 布局选择。
 *
 * 实现原理：
 * Vue Router 的 beforeEach 在每次路由切换前执行，
 * 先检查是否已登录（通过 userStore.isLoggedIn），未登录则拦截跳转到 /login。
 * 同时将目标路由的 query 参数拼接为 ?redirect=xxx，登录后自动跳回。
 */
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router
