import { useMutation, useQuery } from "@tanstack/react-query"
import { toast } from "sonner"

import type { ApiError } from "@/lib/api/client"
import { fetchReceiptPdf, getSessionReceipt } from "../api/receipts"

export function useSessionReceipt(sessionId: string) {
  return useQuery({
    queryKey: ["sessions", sessionId, "receipt"],
    queryFn: () => getSessionReceipt(sessionId),
  })
}

/** Downloads the PDF and opens it in a new tab for printing/saving. */
export function useOpenReceiptPdf() {
  return useMutation({
    mutationFn: fetchReceiptPdf,
    onSuccess: (blob) => {
      const url = URL.createObjectURL(blob)
      const newWin = window.open(url, "_blank", "noopener")
      if (!newWin) {
        toast.error("Popup blocked. Please allow popups to view the receipt.")
      }
      // Give the new tab time to load the blob before revoking.
      setTimeout(() => URL.revokeObjectURL(url), 60_000)
    },
    onError: (error: ApiError) => toast.error(error.message),
  })
}
