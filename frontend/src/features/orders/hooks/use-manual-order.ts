import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"

import { listAttachedModifierGroups } from "@/features/modifiers/api/modifier-groups"
import type { ApiError } from "@/lib/api/client"
import { submitCashierRound } from "../api/rounds"
import type { CashierRoundRequest } from "../types"

export function useItemModifierGroups(menuItemId: string | undefined) {
  return useQuery({
    queryKey: ["menu-items", menuItemId, "modifier-groups"],
    queryFn: () => listAttachedModifierGroups(menuItemId!),
    enabled: !!menuItemId,
  })
}

export function useSubmitRound(sessionId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: CashierRoundRequest) =>
      submitCashierRound(sessionId, body),
    onSuccess: (round) => {
      queryClient.invalidateQueries({ queryKey: ["sessions", sessionId] })
      queryClient.invalidateQueries({ queryKey: ["tables", "overview"] })
      toast.success(`Round #${round.roundNumber} sent`)
    },
    onError: (error: ApiError) => toast.error(error.message),
  })
}
