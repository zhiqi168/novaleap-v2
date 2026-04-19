import { ref } from 'vue'
import { withApiBase } from '@/config/api'

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

  const sanitizeErrorMessage = (message, statusCode) => {
    const raw = String(message || '').trim()
    if (!raw) {
      return statusCode ? `请求失败（HTTP ${statusCode}）` : '请求失败'
    }
    const lower = raw.toLowerCase()
    if (/https?:\/\//i.test(raw)) {
      return 'AI 服务暂时不可用，请稍后重试。'
    }
    if (lower.includes('401') || lower.includes('unauthorized')) {
      return 'AI 服务鉴权失败，请联系管理员。'
    }
    if (lower.includes('timeout') || lower.includes('timed out')) {
      return 'AI 服务请求超时，请稍后重试。'
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

  const startStream = async (url, options = {}) => {
    stopAppendTimer()
    pendingChunks = ''
    streamClosed = false
    content.value = ''
    items.value = []
    isStreaming.value = true
    isDone.value = false
    error.value = null

    abortController = new AbortController()
    const signal = abortController.signal
    const token = sessionStorage.getItem('nova_token') || localStorage.getItem('nova_token')

    try {
      const response = await fetch(withApiBase(url), {
        method: options.method || 'GET',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
          Accept: 'text/event-stream',
        },
        signal,
        body: options.body ? JSON.stringify(options.body) : undefined,
      })

      if (!response.ok) {
        const reason = await response.text().catch(() => '')
        throw new Error(sanitizeErrorMessage(reason || `HTTP ${response.status}`, response.status))
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      let eventDataLines = []

      const handleSsePayload = (payload) => {
        const raw = String(payload ?? '')
        const trimmed = raw.trim()
        if (!trimmed) return

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
            // Compatible with OpenAI-style stream chunks.
            enqueueContentChunk(parsed.choices[0].delta.content)
          }
        } catch (_) {
          // Compatible with plain text SSE: treat non-JSON as a content chunk.
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
          if (line.startsWith(':')) {
            continue
          }

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
            continue
          }
        }
      }

      if (eventDataLines.length > 0) {
        handleSsePayload(eventDataLines.join('\n'))
      }
    } catch (e) {
      error.value = sanitizeErrorMessage(e?.message)
      streamClosed = true
      finalizeStreamingState()
    } finally {
      streamClosed = true
      finalizeStreamingState()
    }
  }

  const reset = () => {
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
    if (abortController) {
      try {
        abortController.abort()
      } catch (_) {
        // no-op
      }
    }
    stopAppendTimer()
    pendingChunks = ''
    streamClosed = true
    isStreaming.value = false
  }

  return { content, items, isStreaming, isDone, error, startStream, reset, abort }
}
