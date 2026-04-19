<template>
  <aside
    class="sidebar-shell hidden h-full shrink-0 flex-col border-r border-border-subtle md:flex"
    :class="sidebarCollapsed ? 'w-[92px]' : 'w-[244px]'"
  >
    <div class="px-4 pt-5">
      <div class="brand-card" :class="{ 'justify-center px-0': sidebarCollapsed }">
        <div class="brand-logo">
          <img src="@/assets/logo.png" alt="logo" class="h-12 w-auto object-contain" />
        </div>
        <div v-if="!sidebarCollapsed" class="min-w-0">
          <p class="text-[24px] font-semibold tracking-[-0.04em] text-text-primary">NovaLeap</p>
          <p class="mt-1 text-xs font-medium tracking-[0.22em] text-text-muted">LEARNING PRODUCT</p>
        </div>
      </div>
    </div>

    <nav class="custom-scrollbar flex-1 space-y-1.5 overflow-y-auto px-3 py-6">
      <RouterLink
        v-for="item in navItems"
        :key="item.path"
        :to="item.path"
        class="nav-item group"
        :class="isActive(item.path) ? 'nav-item-active' : 'nav-item-idle'"
      >
        <span v-if="isActive(item.path)" class="nav-active-bar"></span>
        <span class="nav-icon" :class="isActive(item.path) ? 'nav-icon-active' : 'nav-icon-idle'">
          <AppIcon :name="item.icon" :size="20" />
        </span>
        <span v-if="!sidebarCollapsed" class="truncate text-sm font-medium">{{ item.name }}</span>
      </RouterLink>
    </nav>

    <div class="space-y-3 px-4 pb-4">
      <button type="button" class="action-card" :class="{ 'justify-center px-0': sidebarCollapsed }" @click="$emit('toggle-dark-mode')">
        <div class="action-icon" :class="isDarkMode ? 'bg-amber-100 text-amber-500' : 'bg-indigo-100 text-indigo-500'">
          <svg v-if="isDarkMode" class="h-4.5 w-4.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.9" d="M12 3v1m0 16v1m9-9h-1M4 12H3m14.364-6.364l-.707.707M7.05 16.95l-.707.707m11.314 0l-.707-.707M7.05 7.05l-.707-.707M12 8a4 4 0 100 8 4 4 0 000-8z" />
          </svg>
          <svg v-else class="h-4.5 w-4.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.9" d="M20.354 15.354A9 9 0 018.646 3.646 9 9 0 1012 21a9 9 0 008.354-5.646z" />
          </svg>
        </div>
        <div v-if="!sidebarCollapsed" class="min-w-0 text-left">
          <p class="text-sm font-medium text-text-primary">{{ isDarkMode ? '深色模式' : '浅色模式' }}</p>
          <p class="text-xs text-text-muted">切换界面氛围</p>
        </div>
      </button>

      <div class="user-card" :class="{ 'items-center justify-center px-0': sidebarCollapsed }">
        <button type="button" class="avatar-button" title="进入个人资料" @click="$emit('go-profile')">
          <span v-if="isEmoji(displayAvatar)" class="text-[20px] leading-none">{{ displayAvatar }}</span>
          <img v-else :src="displayAvatar" alt="avatar" class="h-full w-full object-cover" />
        </button>
        <div v-if="!sidebarCollapsed" class="min-w-0 flex-1">
          <p class="truncate text-sm font-medium text-text-primary">{{ nickname }}</p>
          <p class="mt-0.5 truncate text-xs text-text-muted">{{ isGuest ? '游客账号' : '学习中' }}</p>
        </div>
      </div>

      <div class="flex gap-2">
        <button type="button" class="action-card flex-1" :class="{ 'flex-none w-full justify-center px-0': sidebarCollapsed }" @click="$emit('toggle-sidebar')">
          <div class="action-icon bg-slate-100 text-slate-500">
            <svg class="h-4.5 w-4.5 transition-transform duration-200" :class="{ 'rotate-180': sidebarCollapsed }" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.9" d="M15 19l-7-7 7-7" />
            </svg>
          </div>
          <div v-if="!sidebarCollapsed" class="text-left">
            <p class="text-sm font-medium text-text-primary">收起侧栏</p>
            <p class="text-xs text-text-muted">调整阅读宽度</p>
          </div>
        </button>

        <button
          v-if="!sidebarCollapsed"
          type="button"
          class="logout-button"
          title="退出登录"
          @click="$emit('logout')"
        >
          <svg class="h-4.5 w-4.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.9" d="M17 16l4-4m0 0l-4-4m4 4H8m5 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
          </svg>
        </button>
      </div>
    </div>
  </aside>
</template>

<script setup>
import AppIcon from '@/components/common/AppIcon.vue'

defineProps({
  navItems: {
    type: Array,
    default: () => [],
  },
  isActive: {
    type: Function,
    required: true,
  },
  sidebarCollapsed: {
    type: Boolean,
    default: false,
  },
  isDarkMode: {
    type: Boolean,
    default: false,
  },
  displayAvatar: {
    type: String,
    default: '',
  },
  nickname: {
    type: String,
    default: '',
  },
  isGuest: {
    type: Boolean,
    default: false,
  },
  isEmoji: {
    type: Function,
    required: true,
  },
})

defineEmits(['toggle-dark-mode', 'go-profile', 'logout', 'toggle-sidebar'])
</script>

<style scoped>
.sidebar-shell {
  position: relative;
  z-index: 10;
  background: var(--bg-surface);
  backdrop-filter: blur(20px);
  box-shadow: 24px 0 40px rgba(31, 41, 55, 0.03);
  transition: width 0.28s ease;
}

.brand-card,
.action-card,
.user-card {
  display: flex;
  align-items: center;
  gap: 12px;
  border-radius: 22px;
  border: 1px solid var(--border-soft);
  background: var(--bg-elevated);
  padding: 12px;
}

.brand-logo {
  width: 48px;
  height: 48px;
  display: grid;
  place-items: center;
  border-radius: 18px;
  background: var(--bg-card);
}

.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 50px;
  border-radius: 18px;
  padding: 12px 14px;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.nav-item-idle {
  color: var(--text-secondary);
}

.nav-item-idle:hover {
  background: rgba(109, 140, 255, 0.05);
  color: var(--text-primary);
}

.nav-item-active {
  background: rgba(109, 140, 255, 0.08);
  color: var(--text-primary);
}

.nav-active-bar {
  position: absolute;
  left: 0;
  top: 50%;
  width: 3px;
  height: 28px;
  transform: translateY(-50%);
  border-radius: 999px;
  background: var(--primary);
}

.nav-icon {
  width: 22px;
  height: 22px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.nav-icon-idle {
  color: #64748b;
}

.nav-icon-active {
  color: var(--primary);
}

.action-icon {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  flex-shrink: 0;
}

.avatar-button {
  width: 42px;
  height: 42px;
  overflow: hidden;
  display: grid;
  place-items: center;
  border-radius: 16px;
  border: 1px solid var(--border-soft);
  background: var(--bg-card);
}

.logout-button {
  width: 48px;
  height: 52px;
  display: grid;
  place-items: center;
  border-radius: 18px;
  border: 1px solid rgba(252, 165, 165, 0.5);
  background: rgba(254, 242, 242, 0.86);
  color: #ef4444;
}

.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background: transparent;
}

.custom-scrollbar:hover::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.5);
}

.dark .sidebar-shell,
.dark .brand-card,
.dark .action-card,
.dark .user-card {
  background: rgba(15, 23, 42, 0.88);
  border-color: rgba(51, 65, 85, 0.82);
}

.dark .brand-logo,
.dark .avatar-button {
  background: rgba(17, 24, 39, 0.9);
  border-color: rgba(51, 65, 85, 0.82);
}

.dark .logout-button {
  background: rgba(69, 10, 10, 0.35);
  border-color: rgba(127, 29, 29, 0.65);
}
</style>
