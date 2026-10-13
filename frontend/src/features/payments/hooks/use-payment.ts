import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"

import type { ApiError } from "@/lib/api/client"
import { getBill, paySession } from "../api/payments"
import type { PaymentRequest } from "../types"

// Guests can still send rounds until the session is paid — keep the bill live.
const BILL_REFETCH_MS = 5000

export function useBill(sessionId: string) {
  return useQuery({
    queryKey: ["sessions", sessionId, "bill"],
    queryFn: () => getBill(sessionId),
    refetchInterval: BILL_REFETCH_MS,
  })
}

export function usePay(sessionId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: PaymentRequest) => paySession(sessionId, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["sessions", sessionId] })
      queryClient.invalidateQueries({ queryKey: ["tables", "overview"] })
      toast.success("Payment recorded — session closed")
    },
    onError: (error: ApiError) => toast.error(error.message),
  })
}
