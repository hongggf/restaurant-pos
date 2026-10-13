import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"

import type { ApiError } from "@/lib/api/client"
import {
  cancelRound,
  getSessionRounds,
  updateRoundLineSelections,
  voidRoundLine,
} from "../api/rounds"

// Guests keep ordering from their phones while the cashier has this open.
const ROUNDS_REFETCH_MS = 5000

export function useSessionRounds(sessionId: string) {
  return useQuery({
    queryKey: ["sessions", sessionId, "rounds"],
    queryFn: () => getSessionRounds(sessionId),
    refetchInterval: ROUNDS_REFETCH_MS,
  })
}

function useInvalidateRounds(sessionId: string) {
  const queryClient = useQueryClient()
  return () => {
    queryClient.invalidateQueries({ queryKey: ["sessions", sessionId] })
    queryClient.invalidateQueries({ queryKey: ["tables", "overview"] })
  }
}

export function useCancelRound(sessionId: string) {
  const invalidate = useInvalidateRounds(sessionId)
  return useMutation({
    mutationFn: ({ roundId, reason }: { roundId: string; reason: string }) =>
      cancelRound(roundId, reason),
    onSuccess: (round) => {
      invalidate()
      toast.success(`Round #${round.roundNumber} cancelled`)
    },
    onError: (error: ApiError) => toast.error(error.message),
  })
}

export function useVoidLine(sessionId: string) {
  const invalidate = useInvalidateRounds(sessionId)
  return useMutation({
    mutationFn: (vars: { roundId: string; lineId: string; reason: string }) =>
      voidRoundLine(vars.roundId, vars.lineId, vars.reason),
    onSuccess: () => {
      invalidate()
      toast.success("Item voided")
    },
    onError: (error: ApiError) => toast.error(error.message),
  })
}

export function useUpdateLineSelections(sessionId: string) {
  const invalidate = useInvalidateRounds(sessionId)
  return useMutation({
    mutationFn: (vars: {
      roundId: string
      lineId: string
      selections: { modifierOptionId: string; quantity: number }[]
    }) =>
      updateRoundLineSelections(vars.roundId, vars.lineId, vars.selections),
    onSuccess: () => {
      invalidate()
      toast.success("Item updated")
    },
    onError: (error: ApiError) => toast.error(error.message),
  })
}
