<template>
  <div class="app-layout">
    <div class="app-atmosphere"></div>

    <header class="app-header">
      <div class="floating-nav">
        <button type="button" class="brand-pill" @click="navigateNav(navItems[0])">
          <img src="@/assets/logo.png" alt="logo" class="h-7 w-auto object-contain" />
          <div class="hidden min-w-0 text-left sm:block">
            <p class="text-sm font-semibold tracking-[-0.02em] text-text-primary">NovaLeap</p>
            <p class="text-[11px] text-[#f89aa5]">知跃</p>
          </div>
        </button>

        <nav class="nav-center hidden xl:flex">
          <button
            v-for="item in navItems"
            :key="`desktop-${item.name}-${item.path}`"
            type="button"
            class="nav-item-pill"
            :class="isNavActive(item) ? 'nav-item-pill-active' : 'nav-item-pill-idle'"
            @click="navigateNav(item)"
          >
            <svg
              v-if="item.icon && NovaIcons[item.icon]"
              class="h-4 w-4"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              v-html="NovaIcons[item.icon]"
            ></svg>
            <span>{{ item.name }}</span>
          </button>
        </nav>

        <div class="nav-actions hidden xl:flex">
          <button
            type="button"
            class="icon-btn"
            :title="uiStore.isDarkMode ? '切换到浅色模式' : '切换到深色模式'"
            @click="uiStore.toggleDarkMode"
          >
            <svg v-if="uiStore.isDarkMode" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="5" />
              <path d="M12 1v2m0 18v2M4.22 4.22l1.42 1.42m12.72 12.72l1.42 1.42M1 12h2m18 0h2M4.22 19.78l1.42-1.42m12.72-12.72l1.42-1.42" />
            </svg>
            <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
            </svg>
          </button>

          <button type="button" class="profile-pill" @click="goMe">
            <span v-if="isEmoji(displayAvatar)" class="profile-avatar">{{ displayAvatar }}</span>
            <img v-else :src="displayAvatar" alt="avatar" class="profile-avatar profile-avatar-img" />
            <div class="profile-copy">
              <span class="profile-label">{{ authStore.isLoggedIn ? '我的' : '登录' }}</span>
              <span class="profile-name hidden lg:block" :title="displayNicknameRaw">{{ displayNickname }}</span>
            </div>
          </button>

          <button
            v-if="authStore.isLoggedIn"
            type="button"
            class="icon-btn icon-btn-danger"
            title="退出登录"
            @click="handleLogout"
          >
            <svg class="h-[18px] w-[18px]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.9" d="M17 16l4-4m0 0l-4-4m4 4H8m5 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
            </svg>
          </button>
        </div>

        <button type="button" class="icon-btn nav-mobile-trigger xl:hidden" @click="mobileMenuOpen = true">
          <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.9" d="M4 6h16M4 12h16m-7 6h7" />
          </svg>
        </button>
      </div>
    </header>

    <div v-if="mobileMenuOpen" class="mobile-mask xl:hidden" @click="mobileMenuOpen = false"></div>

    <aside class="mobile-drawer xl:hidden" :class="mobileMenuOpen ? 'translate-x-0' : 'translate-x-full'">
      <div class="flex items-center justify-between px-5 py-5">
        <div>
          <p class="text-lg font-semibold tracking-[-0.03em] text-text-primary">导航</p>
          <p class="mt-1 text-xs uppercase tracking-[0.2em] text-text-tertiary">NovaLeap</p>
        </div>
        <button type="button" class="icon-btn" @click="mobileMenuOpen = false">
          <svg class="h-[18px] w-[18px]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.9" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <nav class="flex-1 space-y-2 overflow-y-auto px-4 pb-5">
        <button
          v-for="item in navItems"
          :key="`mobile-${item.name}-${item.path}`"
          type="button"
          class="mobile-nav-item"
          :class="isNavActive(item) ? 'mobile-nav-item-active' : 'mobile-nav-item-idle'"
          @click="navigateNav(item)"
        >
          <span>{{ item.name }}</span>
          <svg class="h-4 w-4 opacity-55" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.9" d="M9 5l7 7-7 7" />
          </svg>
        </button>
      </nav>

      <div class="space-y-2 border-t border-border-subtle px-4 py-4">
        <button type="button" class="mobile-action-btn" @click="goMe">
          <span>进入我的</span>
          <span class="text-text-tertiary">{{ displayNickname }}</span>
        </button>
        <button type="button" class="mobile-action-btn" @click="uiStore.toggleDarkMode">
          <span>{{ uiStore.isDarkMode ? '浅色模式' : '深色模式' }}</span>
          <span class="text-text-tertiary">{{ uiStore.isDarkMode ? 'Light' : 'Dark' }}</span>
        </button>
        <button v-if="authStore.isLoggedIn" type="button" class="mobile-logout-btn" @click="handleMobileLogout">退出登录</button>
      </div>
    </aside>

    <div class="app-main">
      <div class="app-header-spacer"></div>
      <main class="app-content">
        <section class="workspace-host">
          <router-view v-slot="{ Component }">
            <transition
              enter-active-class="transition-opacity duration-200"
              enter-from-class="opacity-0"
              enter-to-class="opacity-100"
              leave-active-class="transition-opacity duration-150"
              leave-from-class="opacity-100"
              leave-to-class="opacity-0"
              mode="out-in"
            >
              <keep-alive :include="['Home', 'QuestionBank', 'Me']">
                <component :is="Component" />
              </keep-alive>
            </transition>
          </router-view>
        </section>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import { useAuth } from '@/composables/useAuth'
import { api } from '@/composables/useRequest'

// 极简 SVG 图标库实现：解决依赖缺失问题
const NovaIcons = {
  Home: '<path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/>',
  Trophy: '<path d="M6 9H4.5a2.5 2.5 0 0 1 0-5H6"/><path d="M18 9h1.5a2.5 2.5 0 0 0 0-5H18"/><path d="M4 22h16"/><path d="M10 14.66V17c0 .55-.47.98-.97 1.21C7.85 18.75 7 20.24 7 22"/><path d="M14 14.66V17c0 .55.47.98.97 1.21C16.15 18.75 17 20.24 17 22"/><path d="M18 2H6v7a6 6 0 0 0 12 0V2Z"/>',
  Edit3: '<path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/>',
  FileText: '<path d="M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z"/><path d="M14 2v4a2 2 0 0 0 2 2h4"/><path d="M10 9H8"/><path d="M16 13H8"/><path d="M16 17H8"/>',
  UserCheck: '<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><polyline points="16 11 18 13 22 9"/>',
  Star: '<polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>',
  Gamepad2: '<line x1="6" x2="10" y1="11" y2="11"/><line x1="8" x2="8" y1="9" y2="13"/><rect width="20" height="12" x="2" y="6" rx="2"/><path d="M15 12h.01"/><path d="M18 10h.01"/>'
}

const route = useRoute()
const router = useRouter()
const uiStore = useUiStore()
const authStore = useAuthStore()
const { logout } = useAuth()
const mobileMenuOpen = ref(false)
const activeHomeSection = ref('hero')

const navItems = [
  { name: '首页', path: '/', icon: 'Home', scrollTarget: 'hero', activeSection: 'hero' },
  { name: '拾光题库', path: '/questions', icon: 'FileText' },
  { name: '灵感手记', path: '/notes', icon: 'Edit3' },
  { name: '简历工坊', path: '/resume', icon: 'FileText' },
  { name: '知跃陪练', path: '/coach', icon: 'UserCheck' },
  { name: '星愿墙', path: '/wishes', icon: 'Star' },
  { name: '休闲时刻', path: '/game', icon: 'Gamepad2' },
  { name: '排行榜', path: '/leaderboard', icon: 'Trophy' },
]

const isHomeRoute = computed(() => route.path === '/')

const guestIdPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i
const isGuest = computed(() => {
  if (authStore.isGuest) return true
  const nickname = String(authStore.nickname || '')
  const username = String(authStore.user?.username || authStore.username || '')
  return nickname.startsWith('游客') || guestIdPattern.test(username)
})

const displayAvatar = computed(() => {
  const avatar = String(authStore.avatar || '').trim()
  if (!avatar) return '🥳'
  if (/^https?:\/\//i.test(avatar)) return avatar
  return avatar
})

const displayNicknameRaw = computed(() => {
  if (isGuest.value) return 'Nova 访客'
  return authStore.nickname || (authStore.isLoggedIn ? 'Nova 用户' : '立即登录')
})

const displayNickname = computed(() => {
  const raw = String(displayNicknameRaw.value || '').trim()
  return raw.length > 14 ? `${raw.slice(0, 12)}…` : raw
})

const isEmoji = (val) => typeof val === 'string' && !val.startsWith('http')

const emitHomeScroll = (sectionId) => {
  window.dispatchEvent(new CustomEvent('nova-home-scroll', { detail: { section: sectionId } }))
}

const emitHomeScrollWithRetry = (sectionId) => {
  const delays = [0, 80, 220, 420]
  delays.forEach((delay) => {
    window.setTimeout(() => emitHomeScroll(sectionId), delay)
  })
}

const navigateNav = async (item) => {
  mobileMenuOpen.value = false

  if (isHomeRoute.value && item.scrollTarget) {
    activeHomeSection.value = item.activeSection || item.scrollTarget
    emitHomeScrollWithRetry(item.scrollTarget)
    return
  }

  if (route.path !== item.path) {
    await router.push(item.path)
  }

  if (item.path === '/' && item.scrollTarget) {
    activeHomeSection.value = item.activeSection || item.scrollTarget
    emitHomeScrollWithRetry(item.scrollTarget)
  }
}

const isNavActive = (item) => {
  if (isHomeRoute.value && item.scrollTarget) {
    const activeKey = item.activeSection || item.scrollTarget
    return activeHomeSection.value === activeKey
  }
  if (item.path === '/') return route.path === '/'
  return route.path === item.path || route.path.startsWith(`${item.path}/`)
}

const goMe = () => {
  mobileMenuOpen.value = false
  router.push('/me')
}

const handleLogout = async () => {
  if (window.confirm('确定要退出登录吗？')) {
    await logout()
  }
}

const handleMobileLogout = async () => {
  if (window.confirm('确定要退出登录吗？')) {
    mobileMenuOpen.value = false
    await logout()
  }
}

const handleHomeActive = (event) => {
  const section = event.detail?.section
  if (section) activeHomeSection.value = section
}

watch(() => route.fullPath, () => {
  mobileMenuOpen.value = false
})

onMounted(async () => {
  window.addEventListener('nova-home-active', handleHomeActive)
  try {
    const res = await api.get('/api/auth/profile')
    if (res.code === 200 && res.data?.avatar) {
      authStore.patchUser({
        avatar: res.data.avatar,
        nickname: res.data.nickname || authStore.nickname,
      })
    }
  } catch (_) {
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('nova-home-active', handleHomeActive)
})
</script>

<style scoped>
.app-layout {
  position: relative;
  isolation: isolate;
  min-height: 100vh;
  height: 100dvh;
  width: 100%;
  overflow: hidden;
  background: var(--app-shell-bg);
}

@supports (height: 100svh) {
  .app-layout {
    min-height: 100svh;
  }
}

.app-atmosphere {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background: transparent;
}

.app-header {
  position: fixed;
  left: 50%;
  top: 14px;
  z-index: 50;
  width: min(1240px, calc(100% - 20px));
  transform: translateX(-50%);
}

.floating-nav {
  position: relative;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.62);
  border-bottom: 1px solid rgba(93, 84, 117, 0.12);
  background:
    linear-gradient(180deg, rgba(255, 251, 253, 0.82), rgba(255, 255, 255, 0.68));
  backdrop-filter: blur(22px) saturate(180%);
  -webkit-backdrop-filter: blur(22px) saturate(180%);
  transform: translateZ(0);
  box-shadow:
    0 18px 42px rgba(106, 103, 125, 0.12),
    0 8px 18px rgba(227, 225, 238, 0.42),
    inset 0 1px 0 rgba(255, 255, 255, 0.62);
}

.brand-pill {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.42);
  background: rgba(255, 250, 252, 0.46);
  padding: 8px 12px;
  white-space: nowrap;
  transition: border-color 0.2s ease, background-color 0.2s ease, box-shadow 0.2s ease;
}

.brand-pill:hover {
  border-color: rgba(248, 154, 165, 0.22);
  background: rgba(255, 252, 253, 0.62);
  box-shadow: 0 10px 24px rgba(248, 154, 165, 0.1);
}

.nav-center {
  min-width: 0;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.nav-item-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border-radius: 999px;
  border: 1px solid transparent;
  padding: 9px 14px;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  transition: color 0.2s ease, border-color 0.2s ease, background-color 0.2s ease, box-shadow 0.2s ease;
  will-change: border-color, background-color, color;
}

.nav-item-pill-idle {
  color: rgba(39, 34, 53, 0.68);
}

.nav-item-pill-idle:hover {
  background: linear-gradient(135deg, rgba(248, 154, 165, 0.12), rgba(248, 154, 165, 0.045));
  border-color: rgba(248, 154, 165, 0.18);
  color: color-mix(in srgb, var(--ai-from) 56%, var(--primary) 44%);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.48),
    0 8px 18px rgba(248, 154, 165, 0.06);
}

.nav-item-pill-active {
  background: linear-gradient(135deg, rgba(248, 154, 165, 0.16), rgba(248, 154, 165, 0.06));
  border-color: rgba(248, 154, 165, 0.28);
  color: color-mix(in srgb, var(--ai-from) 62%, var(--primary) 38%);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.58),
    0 10px 22px rgba(248, 154, 165, 0.08);
}

.nav-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  justify-self: end;
}

.icon-btn {
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.4);
  background: rgba(255, 250, 252, 0.42);
  color: rgba(39, 34, 53, 0.68);
  transition: transform 0.2s ease, color 0.2s ease, border-color 0.2s ease, background-color 0.2s ease;
}

.icon-btn:hover {
  transform: translateY(-1px);
  border-color: rgba(248, 154, 165, 0.2);
  color: color-mix(in srgb, var(--ai-from) 56%, var(--primary) 44%);
  background: linear-gradient(135deg, rgba(248, 154, 165, 0.1), rgba(248, 154, 165, 0.035));
}

.icon-btn-danger {
  color: var(--danger);
}

.profile-pill {
  min-height: 40px;
  max-width: min(320px, 26vw);
  min-width: 0;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.42);
  background: rgba(255, 250, 252, 0.42);
  padding: 0 14px 0 10px;
  color: var(--text-primary);
  transition: border-color 0.2s ease, background-color 0.2s ease, box-shadow 0.2s ease;
}

.profile-pill:hover {
  border-color: rgba(248, 154, 165, 0.22);
  background: rgba(255, 252, 253, 0.58);
  box-shadow: 0 10px 24px rgba(248, 154, 165, 0.08);
}

.profile-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  align-items: flex-start;
  line-height: 1.15;
}

.profile-label {
  font-size: 13px;
  font-weight: 600;
}

.profile-name {
  font-size: 11px;
  color: var(--text-tertiary);
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-avatar {
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: var(--accent-soft);
  overflow: hidden;
}

.profile-avatar-img {
  object-fit: cover;
}

.nav-mobile-trigger {
  display: none;
}

.mobile-mask {
  position: fixed;
  inset: 0;
  z-index: 45;
  background: rgba(15, 23, 42, 0.22);
}

.mobile-drawer {
  position: fixed;
  right: 0;
  top: 0;
  bottom: 0;
  z-index: 60;
  width: min(84vw, 320px);
  display: flex;
  flex-direction: column;
  border-left: 1px solid var(--border-soft);
  background: var(--bg-elevated);
  box-shadow: -26px 0 48px rgba(15, 23, 42, 0.2);
  transition: transform 0.26s ease;
}

.mobile-nav-item,
.mobile-action-btn {
  width: 100%;
  min-height: 48px;
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  border-radius: 14px;
  border: 1px solid var(--border-subtle);
  background: var(--bg-soft);
  padding: 0 12px;
  font-size: 13px;
}

.mobile-nav-item-idle {
  color: var(--text-tertiary);
}

.mobile-nav-item-active {
  color: var(--primary);
  background: var(--accent-soft);
  border-color: var(--accent-border);
  font-weight: 600;
}

.mobile-logout-btn {
  width: 100%;
  border-radius: 14px;
  border: 1px solid var(--danger-border);
  background: var(--danger-soft);
  color: var(--danger);
  padding: 11px 12px;
  font-size: 13px;
  font-weight: 600;
}

.app-main {
  position: relative;
  z-index: 1;
  display: flex;
  height: 100%;
  flex-direction: column;
}

.app-header-spacer {
  height: 91px;
  flex-shrink: 0;
}

.app-content {
  min-height: 0;
  flex: 1;
  padding: 0;
}

.workspace-host {
  margin: 0;
  height: 100%;
  width: 100%;
  overflow: hidden;
  border: none;
  border-radius: 0;
  background: var(--app-shell-bg);
  box-shadow: none;
}

@media (max-width: 1279px) {
  .floating-nav {
    grid-template-columns: auto 1fr auto;
  }

  .nav-actions {
    margin-left: auto;
  }

  .nav-mobile-trigger {
    display: inline-flex;
    justify-self: end;
  }

  .workspace-host {
    border-radius: 0;
  }
}

@media (max-width: 639px) {
  .app-header {
    top: 10px;
    width: calc(100% - 10px);
  }

  .floating-nav {
    grid-template-columns: auto 1fr auto;
    padding: 7px 8px;
  }

  .app-header-spacer {
    height: 84px;
  }

  .app-content {
    padding: 0;
  }

  .workspace-host {
    border-radius: 0;
  }
}

.dark .floating-nav,
.dark .brand-pill,
.dark .icon-btn,
.dark .profile-pill,
.dark .mobile-drawer,
.dark .mobile-nav-item,
.dark .mobile-action-btn {
  background: color-mix(in srgb, var(--bg-elevated) 92%, #0f172a 8%);
  border-color: rgba(255, 255, 255, 0.12);
}

.dark .floating-nav {
  border-bottom-color: rgba(255, 255, 255, 0.08);
  box-shadow:
    0 22px 48px rgba(0, 0, 0, 0.42),
    inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.dark .nav-item-pill-idle,
.dark .icon-btn {
  color: rgba(255, 255, 255, 0.68);
}

.dark .nav-item-pill-idle:hover,
.dark .nav-item-pill-active,
.dark .icon-btn:hover {
  color: color-mix(in srgb, var(--ai-from) 74%, white 26%);
}

.dark .mobile-nav-item-active {
  background: var(--accent-soft);
  border-color: var(--accent-border);
}

.dark .workspace-host {
  background: var(--app-shell-bg);
}

.dark .app-atmosphere {
  background: transparent;
}
</style>
