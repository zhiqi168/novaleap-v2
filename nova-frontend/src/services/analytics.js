import { withApiBase } from '@/config/api'

const VISITOR_KEY = 'nova_visitor_id'

let lastTrackedPath = ''
let lastTrackedAt = 0

function buildVisitorId() {
  if (window.crypto?.randomUUID) {
    return window.crypto.randomUUID()
  }
  return `v_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`
}

export function getVisitorId() {
  let visitorId = localStorage.getItem(VISITOR_KEY)
  if (!visitorId) {
    visitorId = buildVisitorId()
    localStorage.setItem(VISITOR_KEY, visitorId)
  }
  return visitorId
}

export function regenerateVisitorId() {
  const visitorId = buildVisitorId()
  localStorage.setItem(VISITOR_KEY, visitorId)
  return visitorId
}

export async function reportVisit(path) {
  const safePath = typeof path === 'string' && path.length ? path : '/'
  const now = Date.now()

  // avoid accidental duplicate reporting in very short intervals
  if (safePath === lastTrackedPath && now - lastTrackedAt < 800) {
    return
  }
  lastTrackedPath = safePath
  lastTrackedAt = now

  const headers = {
    'Content-Type': 'application/json'
  }

  const token = sessionStorage.getItem('nova_token') || localStorage.getItem('nova_token')
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  try {
    await fetch(withApiBase('/api/analytics/visit'), {
      method: 'POST',
      headers,
      body: JSON.stringify({
        visitorId: getVisitorId(),
        path: safePath
      })
    })
  } catch (_) {
    // do not break UI for analytics failures
  }
}
