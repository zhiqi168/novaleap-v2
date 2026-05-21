const API_BASE_URL = String(import.meta.env.VITE_API_BASE_URL || '').trim().replace(/\/+$/, '')

export function withApiBase(url = '/') {
  const raw = String(url || '').trim()
  if (!raw) return `${API_BASE_URL}/`
  if (/^https?:\/\//i.test(raw)) return raw
  return `${API_BASE_URL}${raw.startsWith('/') ? raw : `/${raw}`}`
}

export { API_BASE_URL }
