import { guestSessionStore } from "@/features/guest/lib/session-store"
import { getGuestToken } from "@/lib/auth/guest-token"
import { apiFetch, ApiError } from "./client"

export async function guestApiFetch<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const headers = new Headers(options.headers)
  const token = getGuestToken()
  if (token) {
    headers.set("Authorization", `Bearer ${token}`)
  }
  try {
    return await apiFetch<T>(path, { ...options, headers })
  } catch (err) {
    if (err instanceof ApiError) {
      guestSessionStore.handleApiError(err)
    }
    throw err
  }
}
