import { apiFetch } from "@/lib/api/client"
import type { CashierRound } from "../types"

export function getSessionRounds(sessionId: string) {
  return apiFetch<CashierRound[]>(`/sessions/${sessionId}/rounds`)
}


export function cancelRound(roundId: string, reason: string) {
  return apiFetch<CashierRound>(`/rounds/${roundId}/cancel`, {
    method: "PUT",
    body: JSON.stringify({ reason }),
  })
}

export function voidRoundLine(roundId: string, lineId: string, reason: string) {
  return apiFetch<CashierRound>(`/rounds/${roundId}/lines/${lineId}/void`, {
    method: "PUT",
    body: JSON.stringify({ reason }),
  })
}

export function updateRoundLineSelections(
  roundId: string,
  lineId: string,
  selections: { modifierOptionId: string; quantity: number }[]
) {
  return apiFetch<CashierRound>(
    `/rounds/${roundId}/lines/${lineId}/selections`,
    {
      method: "PUT",
      body: JSON.stringify({ selections }),
    }
  )
}
