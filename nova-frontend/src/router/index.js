import { ref } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import { reportVisit } from '@/services/analytics'
import { TOKEN_KEY, USER_KEY } from '@/config/constants'

const readAuthStorage = (key) => sessionStorage.getItem(key) || localStorage.getItem(key)

const isSlowNetwork = () => {
  if (typeof navigator === 'undefined') return false
  const connection = navigator.connection || navigator.mozConnection || navigator.webkitConnection
  if (!connection) return false
  if (connection.saveData) return true
  return ['slow-2g', '2g', '3g'].includes(String(connection.effectiveType || '').toLowerCase())
}

const scheduleIdleTask = (callback, timeout = 1500) => {
  if (typeof window === 'undefined') return
  if (typeof window.requestIdleCallback === 'function') {
    window.requestIdleCallback(callback, { timeout })
    return
  }
  window.setTimeout(callback, timeout)
}

const LoginView = () => import('@/views/Login.vue')
const TermsView = () => import('@/views/Terms.vue')
const PrivacyView = () => import('@/views/Privacy.vue')
const LayoutView = () => import('@/views/Layout.vue')
const HomeView = () => import('@/views/Home.vue')
const QuestionBankView = () => import('@/views/QuestionBank.vue')
const NotesView = () => import('@/views/Notes.vue')
const ResumeView = () => import('@/views/Resume.vue')
const CoachView = () => import('@/views/Coach.vue')
const WishWallView = () => import('@/views/WishWall.vue')
const GameView = () => import('@/views/Game.vue')
const MeView = () => import('@/views/Me.vue')
const LeaderboardView = () => import('@/views/Leaderboard.vue')
const ProfileView = () => import('@/views/Profile.vue')

export const routeLoading = ref(false)

let routeLoadingTimer = 0
let warmedAuthenticatedRoutes = false

const startRouteLoading = () => {
  if (typeof window === 'undefined') {
    routeLoading.value = true
    return
  }
  if (routeLoadingTimer) window.clearTimeout(routeLoadingTimer)
  routeLoadingTimer = window.setTimeout(() => {
    routeLoading.value = true
  }, 120)
}

const stopRouteLoading = () => {
  if (typeof window !== 'undefined' && routeLoadingTimer) {
    window.clearTimeout(routeLoadingTimer)
    routeLoadingTimer = 0
  }
  routeLoading.value = false
}

const warmRouteChunks = () => {
  if (warmedAuthenticatedRoutes || isSlowNetwork()) return
  warmedAuthenticatedRoutes = true

  scheduleIdleTask(() => {
    ;[
      HomeView,
      QuestionBankView,
      NotesView,
      ResumeView,
      CoachView,
      WishWallView,
      GameView,
      MeView,
      LeaderboardView,
      ProfileView,
    ].forEach((loadView) => {
      loadView().catch(() => {})
    })
  }, 1800)
}

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: LoginView,
    meta: { title: '登录 - NovaLeap', requiresAuth: false },
  },
  {
    path: '/register',
    name: 'Register',
    component: LoginView,
    meta: { title: '注册 - NovaLeap', requiresAuth: false },
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: LoginView,
    meta: { title: '找回密码 - NovaLeap', requiresAuth: false },
  },
  {
    path: '/terms',
    name: 'Terms',
    component: TermsView,
    meta: { title: '用户协议 - NovaLeap', requiresAuth: false },
  },
  {
    path: '/privacy',
    name: 'Privacy',
    component: PrivacyView,
    meta: { title: '隐私政策 - NovaLeap', requiresAuth: false },
  },
  {
    path: '/',
    component: LayoutView,
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'Home',
        component: HomeView,
        meta: { title: '首页 - NovaLeap' },
      },
      {
        path: 'questions',
        name: 'QuestionBank',
        component: QuestionBankView,
        meta: { title: '拾光题库 - NovaLeap' },
      },
      {
        path: 'notes',
        name: 'Notes',
        component: NotesView,
        meta: { title: '灵感手记 - NovaLeap' },
      },
      {
        path: 'resume',
        name: 'Resume',
        component: ResumeView,
        meta: { title: '简历工坊 - NovaLeap' },
      },
      {
        path: 'coach',
        name: 'Coach',
        component: CoachView,
        meta: { title: '知跃陪练 - NovaLeap' },
      },
      {
        path: 'wishes',
        name: 'WishWall',
        component: WishWallView,
        meta: { title: '星愿墙 - NovaLeap' },
      },
      {
        path: 'game',
        name: 'Game',
        component: GameView,
        meta: { title: '休闲时刻 - NovaLeap' },
      },
      {
        path: 'me',
        name: 'Me',
        component: MeView,
        meta: { title: '我的 - NovaLeap' },
      },
      {
        path: 'leaderboard',
        name: 'Leaderboard',
        component: LeaderboardView,
        meta: { title: '排行榜 - NovaLeap' },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: ProfileView,
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
  startRouteLoading()

  const token = readAuthStorage(TOKEN_KEY)
  let isGuest = false

  try {
    const user = JSON.parse(readAuthStorage(USER_KEY) || 'null')
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
  if (to.matched.some((record) => record.meta?.requiresAuth)) {
    warmRouteChunks()
  }
  reportVisit(to.fullPath || to.path)
  stopRouteLoading()
})

router.onError(() => {
  stopRouteLoading()
})

export default router
