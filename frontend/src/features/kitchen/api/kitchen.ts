import { apiFetch } from "@/lib/api/client"
import type { CashierRound } from "@/features/sessions/types"

/**
 * The kitchen's own endpoints. They return the same payload as the cashier's
 * `/rounds`, but sit behind ADMIN/CHEF rather than ADMIN/CASHIER so a kitchen
 * tablet can't reach cancel, void or payment actions.
 */
export function getKitchenQueue(lineStatus: string = "SENT") {
  return apiFetch<CashierRound[]>(`/kitchen/rounds?lineStatus=${lineStatus}`)
}

export function markKitchenRoundReady(roundId: string) {
  return apiFetch<CashierRound>(`/kitchen/rounds/${roundId}/ready`, {
    method: "PUT",
  })
}

export function startCookingKitchenRound(roundId: string) {
  return apiFetch<CashierRound>(`/kitchen/rounds/${roundId}/start-cooking`, {
    method: "PUT",
  })
}

export function bumpKitchenRound(roundId: string) {
  return apiFetch<CashierRound>(`/kitchen/rounds/${roundId}/bump`, {
    method: "PUT",
  })
}

export function updateKitchenLineStatus({ roundId, lineId, status }: { roundId: string, lineId: string, status: string }) {
  return apiFetch<CashierRound>(`/kitchen/rounds/${roundId}/lines/${lineId}/status?status=${status}`, {
    method: "PUT",
  })
}
