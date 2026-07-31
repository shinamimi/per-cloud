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

/*
 * 为什么管理后台路由不要求 meta.layout？
 * 管理后台页面使用 MainLayout（和其他业务页面相同），
 * 通过侧边栏的 el-menu 子菜单导航。
 * 路由默认 meta.layout 为 'main'（在 App.vue 的 layout 计算中 fallback）。
 */

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

  /*
   * 管理后台页面组 —— 使用 MainLayout，需要 ADMIN 及以上角色
   */
  {
    path: '/admin',
    name: 'AdminDashboard',
    component: () => import('@/views/admin/AdminDashboardView.vue'),
    meta: { layout: 'main', title: '仪表盘', requiresAuth: true, requiresAdmin: true },
  },
  {
    path: '/admin/users',
    name: 'AdminUsers',
    component: () => import('@/views/admin/AdminUserView.vue'),
    meta: { layout: 'main', title: '用户管理', requiresAuth: true, requiresAdmin: true },
  },
  {
    path: '/admin/admins',
    name: 'AdminAdmins',
    component: () => import('@/views/admin/AdminAdminView.vue'),
    meta: { layout: 'main', title: '管理员管理', requiresAuth: true, requiresAdmin: true },
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
/*
 * 导航守卫 —— 两层校验：
 * 1. 需登录的页面 → 未登录则重定向到 /login
 * 2. 需管理员的页面（requiresAdmin）→ 非 ADMIN 及以上角色则重定向到 /files
 *
 * 为什么不做更细粒度的角色校验？
 * requiresAdmin 只检查 role >= ADMIN，后端 SecurityConfig 会进一步拦截权限不足的请求。
 * 前端仅做粗粒度的路由保护，避免非管理员看到管理入口。
 */
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }

  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    next({ name: 'Files' })
    return
  }

  next()
})

export default router
