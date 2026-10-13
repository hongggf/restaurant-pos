import { useSyncExternalStore } from "react"
import { guestSessionStore, type GuestSessionState } from "../lib/session-store"

export function useGuestSession(): GuestSessionState {
  return useSyncExternalStore(
    guestSessionStore.subscribe,
    guestSessionStore.getState
  )
}
