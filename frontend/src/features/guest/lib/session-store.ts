import type { ApiError } from "@/lib/api/client"
import {
  clearGuestSession,
  getGuestSessionMeta,
  isGuestSpent,
  setGuestSpent,
} from "@/lib/auth/guest-token"

export type GuestSessionState =
  | { status: "no-session" }
  | { status: "resolving" }
  | { status: "browsing"; sessionId: string; tableNumber: string }
  | { status: "spent"; sessionId: string; tableNumber: string }
  | { status: "closed" }

function initialState(): GuestSessionState {
  const meta = getGuestSessionMeta()
  if (!meta) return { status: "no-session" }
  if (isGuestSpent()) {
    return { status: "spent", ...meta }
  }
  return { status: "browsing", ...meta }
}

let state: GuestSessionState = initialState()
const listeners = new Set<() => void>()

function set(next: GuestSessionState) {
  state = next
  listeners.forEach((listener) => listener())
}

export const guestSessionStore = {
  getState(): GuestSessionState {
    return state
  },
  subscribe(listener: () => void): () => void {
    listeners.add(listener)
    return () => listeners.delete(listener)
  },
  setResolving() {
    set({ status: "resolving" })
  },
  setNoSession() {
    clearGuestSession()
    set({ status: "no-session" })
  },
  setBrowsing(meta: { sessionId: string; tableNumber: string }) {
    setGuestSpent(false)
    set({ status: "browsing", ...meta })
  },
  markSpent() {
    if (state.status === "browsing" || state.status === "spent") {
      setGuestSpent(true)
      set({ status: "spent", sessionId: state.sessionId, tableNumber: state.tableNumber })
    }
  },
  handleApiError(err: ApiError) {
    if (err.status === 401) {
      clearGuestSession()
      set({ status: "no-session" })
    } else if (err.status === 410) {
      clearGuestSession()
      set({ status: "closed" })
    } else if (err.status === 403) {
      guestSessionStore.markSpent()
    }
  },
}
