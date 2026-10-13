import { apiFetch } from "@/lib/api/client"
import type { Bill, Payment, PaymentRequest } from "../types"

export function getBill(sessionId: string) {
  return apiFetch<Bill>(`/sessions/${sessionId}/bill`)
}

export function paySession(sessionId: string, body: PaymentRequest) {
  return apiFetch<Payment>(`/sessions/${sessionId}/payments`, {
    method: "POST",
    body: JSON.stringify(body),
  })
}
