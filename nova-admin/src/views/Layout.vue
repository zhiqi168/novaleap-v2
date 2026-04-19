<template>
  <div class="flex h-screen bg-admin-bg">
    <aside class="w-64 bg-admin-sidebar text-white/80 flex flex-col">
      <div class="h-16 flex items-center justify-center border-b border-white/10">
        <span class="text-xl font-bold tracking-wider text-white">Nova Admin</span>
      </div>
      <nav class="flex-1 py-4 px-2 space-y-1">
        <router-link to="/dashboard" class="nav-item">
          <LayoutDashboard class="w-5 h-5 mr-3" /> 总览大盘
        </router-link>
        <router-link to="/users" class="nav-item">
          <Users class="w-5 h-5 mr-3" /> 用户管理
        </router-link>
        <router-link to="/questions" class="nav-item">
          <BookOpen class="w-5 h-5 mr-3" /> 题库管理
        </router-link>
        <router-link to="/notes" class="nav-item">
          <NotebookPen class="w-5 h-5 mr-3" /> 手记管理
        </router-link>
        <router-link to="/wishes" class="nav-item">
          <Star class="w-5 h-5 mr-3" /> 星愿审核
        </router-link>
        <router-link to="/visitor-records" class="nav-item">
          <Globe2 class="w-5 h-5 mr-3" /> 浏览访客记录
        </router-link>
        <router-link to="/monitor" class="nav-item">
          <Monitor class="w-5 h-5 mr-3" /> 系统监控
        </router-link>
      </nav>
      <div class="p-4 border-t border-white/10">
        <button
          @click="handleLogout"
          class="w-full flex items-center px-4 py-2 text-sm text-gray-300 hover:text-white hover:bg-white/10 rounded-md transition-colors"
        >
          <LogOut class="w-4 h-4 mr-2" /> 退出登�?
        </button>
      </div>
    </aside>

    <main class="flex-1 flex flex-col overflow-hidden">
      <header class="h-16 bg-admin-card shadow-sm flex items-center justify-between px-6 shrink-0">
        <h1 class="text-lg font-medium text-admin-text">{{ currentRouteName }}</h1>
        <div class="flex items-center space-x-4">
          <div class="w-8 h-8 rounded-full bg-admin-primary/10 flex items-center justify-center text-admin-primary font-bold">
            Ad
          </div>
        </div>
      </header>
      <div class="flex-1 overflow-auto p-6">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { clearAdminToken } from '@/composables/adminAuthStorage'
import { useRoute, useRouter } from 'vue-router'
import {
  LayoutDashboard,
  Monitor,
  LogOut,
  Users,
  BookOpen,
  NotebookPen,
  Star,
  Globe2,
} from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const currentRouteName = computed(() => route.name || 'Nova Admin')

const handleLogout = () => {
  if (confirm('确定要退出管理端吗？')) {
    clearAdminToken()
    router.push('/login')
  }
}
</script>

<style scoped>
.nav-item {
  @apply flex items-center px-4 py-3 text-sm rounded-md transition-colors duration-200 mt-1;
}

.router-link-active {
  @apply bg-admin-primary/10 text-admin-primary font-medium;
}

.nav-item:not(.router-link-active):hover {
  @apply bg-white/5 text-white;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

