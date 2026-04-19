import { createRouter, createWebHistory } from 'vue-router'
import { reportVisit } from '@/services/analytics'

const readAuthStorage = (key) => sessionStorage.getItem(key) || localStorage.getItem(key)

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录 - NovaLeap', requiresAuth: false },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Login.vue'),
    meta: { title: '注册 - NovaLeap', requiresAuth: false },
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('@/views/Login.vue'),
    meta: { title: '找回密码 - NovaLeap', requiresAuth: false },
  },
  {
    path: '/terms',
    name: 'Terms',
    component: () => import('@/views/Terms.vue'),
    meta: { title: '用户协议 - NovaLeap', requiresAuth: false },
  },
  {
    path: '/privacy',
    name: 'Privacy',
    component: () => import('@/views/Privacy.vue'),
    meta: { title: '隐私政策 - NovaLeap', requiresAuth: false },
  },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/Home.vue'),
        meta: { title: '首页 - NovaLeap' },
      },
      {
        path: 'questions',
        name: 'QuestionBank',
        component: () => import('@/views/QuestionBank.vue'),
        meta: { title: '拾光题库 - NovaLeap' },
      },
      {
        path: 'notes',
        name: 'Notes',
        component: () => import('@/views/Notes.vue'),
        meta: { title: '灵感手记 - NovaLeap' },
      },
      {
        path: 'resume',
        name: 'Resume',
        component: () => import('@/views/Resume.vue'),
        meta: { title: '简历工坊 - NovaLeap' },
      },
      {
        path: 'coach',
        name: 'Coach',
        component: () => import('@/views/Coach.vue'),
        meta: { title: '知跃陪练 - NovaLeap' },
      },
      {
        path: 'wishes',
        name: 'WishWall',
        component: () => import('@/views/WishWall.vue'),
        meta: { title: '星愿墙 - NovaLeap' },
      },
      {
        path: 'game',
        name: 'Game',
        component: () => import('@/views/Game.vue'),
        meta: { title: '休闲时刻 - NovaLeap' },
      },
      {
        path: 'me',
        name: 'Me',
        component: () => import('@/views/Me.vue'),
        meta: { title: '我的 - NovaLeap' },
      },
      {
        path: 'leaderboard',
        name: 'Leaderboard',
        component: () => import('@/views/Leaderboard.vue'),
        meta: { title: '排行榜 - NovaLeap' },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '个人资料 - NovaLeap' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  const token = readAuthStorage('nova_token')
  let isGuest = false

  try {
    const user = JSON.parse(readAuthStorage('nova_user') || 'null')
    isGuest = user?.role === 'GUEST'
  } catch (_) {
    isGuest = false
  }

  if (to.meta.title) {
    document.title = to.meta.title
  }

  const publicAuthPages = new Set(['Login', 'Register', 'ForgotPassword'])

  if (to.meta.requiresAuth && !token) {
    next({ name: 'Login' })
  } else if (token && publicAuthPages.has(to.name) && !isGuest) {
    next({ name: 'Home' })
  } else {
    next()
  }
})

router.afterEach((to) => {
  reportVisit(to.fullPath || to.path)
})

export default router
