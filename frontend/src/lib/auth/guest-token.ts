const TOKEN_KEY = "guest.accessToken"
const EXPIRES_AT_KEY = "guest.tokenExpiresAt"
const SESSION_ID_KEY = "guest.sessionId"
const TABLE_NUMBER_KEY = "guest.tableNumber"
const SPENT_KEY = "guest.isSpent"

interface GuestSession {
  accessToken: string
  expiresInMs: number
  sessionId: string
  tableNumber: string
}

export function saveGuestSession(session: GuestSession) {
  localStorage.setItem(TOKEN_KEY, session.accessToken)
  localStorage.setItem(EXPIRES_AT_KEY, String(Date.now() + session.expiresInMs))
  localStorage.setItem(SESSION_ID_KEY, session.sessionId)
  localStorage.setItem(TABLE_NUMBER_KEY, session.tableNumber)
  localStorage.removeItem(SPENT_KEY)
}

export function setGuestSpent(spent: boolean) {
  if (spent) {
    localStorage.setItem(SPENT_KEY, "true")
  } else {
    localStorage.removeItem(SPENT_KEY)
  }
}

export function isGuestSpent(): boolean {
  return localStorage.getItem(SPENT_KEY) === "true"
}

export function getGuestToken(): string | null {
  const token = localStorage.getItem(TOKEN_KEY)
  const expiresAt = Number(localStorage.getItem(EXPIRES_AT_KEY))
  if (!token || !expiresAt || Date.now() >= expiresAt) {
    return null
  }
  return token
}

export function getGuestSessionMeta(): { sessionId: string; tableNumber: string } | null {
  if (!getGuestToken()) return null
  const sessionId = localStorage.getItem(SESSION_ID_KEY)
  const tableNumber = localStorage.getItem(TABLE_NUMBER_KEY)
  if (!sessionId || !tableNumber) return null
  return { sessionId, tableNumber }
}

export function clearGuestSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(EXPIRES_AT_KEY)
  localStorage.removeItem(SESSION_ID_KEY)
  localStorage.removeItem(TABLE_NUMBER_KEY)
  localStorage.removeItem(SPENT_KEY)
}
