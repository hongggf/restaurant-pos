import { clearToken, getToken } from "@/lib/auth/token"

const BASE_URL = import.meta.env.VITE_API_URL ?? ""

interface ApiEnvelope<T> {
  success: boolean
  message: string
  data: T
  timestamp: string
}

export class ApiError extends Error {
  readonly status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = "ApiError"
    this.status = status
  }
}

export async function apiFetch<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const headers = new Headers(options.headers)
  if (
    options.body &&
    !(options.body instanceof FormData) &&
    !headers.has("Content-Type")
  ) {
    headers.set("Content-Type", "application/json")
  }
  const token = getToken()
  if (token && !headers.has("Authorization")) {
    headers.set("Authorization", `Bearer ${token}`)
  }

  let response: Response
  try {
    response = await fetch(`${BASE_URL}/api/v1${path}`, { ...options, headers })
  } catch {
    throw new ApiError("Cannot reach the server. Is the API running?", 0)
  }

  let envelope: ApiEnvelope<T> | null = null
  try {
    envelope = await response.json()
  } catch {
    // non-JSON body (e.g. gateway error page); fall through to status check
  }

  if (!response.ok || !envelope?.success) {
    if (response.status === 401) {
      clearToken()
      window.location.href = `/login?from=${encodeURIComponent(
        window.location.pathname + window.location.search
      )}`
    }
    throw new ApiError(
      envelope?.message ?? `Request failed (${response.status})`,
      response.status
    )
  }

  return envelope.data
}

/** Resolve a backend-relative asset path (e.g. /uploads/…) to a fetchable URL. */
export function assetUrl(path: string | null | undefined): string | null {
  if (!path) return null
  if (/^https?:\/\//.test(path)) return path
  if (path.startsWith("/food-images/")) return path
  return `${BASE_URL}${path}`
}
