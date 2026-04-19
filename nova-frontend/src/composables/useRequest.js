import { ref } from 'vue'

const BASE_URL = String(import.meta.env.VITE_API_BASE_URL || '').trim().replace(/\/+$/, '')

function normalizeUrl(url) {
  const raw = String(url || '').trim()
  if (!raw) return '/'
  if (/^https?:\/\//i.test(raw)) {
    return raw
  }
  return raw.startsWith('/') ? raw : `/${raw}`
}

const readToken = () => sessionStorage.getItem('nova_token') || localStorage.getItem('nova_token')
const clearAuthStorage = () => {
  sessionStorage.removeItem('nova_token')
  sessionStorage.removeItem('nova_user')
  localStorage.removeItem('nova_token')
  localStorage.removeItem('nova_user')
}

async function request(url, options = {}) {
  const token = readToken()
  const isFormData = options.body instanceof FormData

  const headers = {
    ...options.headers,
  }
  if (!isFormData) {
    headers['Content-Type'] = headers['Content-Type'] || 'application/json'
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const response = await fetch(`${BASE_URL}${normalizeUrl(url)}`, {
    ...options,
    headers,
    body: options.body
      ? (isFormData ? options.body : JSON.stringify(options.body))
      : undefined,
  })

  if (!response.ok) {
    if (response.status === 401) {
      clearAuthStorage()
      window.location.href = '/login'
    }
    const errorData = await response.json().catch(() => ({}))
    throw new Error(errorData.msg || `请求失败: HTTP ${response.status}`)
  }

  return response.json()
}

export function useRequest(apiFn) {
  const data = ref(null)
  const loading = ref(false)
  const error = ref(null)

  async function execute(...args) {
    loading.value = true
    error.value = null
    try {
      const result = await apiFn(...args)
      if (result.code === 200) {
        data.value = result.data
      } else {
        throw new Error(result.msg || '未知错误')
      }
      return data.value
    } catch (e) {
      error.value = e.message
      throw e
    } finally {
      loading.value = false
    }
  }

  return { data, loading, error, execute }
}

export const api = {
  get: (url, options = {}) => request(url, { method: 'GET', ...options }),
  post: (url, body) => request(url, { method: 'POST', body }),
  put: (url, body) => request(url, { method: 'PUT', body }),
  delete: (url) => request(url, { method: 'DELETE' }),
  upload: (url, formData) => request(url, { method: 'POST', body: formData }),
}
