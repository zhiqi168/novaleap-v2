const TOKEN_KEY = 'nova_admin_token'

export function getAdminToken() {
  const sessionToken = sessionStorage.getItem(TOKEN_KEY)
  if (sessionToken) {
    return sessionToken
  }

  const legacyToken = localStorage.getItem(TOKEN_KEY) || ''
  if (legacyToken) {
    sessionStorage.setItem(TOKEN_KEY, legacyToken)
    localStorage.removeItem(TOKEN_KEY)
  }
  return legacyToken
}

export function setAdminToken(token) {
  if (token) {
    sessionStorage.setItem(TOKEN_KEY, token)
  } else {
    sessionStorage.removeItem(TOKEN_KEY)
  }
  localStorage.removeItem(TOKEN_KEY)
}

export function clearAdminToken() {
  sessionStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(TOKEN_KEY)
}
