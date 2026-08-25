import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

// 后端菜单 component 字符串 -> 前端组件 的映射
const componentMap = {
  'dashboard/index': () => import('@/views/dashboard/index.vue'),
  'device/ledger': () => import('@/views/device/ledger.vue'),
  'monitor/index': () => import('@/views/monitor/index.vue'),
  'alarm/index': () => import('@/views/alarm/index.vue'),
  'ai/analyze': () => import('@/views/ai/analyze.vue'),
  'ai/log': () => import('@/views/ai/log.vue'),
  'report/index': () => import('@/views/report/index.vue'),
  'system/user': () => import('@/views/system/user.vue'),
  'system/role': () => import('@/views/system/role.vue'),
  'system/device': () => import('@/views/system/device.vue')
}

const constantRoutes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Login.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/bigscreen',
    name: 'BigScreen',
    component: () => import('@/views/bigscreen/index.vue'),
    meta: { title: '数据大屏' }
  },
  {
    path: '/redirect/:path(.*)',
    component: () => import('@/views/404.vue'),
    meta: { public: true }
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/404.vue'),
    meta: { public: true, title: '404' }
  }
]

// 登录后动态注册
let addedRoutes = []

function registerDynamicRoutes(menus) {
  addedRoutes.forEach((r) => {
    if (r.name && router.hasRoute(r.name)) router.removeRoute(r.name)
  })
  addedRoutes = []

  const leaves = []

  // 递归收集所有"有页面组件"的叶子：
  // - 顶层页面（无 children 但有 component，如 /dashboard）
  // - 目录下子页面（如 /system/user）
  const walk = (menuList, parentPath) => {
    menuList.forEach((m) => {
      const view = componentMap[m.component]
      const kids = m.children || []
      if (view) {
        leaves.push({
          path: m.path,
          name: m.path ? m.path.replace(/^\//, '') : undefined,
          component: view,
          meta: {
            title: m.name,
            icon: m.icon,
            menuId: m.id,
            parent: parentPath || ''
          }
        })
      }
      if (kids.length) walk(kids, m.path)
    })
  }
  walk(menus, '')

  // 一块 MainLayout 下挂所有叶子，便于统一布局
  addedRoutes.push({
    path: '/',
    name: 'MainLayout',
    component: () => import('@/layout/MainLayout.vue'),
    children: leaves,
    meta: { hidden: true }
  })

  // 兜底：未命中动态路由
  addedRoutes.push({
    path: '/:pathMatch(.*)*',
    redirect: '/404'
  })

  addedRoutes.forEach((r) => router.addRoute(r))
}

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes
})

const whiteList = ['/login', '/404']

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()

  if (whiteList.includes(to.path)) {
    if (to.path === '/login' && userStore.isLoggedIn) {
      return next('/dashboard')
    }
    return next()
  }

  if (!userStore.isLoggedIn) {
    return next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
  }

  // 已登录但尚未注册动态路由
  if (addedRoutes.length === 0) {
    if (!userStore.menus.length) {
      await userStore.fetchMe()
    }
    registerDynamicRoutes(userStore.menus)
    return next({ ...to, replace: true })
  }

  next()
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · 水务监测` : '水务监测分析系统'
})

export default router
export { componentMap }