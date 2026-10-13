import { ApiError, apiFetch, assetUrl } from "@/lib/api/client"
import { getToken } from "@/lib/auth/token"
import type { Receipt } from "../types"

export function getSessionReceipt(sessionId: string) {
  return apiFetch<Receipt>(`/sessions/${sessionId}/receipt`)
}

/** The PDF endpoint returns raw bytes, not the JSON envelope. */
export async function fetchReceiptPdf(receiptId: string): Promise<Blob> {
  const response = await fetch(assetUrl(`/api/v1/receipts/${receiptId}/pdf`)!, {
    headers: { Authorization: `Bearer ${getToken()}` },
  })
  if (!response.ok) {
    throw new ApiError(
      `Couldn't download the receipt PDF (${response.status})`,
      response.status
    )
  }
  return response.blob()
}
