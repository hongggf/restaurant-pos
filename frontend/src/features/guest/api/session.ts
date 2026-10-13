import { apiFetch } from "@/lib/api/client"
import type { GuestSessionResponse } from "../types"

export function resolveGuestSession(qrToken: string) {
  return apiFetch<GuestSessionResponse>("/guest/sessions", {
    method: "POST",
    body: JSON.stringify({ qrToken }),
  })
}
