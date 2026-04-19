import { clearAdminToken, getAdminToken } from '@/composables/adminAuthStorage'

const BASE_URL = String(import.meta.env.VITE_API_BASE_URL || '').trim().replace(/\/+$/, '')

function normalizeUrl(url) {
  const raw = String(url || '').trim()
  if (!raw) return '/'
  if (/^https?:\/\//i.test(raw)) {
    return raw
  }
  return raw.startsWith('/') ? raw : `/${raw}`
}

async function request(url, options = {}) {
  const token = getAdminToken()
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
    if (response.status === 401 || response.status === 403) {
      clearAdminToken()
      window.location.href = '/login'
    }
    const errorData = await response.json().catch(() => ({}))
    throw new Error(errorData.msg || `Request failed: HTTP ${response.status}`)
  }

  return response.json()
}

export const api = {
  get: (url) => request(url, { method: 'GET' }),
  post: (url, body) => request(url, { method: 'POST', body }),
  put: (url, body) => request(url, { method: 'PUT', body }),
  delete: (url) => request(url, { method: 'DELETE' }),
  upload: (url, formData) => request(url, { method: 'POST', body: formData }),
}
