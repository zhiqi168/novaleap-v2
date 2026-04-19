import { createRouter, createWebHistory } from 'vue-router'
import { getAdminToken } from '@/composables/adminAuthStorage'
import Layout from '@/views/Layout.vue'
import Dashboard from '@/views/Dashboard.vue'
import SystemMonitor from '@/views/SystemMonitor.vue'
import UserManage from '@/views/UserManage.vue'
import QuestionManage from '@/views/QuestionManage.vue'
import NoteManage from '@/views/NoteManage.vue'
import WishManage from '@/views/WishManage.vue'
import VisitorRecords from '@/views/VisitorRecords.vue'
import Login from '@/views/Login.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      component: Login,
      name: 'Login',
      meta: { requiresAuth: false },
    },
    {
      path: '/',
      component: Layout,
      redirect: '/dashboard',
      meta: { requiresAuth: true },
      children: [
        { path: 'dashboard', component: Dashboard, name: '总览大盘' },
        { path: 'users', component: UserManage, name: '用户管理' },
        { path: 'questions', component: QuestionManage, name: '题库管理' },
        { path: 'notes', component: NoteManage, name: '手记管理' },
        { path: 'wishes', component: WishManage, name: '星愿审核' },
        { path: 'visitor-records', component: VisitorRecords, name: '浏览访客记录' },
        { path: 'monitor', component: SystemMonitor, name: '系统监控' },
      ],
    },
  ],
})

router.beforeEach((to, from, next) => {
  const token = getAdminToken()

  if (to.meta.requiresAuth !== false && !token && to.name !== 'Login') {
    next({ name: 'Login' })
  } else if (to.name === 'Login' && token) {
    next({ name: '总览大盘' })
  } else {
    next()
  }
})

export default router
