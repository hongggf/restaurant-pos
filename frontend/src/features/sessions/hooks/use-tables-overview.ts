import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"

import type { ApiError } from "@/lib/api/client"
import { getTablesOverview, openSession } from "../api/sessions"

// Customer phones send rounds at any moment — poll so the board stays live.
const OVERVIEW_REFETCH_MS = 5000

export function useTablesOverview() {
  return useQuery({
    queryKey: ["tables", "overview"],
    queryFn: getTablesOverview,
    refetchInterval: OVERVIEW_REFETCH_MS,
  })
}

export function useOpenSession() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: openSession,
    onSuccess: (session) => {
      queryClient.invalidateQueries({ queryKey: ["tables", "overview"] })
      toast.success(`Session opened for table ${session.tableNumber}`)
    },
    onError: (error: ApiError) => toast.error(error.message),
  })
}
