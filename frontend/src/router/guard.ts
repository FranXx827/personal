/**
 * 路由守卫
 * - 未登录拦截 → /login
 * - 已登录访问 /login → 跳转到对应角色首页
 * - 角色不匹配 → 跳转到自己的首页
 * - 动态 title
 */
import type { Router } from 'vue-router'
import NProgress from 'nprogress'
import { useUserStore } from '@/stores/user'

NProgress.configure({ showSpinner: false })

const APP_TITLE = import.meta.env.VITE_APP_TITLE || '智能电商平台'

export function setupRouterGuard(router: Router) {
  router.beforeEach(async (to, _from, next) => {
    NProgress.start()

    const userStore = useUserStore()
    const requiresAuth = to.meta.requiresAuth === true
    const allowedRoles = (to.meta.roles as string[] | undefined) ?? []
    const loggedIn = userStore.isLoggedIn
    const role = userStore.role

    // 已登录却访问登录页 → 跳到自己角色的首页
    if (to.name === 'Login' && loggedIn) {
      next(role === 'MERCHANT' ? '/merchant/dashboard' : '/')
      return
    }

    // 需要登录但未登录 → /login（带回跳地址）
    if (requiresAuth && !loggedIn) {
      next({ name: 'Login', query: { redirect: to.fullPath } })
      return
    }

    // 已登录但角色不匹配 → 跳到自己角色的首页
    if (loggedIn && allowedRoles.length > 0 && !allowedRoles.includes(role)) {
      next(role === 'MERCHANT' ? '/merchant/dashboard' : '/')
      return
    }

    next()
  })

  router.afterEach((to) => {
    const title = to.meta.title as string | undefined
    document.title = title ? `${title} · ${APP_TITLE}` : APP_TITLE
    NProgress.done()
  })

  router.onError(() => {
    NProgress.done()
  })
}
