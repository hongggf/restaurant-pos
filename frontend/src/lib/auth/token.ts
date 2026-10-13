const TOKEN_KEY = "pos.accessToken"
const EXPIRES_AT_KEY = "pos.tokenExpiresAt"

export function saveToken(accessToken: string, expiresInMs: number) {
  localStorage.setItem(TOKEN_KEY, accessToken)
  localStorage.setItem(EXPIRES_AT_KEY, String(Date.now() + expiresInMs))
}

export function getToken(): string | null {
  const token = localStorage.getItem(TOKEN_KEY)
  const expiresAt = Number(localStorage.getItem(EXPIRES_AT_KEY))
  if (!token || !expiresAt || Date.now() >= expiresAt) {
    return null
  }
  return token
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(EXPIRES_AT_KEY)
}

export type Role = "ADMIN" | "CASHIER" | "CHEF"

interface JwtPayload {
  sub?: string
  roles?: unknown
  authorities?: unknown
  scope?: unknown
}

function decodePayload(token: string): JwtPayload {
  try {
    const base64 = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/")
    return JSON.parse(atob(base64))
  } catch {
    return {}
  }
}

// Claim name varies by backend config (roles / authorities / scope), so
// collect from all of them and normalize away Spring's ROLE_ prefix.
export function getRoles(): Role[] {
  const token = getToken()
  if (!token) return []
  const payload = decodePayload(token)
  const raw = [payload.roles, payload.authorities, payload.scope]
    .flatMap((claim) => {
      if (typeof claim === "string") return claim.split(" ")
      if (Array.isArray(claim)) return claim
      return []
    })
    .filter((value): value is string => typeof value === "string")
  const normalized = raw.map((role) => role.replace(/^ROLE_/, "").toUpperCase())
  return [...new Set(normalized)].filter(
    (role): role is Role =>
      role === "ADMIN" || role === "CASHIER" || role === "CHEF"
  )
}

export function getUsername(): string | null {
  const token = getToken()
  return token ? (decodePayload(token).sub ?? null) : null
}
