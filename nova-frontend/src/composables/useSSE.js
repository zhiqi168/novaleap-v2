import { ref } from 'vue'
import { withApiBase } from '@/config/api'
import { TOKEN_KEY } from '@/config/constants'

const DEFAULT_STREAM_TIMEOUT_MS = 25000
const DEFAULT_FIRST_CHUNK_TIMEOUT_MS = 6000

function sleep(ms) {
  return new Promise((resolve) => window.setTimeout(resolve, ms))
}

function createTimeoutError(message, name = 'TimeoutError') {
  const error = new Error(message)
  error.name = name
  error.retryable = true
  return error
}

function isRetryableStreamError(error) {
  if (!error) return false
  if (error.retryable === true) return true
  const message = String(error.message || '').toLowerCase()
  return message.includes('failed to fetch')
    || message.includes('networkerror')
    || message.includes('load failed')
    || message.includes('fetch failed')
}

export function useSSE() {
  const content = ref('')
  const isStreaming = ref(false)
  const isDone = ref(false)
  const error = ref(null)
  const items = ref([])
  let abortController = null
  let pendingChunks = ''
  let appendTimer = null
  let streamClosed = false
  let activeRequestId = 0

  const sanitizeErrorMessage = (message, statusCode) => {
    const raw = String(message || '').trim()
    if (!raw) {
      return statusCode ? `请求失败（HTTP ${statusCode}）` : '请求失败'
    }
    const lower = raw.toLowerCase()
    if (lower.includes('failed to fetch') || lower.includes('networkerror') || lower.includes('load failed')) {
      return '网络连接失败，AI 服务暂时不可用，请稍后重试。'
    }
    if (/https?:\/\//i.test(raw)) {
      return 'AI 服务暂时不可用，请稍后重试。'
    }
    if (lower.includes('401') || lower.includes('unauthorized')) {
      return 'AI 服务鉴权失败，请重新登录后重试。'
    }
    if (lower.includes('timeout') || lower.includes('timed out')) {
      return 'AI 服务响应超时，请稍后重试。'
    }
    return raw
  }

  const sanitizeDeltaContent = (chunk) => {
    const raw = String(chunk || '')
    if (!raw) return ''
    if (/https?:\/\//i.test(raw) && /unauthorized|401|ai\s*请求失败|ai\s*error/i.test(raw)) {
      return '\n\n>[AI 请求失败：AI 服务暂时不可用，请稍后重试。]'
    }
    return raw
  }

  const stopAppendTimer = () => {
    if (!appendTimer) return
    clearInterval(appendTimer)
    appendTimer = null
  }

  const finalizeStreamingState = () => {
    if (!streamClosed || pendingChunks.length > 0) return
    stopAppendTimer()
    isStreaming.value = false
    if (!isDone.value && !error.value) {
      isDone.value = true
    }
  }

  const ensureAppendTimer = () => {
    if (appendTimer) return
    appendTimer = setInterval(() => {
      if (!pendingChunks.length) {
        finalizeStreamingState()
        return
      }

      const step = Math.max(1, Math.min(26, Math.ceil(pendingChunks.length / 24)))
      content.value += pendingChunks.slice(0, step)
      pendingChunks = pendingChunks.slice(step)

      if (!pendingChunks.length) {
        finalizeStreamingState()
      }
    }, 18)
  }

  const enqueueContentChunk = (chunk) => {
    const text = sanitizeDeltaContent(chunk)
    if (!text) return
    pendingChunks += text
    ensureAppendTimer()
  }

  const clearExistingRequest = () => {
    if (abortController) {
      try {
        abortController.abort()
      } catch (_) {
        // no-op
      }
    }
  }

  const startStream = async (url, options = {}) => {
    const requestId = ++activeRequestId
    const retryCount = Math.max(0, Number(options.retryCount ?? 1))
    const retryDelayMs = Math.max(0, Number(options.retryDelayMs ?? 700))
    const timeoutMs = Math.max(1000, Number(options.timeoutMs ?? DEFAULT_STREAM_TIMEOUT_MS))
    const firstChunkTimeoutMs = Math.max(1000, Number(options.firstChunkTimeoutMs ?? DEFAULT_FIRST_CHUNK_TIMEOUT_MS))
    const token = sessionStorage.getItem(TOKEN_KEY) || localStorage.getItem(TOKEN_KEY)

    clearExistingRequest()
    stopAppendTimer()
    pendingChunks = ''
    streamClosed = false
    content.value = ''
    items.value = []
    isStreaming.value = true
    isDone.value = false
    error.value = null

    const attemptStream = async (attempt) => {
      if (requestId !== activeRequestId) return

      const controller = new AbortController()
      abortController = controller
      const headers = {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
      }
      if (token) {
        headers.Authorization = `Bearer ${token}`
      }

      const requestTimeoutId = window.setTimeout(() => {
        controller.abort(createTimeoutError('AI 服务整体响应超时，请稍后重试。'))
      }, timeoutMs)
      let firstChunkTimeoutId = 0

      const markFirstChunkReceived = () => {
        if (firstChunkTimeoutId) {
          window.clearTimeout(firstChunkTimeoutId)
          firstChunkTimeoutId = 0
        }
      }

      try {
        const response = await fetch(withApiBase(url), {
          method: options.method || 'GET',
          headers,
          signal: controller.signal,
          body: options.body ? JSON.stringify(options.body) : undefined,
        })

        if (!response.ok) {
          const reason = await response.text().catch(() => '')
          const streamError = new Error(sanitizeErrorMessage(reason || `HTTP ${response.status}`, response.status))
          streamError.status = response.status
          streamError.retryable = [408, 429, 502, 503, 504].includes(Number(response.status))
          throw streamError
        }

        if (!response.body) {
          throw new Error('AI 服务没有返回可读取的数据流。')
        }

        firstChunkTimeoutId = window.setTimeout(() => {
          controller.abort(createTimeoutError('AI 服务首包超时，请稍后重试。', 'FirstChunkTimeoutError'))
        }, firstChunkTimeoutMs)

        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''
        let eventDataLines = []

        const handleSsePayload = (payload) => {
          const raw = String(payload ?? '')
          const trimmed = raw.trim()
          if (!trimmed) return

          markFirstChunkReceived()

          if (trimmed === '[DONE]') {
            isDone.value = true
            streamClosed = true
            finalizeStreamingState()
            return
          }

          try {
            const parsed = JSON.parse(trimmed)
            if (parsed.type === 'delta') {
              enqueueContentChunk(parsed.content)
            } else if (parsed.type === 'item') {
              items.value[parsed.index - 1] = parsed.content
            } else if (parsed.type === 'error') {
              error.value = sanitizeErrorMessage(parsed.message || parsed.content || '请求失败')
              streamClosed = true
              finalizeStreamingState()
            } else if (parsed.type === 'done') {
              isDone.value = true
              streamClosed = true
              finalizeStreamingState()
            } else if (parsed.choices?.[0]?.delta?.content) {
              enqueueContentChunk(parsed.choices[0].delta.content)
            }
          } catch (_) {
            enqueueContentChunk(raw)
          }
        }

        while (true) {
          const { done, value } = await reader.read()
          if (done) break

          buffer += decoder.decode(value, { stream: true })
          const lines = buffer.split('\n')
          buffer = lines.pop() || ''

          for (const line of lines) {
            if (line.startsWith(':')) continue

            if (line.startsWith('data:')) {
              let dataPart = line.slice(5)
              if (dataPart.startsWith(' ')) dataPart = dataPart.slice(1)
              eventDataLines.push(dataPart)
              continue
            }

            if (line.trim() === '') {
              if (eventDataLines.length > 0) {
                handleSsePayload(eventDataLines.join('\n'))
                eventDataLines = []
              }
            }
          }
        }

        if (eventDataLines.length > 0) {
          handleSsePayload(eventDataLines.join('\n'))
        }

        markFirstChunkReceived()
        streamClosed = true
        finalizeStreamingState()
      } catch (e) {
        if (requestId !== activeRequestId) return

        const normalizedError = e?.name === 'AbortError'
          ? (e?.cause instanceof Error ? e.cause : createTimeoutError('AI 请求已中断。'))
          : e

        if (attempt < retryCount && isRetryableStreamError(normalizedError)) {
          await sleep(retryDelayMs * (attempt + 1))
          return attemptStream(attempt + 1)
        }

        error.value = sanitizeErrorMessage(normalizedError?.message)
        streamClosed = true
        finalizeStreamingState()
      } finally {
        markFirstChunkReceived()
        window.clearTimeout(requestTimeoutId)
      }
    }

    await attemptStream(0)
  }

  const reset = () => {
    clearExistingRequest()
    stopAppendTimer()
    pendingChunks = ''
    streamClosed = true
    content.value = ''
    items.value = []
    isStreaming.value = false
    isDone.value = false
    error.value = null
  }

  const abort = () => {
    clearExistingRequest()
    stopAppendTimer()
    pendingChunks = ''
    streamClosed = true
    isStreaming.value = false
  }

  return { content, items, isStreaming, isDone, error, startStream, reset, abort }
}
