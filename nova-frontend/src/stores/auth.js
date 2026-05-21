import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { TOKEN_KEY, USER_KEY } from '@/config/constants'

const DEFAULT_AVATAR = '🥳'

const readSession = (key) => sessionStorage.getItem(key)
const writeSession = (key, value) => sessionStorage.setItem(key, value)

const clearAuthStorage = () => {
  sessionStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(USER_KEY)
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

const resolveToken = () => {
  const sessionToken = readSession(TOKEN_KEY)
  if (sessionToken) return sessionToken

  const legacyToken = localStorage.getItem(TOKEN_KEY) || ''
  if (legacyToken) {
    writeSession(TOKEN_KEY, legacyToken)
    localStorage.removeItem(TOKEN_KEY)
  }
  return legacyToken
}

const resolveUser = () => {
  const sessionUser = readSession(USER_KEY)
  if (sessionUser) {
    return JSON.parse(sessionUser || 'null')
  }

  const legacyUser = localStorage.getItem(USER_KEY) || 'null'
  if (legacyUser && legacyUser !== 'null') {
    writeSession(USER_KEY, legacyUser)
    localStorage.removeItem(USER_KEY)
  }
  return JSON.parse(legacyUser || 'null')
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(resolveToken())
  const user = ref(resolveUser())

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const isGuest = computed(() => user.value?.role === 'GUEST')
  const nickname = computed(() => user.value?.nickname || '未登录')
  const avatar = computed(() => user.value?.avatar || DEFAULT_AVATAR)

  function setAuth(tokenValue, userInfo) {
    token.value = tokenValue
    user.value = userInfo
    writeSession(TOKEN_KEY, tokenValue)
    writeSession(USER_KEY, JSON.stringify(userInfo))
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  function patchUser(partial) {
    const next = { ...(user.value || {}), ...(partial || {}) }
    user.value = next
    writeSession(USER_KEY, JSON.stringify(next))
  }

  function logout() {
    token.value = ''
    user.value = null
    clearAuthStorage()
  }

  return {
    token,
    user,
    isLoggedIn,
    isAdmin,
    isGuest,
    nickname,
    avatar,
    setAuth,
    patchUser,
    logout,
  }
})
