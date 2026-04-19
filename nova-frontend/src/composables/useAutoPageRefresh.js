import { onActivated, onMounted, onUnmounted } from 'vue'

export function useAutoPageRefresh(refreshFn, options = {}) {
  const {
    throttleMs = 5000,
    intervalMs = 0,
    refreshOnFocus = true,
    refreshOnVisible = true,
    refreshOnActivated = true,
  } = options

  let timer = null
  let inFlight = false
  let lastRefreshAt = 0

  const triggerRefresh = async (force = false) => {
    const now = Date.now()
    if (!force && throttleMs > 0 && now - lastRefreshAt < throttleMs) {
      return
    }
    if (inFlight) {
      return
    }

    inFlight = true
    try {
      await refreshFn()
      lastRefreshAt = Date.now()
    } finally {
      inFlight = false
    }
  }

  const handleWindowFocus = () => {
    void triggerRefresh()
  }

  const handleVisibilityChange = () => {
    if (document.visibilityState === 'visible') {
      void triggerRefresh()
    }
  }

  onMounted(() => {
    lastRefreshAt = Date.now()
    if (refreshOnFocus) {
      window.addEventListener('focus', handleWindowFocus)
    }
    if (refreshOnVisible) {
      document.addEventListener('visibilitychange', handleVisibilityChange)
    }
    if (intervalMs > 0) {
      timer = window.setInterval(() => {
        void triggerRefresh()
      }, intervalMs)
    }
  })

  if (refreshOnActivated) {
    onActivated(() => {
      void triggerRefresh()
    })
  }

  onUnmounted(() => {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
    if (refreshOnFocus) {
      window.removeEventListener('focus', handleWindowFocus)
    }
    if (refreshOnVisible) {
      document.removeEventListener('visibilitychange', handleVisibilityChange)
    }
  })

  return {
    triggerRefresh,
  }
}
