<template>
  <div class="space-y-6">
    <!-- JVM 虚拟资源监控 -->
    <div class="bg-admin-card rounded-lg p-6 shadow-sm border border-gray-100">
      <h2 class="text-lg font-medium mb-4 flex items-center">
        <Server class="w-5 h-5 mr-2 text-admin-muted" /> JVM 虚拟资源监控
      </h2>
      <div v-if="loading" class="animate-pulse flex space-x-4">
        <div class="flex-1 space-y-4 py-1">
          <div class="h-2 bg-slate-200 rounded"></div>
          <div class="space-y-3">
            <div class="grid grid-cols-3 gap-4">
              <div class="h-2 bg-slate-200 rounded col-span-2"></div>
              <div class="h-2 bg-slate-200 rounded col-span-1"></div>
            </div>
          </div>
        </div>
      </div>
      <div v-else-if="error" class="text-admin-danger bg-red-50 p-4 rounded-md text-sm border border-red-100 flex items-start">
        <AlertTriangle class="w-5 h-5 mr-2 shrink-0" />
        <div>
          <p class="font-medium">未能连接至后端监控探测口 /api/admin/system-monitor</p>
          <p class="mt-1 opacity-80">{{ error }}</p>
        </div>
      </div>
      <div v-else class="space-y-6">
        <div class="flex items-center justify-between text-sm">
          <span class="text-admin-muted font-medium">应用堆栈内存利用率</span>
          <span class="font-bold text-admin-text">{{ memoryPercentage }}%</span>
        </div>
        <div class="w-full bg-gray-100 rounded-full h-4 overflow-hidden shadow-inner">
          <div class="h-4 transition-all duration-500 ease-out"
               :class="memoryPercentage > 85 ? 'bg-admin-danger' : memoryPercentage > 60 ? 'bg-amber-500' : 'bg-admin-success'"
               :style="{ width: `${memoryPercentage}%` }"></div>
        </div>
        <div class="grid grid-cols-3 gap-4 border-t border-gray-100 pt-4 mt-4">
           <div class="text-center">
             <p class="text-xs text-admin-muted mb-1">当前占用刻度</p>
             <p class="text-lg font-bold">{{ monitorData.jvmUsedMemoryMB || 0 }} <span class="text-xs font-normal text-admin-muted">MB</span></p>
           </div>
           <div class="text-center border-l border-r border-gray-100">
             <p class="text-xs text-admin-muted mb-1">分配阀值大限</p>
             <p class="text-lg font-bold">{{ monitorData.jvmMaxMemoryMB || 0 }} <span class="text-xs font-normal text-admin-muted">MB</span></p>
           </div>
           <div class="text-center">
             <p class="text-xs text-admin-muted mb-1">活跃并行任务数</p>
             <p class="text-lg font-bold">{{ monitorData.currentActiveTasks || 0 }}</p>
           </div>
        </div>
      </div>
    </div>

    <!-- Redis 缓存监控 -->
    <div class="bg-admin-card rounded-lg p-6 shadow-sm border border-gray-100">
      <h2 class="text-lg font-medium mb-4 flex items-center">
        <Database class="w-5 h-5 mr-2 text-admin-muted" /> Redis 缓存监控
      </h2>
      <div v-if="loading" class="animate-pulse space-y-4">
        <div class="h-2 bg-slate-200 rounded w-1/3"></div>
        <div class="grid grid-cols-2 gap-4">
          <div class="h-2 bg-slate-200 rounded"></div>
          <div class="h-2 bg-slate-200 rounded"></div>
        </div>
      </div>
      <div v-else-if="!redisConnected" class="text-admin-danger bg-red-50 p-4 rounded-md text-sm border border-red-100 flex items-start">
        <AlertTriangle class="w-5 h-5 mr-2 shrink-0" />
        <div>
          <p class="font-medium">Redis 连接异常</p>
          <p class="mt-1 opacity-80">无法获取 Redis 实例信息，请检查 Redis 服务状态</p>
        </div>
      </div>
      <div v-else class="space-y-6">
        <!-- 连接状态与角色 -->
        <div class="flex items-center justify-between">
          <div class="flex items-center space-x-2">
            <span class="w-2 h-2 rounded-full bg-admin-success animate-pulse"></span>
            <span class="text-sm font-medium text-admin-text">运行中</span>
          </div>
          <div class="flex items-center space-x-4 text-xs text-admin-muted">
            <span>角色: <strong class="text-admin-text">{{ monitorData.role || 'N/A' }}</strong></span>
            <span>已运行 <strong class="text-admin-text">{{ monitorData.uptimeDays || 0 }}</strong> 天</span>
          </div>
        </div>

        <!-- 内存使用 -->
        <div>
          <div class="flex items-center justify-between text-sm mb-2">
            <span class="text-admin-muted font-medium">内存使用率</span>
            <span class="font-bold text-admin-text">{{ monitorData.usedMemory || 'N/A' }} / {{ monitorData.maxMemory || 'N/A' }}</span>
          </div>
          <div class="w-full bg-gray-100 rounded-full h-3 overflow-hidden shadow-inner">
            <div class="h-3 transition-all duration-500 ease-out"
                 :class="monitorData.memoryUsagePercent > 85 ? 'bg-admin-danger' : monitorData.memoryUsagePercent > 60 ? 'bg-amber-500' : 'bg-admin-success'"
                 :style="{ width: `${Math.min(monitorData.memoryUsagePercent || 0, 100)}%` }"></div>
          </div>
          <p class="text-right text-xs text-admin-muted mt-1">{{ monitorData.memoryUsagePercent || 0 }}%</p>
        </div>

        <!-- 连接与键值 -->
        <div class="grid grid-cols-2 lg:grid-cols-4 gap-4 border-t border-gray-100 pt-4">
          <div class="text-center">
            <p class="text-xs text-admin-muted mb-1">活跃连接</p>
            <p class="text-lg font-bold text-admin-text">{{ monitorData.connectedClients || 0 }}</p>
          </div>
          <div class="text-center border-l border-gray-100">
            <p class="text-xs text-admin-muted mb-1">阻塞连接</p>
            <p class="text-lg font-bold" :class="monitorData.blockedClients > 0 ? 'text-admin-danger' : 'text-admin-text'">
              {{ monitorData.blockedClients || 0 }}
            </p>
          </div>
          <div class="text-center border-l border-gray-100">
            <p class="text-xs text-admin-muted mb-1">键总数</p>
            <p class="text-lg font-bold text-admin-text">{{ formatNumber(monitorData.db0Keys || 0) }}</p>
          </div>
          <div class="text-center border-l border-gray-100">
            <p class="text-xs text-admin-muted mb-1">设置过期键</p>
            <p class="text-lg font-bold text-admin-text">{{ formatNumber(monitorData.db0Expires || 0) }}</p>
          </div>
        </div>

        <!-- 命中率与命令 -->
        <div class="grid grid-cols-2 gap-4 border-t border-gray-100 pt-4">
          <div>
            <div class="flex items-center justify-between text-sm mb-2">
              <span class="text-admin-muted font-medium">缓存命中率</span>
              <span class="font-bold" :class="(monitorData.hitRate || 0) >= 80 ? 'text-admin-success' : (monitorData.hitRate || 0) >= 50 ? 'text-amber-500' : 'text-admin-danger'">
                {{ monitorData.hitRate || 0 }}%
              </span>
            </div>
            <div class="w-full bg-gray-100 rounded-full h-2 overflow-hidden">
              <div class="h-2 transition-all duration-500 ease-out bg-admin-primary"
                   :style="{ width: `${Math.min(monitorData.hitRate || 0, 100)}%` }"></div>
            </div>
            <div class="flex justify-between text-xs text-admin-muted mt-1">
              <span>命中: {{ formatNumber(monitorData.keyspaceHits || 0) }}</span>
              <span>未命中: {{ formatNumber(monitorData.keyspaceMisses || 0) }}</span>
            </div>
          </div>
          <div class="flex flex-col justify-center">
            <div class="text-center">
              <p class="text-xs text-admin-muted mb-1">总执行命令数</p>
              <p class="text-lg font-bold text-admin-text">{{ formatNumber(monitorData.totalCommands || 0) }}</p>
            </div>
          </div>
        </div>

        <!-- 网络吞吐 -->
        <div class="grid grid-cols-2 gap-4 border-t border-gray-100 pt-4">
          <div class="text-center">
            <p class="text-xs text-admin-muted mb-1">网络入流量</p>
            <p class="text-sm font-bold text-admin-text">{{ formatBytes(monitorData.netInputBytes || 0) }}</p>
          </div>
          <div class="text-center border-l border-gray-100">
            <p class="text-xs text-admin-muted mb-1">网络出流量</p>
            <p class="text-sm font-bold text-admin-text">{{ formatBytes(monitorData.netOutputBytes || 0) }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { Server, Database, Activity, AlertTriangle } from 'lucide-vue-next'
import { api } from '@/composables/useRequest'

const loading = ref(true)
const error = ref(null)
const monitorData = ref({})
let timer = null

const memoryPercentage = computed(() => {
  if (!monitorData.value.jvmMaxMemoryMB) return 0
  return Math.round((monitorData.value.jvmUsedMemoryMB / monitorData.value.jvmMaxMemoryMB) * 100)
})

const redisConnected = computed(() => monitorData.value.connected === true)

const formatNumber = (num) => {
  if (num >= 10000) return (num / 10000).toFixed(1) + 'w'
  return num.toLocaleString()
}

const formatBytes = (bytes) => {
  if (bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(1024))
  return (bytes / Math.pow(1024, i)).toFixed(2) + ' ' + units[i]
}

const fetchMonitorData = async () => {
  try {
    const json = await api.get('/api/admin/system-monitor')
    if (json && json.code === 200) {
      monitorData.value = json.data
      error.value = null
    } else {
      throw new Error(json?.message || '抓取底层指标出错')
    }
  } catch (err) {
    error.value = err.message || '网络或授权异常'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchMonitorData()
  timer = setInterval(fetchMonitorData, 5000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>
