import { ref } from 'vue'
import { TOKEN_KEY, USER_KEY } from '@/config/constants'
import { withApiBase } from '@/config/api'

const DEFAULT_TIMEOUT_MS = 12000

function isRetryableStatus(status) {
  return [408, 429, 502, 503, 504].includes(Number(status))
}

function createTimeoutError(timeoutMs) {
  const error = new Error(`请求超时（>${Math.ceil(Number(timeoutMs || DEFAULT_TIMEOUT_MS) / 1000)}s）`)
  error.name = 'TimeoutError'
  error.retryable = true
  return error
}

function isRetryableNetworkError(error) {
  if (!error) return false
  if (error.name === 'TimeoutError') return true
  const message = String(error.message || '').toLowerCase()
  return message.includes('failed to fetch')
    || message.includes('networkerror')
    || message.includes('load failed')
    || message.includes('fetch failed')
}

const readToken = () => sessionStorage.getItem(TOKEN_KEY) || localStorage.getItem(TOKEN_KEY)

const clearAuthStorage = () => {
  sessionStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(USER_KEY)
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

async function request(url, options = {}) {
  const {
    timeoutMs = DEFAULT_TIMEOUT_MS,
    retryCount = 0,
    retryDelayMs = 500,
    ...fetchOptions
  } = options

  const token = readToken()
  const isFormData = fetchOptions.body instanceof FormData
  const headers = {
    ...fetchOptions.headers,
  }

  if (!isFormData) {
    headers['Content-Type'] = headers['Content-Type'] || 'application/json'
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const targetUrl = withApiBase(url)
  let lastError = null

  for (let attempt = 0; attempt <= retryCount; attempt += 1) {
    const controller = new AbortController()
    const timeoutId = window.setTimeout(() => controller.abort(createTimeoutError(timeoutMs)), timeoutMs)

    try {
      const response = await fetch(targetUrl, {
        ...fetchOptions,
        headers,
        signal: controller.signal,
        body: fetchOptions.body
          ? (isFormData ? fetchOptions.body : JSON.stringify(fetchOptions.body))
          : undefined,
      })

      if (!response.ok) {
        if (response.status === 401) {
          clearAuthStorage()
          window.location.href = '/login'
        }
        const errorData = await response.json().catch(() => ({}))
        const error = new Error(errorData.msg || `请求失败: HTTP ${response.status}`)
        error.status = response.status
        error.retryable = isRetryableStatus(response.status)
        throw error
      }

      return response.json()
    } catch (error) {
      if (error?.name === 'AbortError') {
        lastError = error?.cause instanceof Error ? error.cause : createTimeoutError(timeoutMs)
      } else {
        lastError = error
      }

      const canRetry = attempt < retryCount && (
        lastError?.retryable === true || isRetryableNetworkError(lastError)
      )

      if (!canRetry) {
        throw lastError
      }

      await sleep(retryDelayMs * (attempt + 1))
    } finally {
      window.clearTimeout(timeoutId)
    }
  }

  throw lastError || new Error('请求失败')
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
  post: (url, body, options = {}) => request(url, { method: 'POST', body, ...options }),
  put: (url, body, options = {}) => request(url, { method: 'PUT', body, ...options }),
  delete: (url, options = {}) => request(url, { method: 'DELETE', ...options }),
  upload: (url, formData, options = {}) => request(url, { method: 'POST', body: formData, ...options }),
}
