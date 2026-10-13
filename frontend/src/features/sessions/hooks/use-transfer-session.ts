import { useMutation, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"
import { apiFetch } from "@/lib/api/client"

export function useTransferSession() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async ({ sessionId, targetTableId }: { sessionId: string; targetTableId: string }) => {
      await apiFetch(`/sessions/${sessionId}/transfer`, { method: "PATCH", body: JSON.stringify({ targetTableId }) })
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["tables", "overview"] })
      // We don't invalidate session strictly if we navigate, but it's good to clear
      toast.success("Session moved successfully")
    },
    onError: (error: any) => toast.error(error.message),
  })
}
