import { createMemoryHistory, createRouter, createWebHistory } from 'vue-router'
import UserLayout from '../layouts/UserLayout.vue'
import AdminLayout from '../layouts/AdminLayout.vue'
import AuthPage from '../modules/auth/pages/AuthPage.vue'
import FlightSearchPage from '../modules/user-flights/pages/FlightSearchPage.vue'
import FavoritesPage from '../modules/user-profile/pages/FavoritesPage.vue'
import SearchHistoryPage from '../modules/user-profile/pages/SearchHistoryPage.vue'
import CrawlJobsPage from '../modules/admin-crawl/pages/CrawlJobsPage.vue'
import DataSourceStatusPage from '../modules/admin-crawl/pages/DataSourceStatusPage.vue'
import { ensureAuthenticatedSession } from '../auth/session.js'

// 测试环境没有浏览器 history，因此使用 memory history；浏览器运行时使用正常地址栏路由。
const history = typeof window === 'undefined'
  ? createMemoryHistory()
  : createWebHistory()

const router = createRouter({
  history,
  routes: [
    {
      path: '/auth',
      name: 'auth',
      component: AuthPage,
      meta: { public: true }
    },
    {
      path: '/',
      component: UserLayout,
      meta: { requiresAuth: true },
      // 普通用户页面统一放在 UserLayout 下，共享顶部导航和用户菜单。
      children: [
        {
          path: '',
          redirect: { name: 'user-flights' }
        },
        {
          path: 'flights',
          name: 'user-flights',
          component: FlightSearchPage
        },
        {
          path: 'favorites',
          name: 'user-favorites',
          component: FavoritesPage
        },
        {
          path: 'history',
          name: 'user-history',
          component: SearchHistoryPage
        }
      ]
    },
    {
      path: '/admin',
      component: AdminLayout,
      meta: { requiresAuth: true },
      // 管理端页面统一放在 AdminLayout 下，目前主要管理爬虫任务和数据源状态。
      children: [
        {
          path: '',
          redirect: { name: 'admin-crawl-jobs' }
        },
        {
          path: 'crawl-jobs',
          name: 'admin-crawl-jobs',
          component: CrawlJobsPage
        },
        {
          path: 'data-sources',
          name: 'admin-data-sources',
          component: DataSourceStatusPage
        }
      ]
    }
  ]
})

// 全局路由守卫：进入页面前先确认 token 是否仍然有效。
router.beforeEach(async to => {
  const isAuthenticated = await ensureAuthenticatedSession()

  // 已登录用户访问登录页时，直接回到航班查询页。
  if (to.meta.public && isAuthenticated) {
    return { name: 'user-flights' }
  }

  // 未登录用户访问受保护页面时，跳转登录页并保留原目标地址。
  if (to.meta.requiresAuth && !isAuthenticated) {
    return {
      name: 'auth',
      query: to.fullPath && to.fullPath !== '/auth'
        ? { redirect: to.fullPath }
        : undefined
    }
  }

  return true
})

export default router
