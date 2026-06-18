import { createMemoryHistory, createRouter, createWebHistory } from 'vue-router'
import UserLayout from '../layouts/UserLayout.vue'
import AdminLayout from '../layouts/AdminLayout.vue'
import AuthPage from '../modules/auth/pages/AuthPage.vue'
import FlightSearchPage from '../modules/user-flights/pages/FlightSearchPage.vue'
import FavoritesPage from '../modules/user-profile/pages/FavoritesPage.vue'
import SearchHistoryPage from '../modules/user-profile/pages/SearchHistoryPage.vue'
import CrawlJobsPage from '../modules/admin-crawl/pages/CrawlJobsPage.vue'
import DataSourceStatusPage from '../modules/admin-crawl/pages/DataSourceStatusPage.vue'

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

export default router
