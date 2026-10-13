import { apiFetch } from "@/lib/api/client"
import type { CashierRound } from "@/features/sessions/types"
import type { CashierRoundRequest } from "../types"

export function submitCashierRound(
  sessionId: string,
  body: CashierRoundRequest
) {
  return apiFetch<CashierRound>(`/sessions/${sessionId}/rounds`, {
    method: "POST",
    body: JSON.stringify(body),
  })
}
