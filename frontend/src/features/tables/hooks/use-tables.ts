import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query"
import { toast } from "sonner"

import type { ApiError } from "@/lib/api/client"
import {
  createTable,
  deleteTable,
  listTables,
  regenerateQrToken,
  updateTable,
} from "../api/tables"
import type { TableUpdateRequest } from "../types"

export const PAGE_SIZE = 10

export function useTables(page: number) {
  return useQuery({
    queryKey: ["tables", "list", page],
    queryFn: () => listTables({ page, size: PAGE_SIZE }),
    placeholderData: keepPreviousData,
  })
}

function useInvalidateTables() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: ["tables"] })
}

export function useCreateTable() {
  const invalidate = useInvalidateTables()
  return useMutation({
    mutationFn: createTable,
    onSuccess: (data) => {
      invalidate()
      toast.success(`Table ${data.tableNumber} created`)
    },
    onError: (error: ApiError) => toast.error(error.message),
  })
}

export function useUpdateTable() {
  const invalidate = useInvalidateTables()
  return useMutation({
    mutationFn: ({ id, ...body }: TableUpdateRequest & { id: string }) =>
      updateTable(id, body),
    onSuccess: (data) => {
      invalidate()
      toast.success(`Table ${data.tableNumber} updated`)
    },
    onError: (error: ApiError) => toast.error(error.message),
  })
}

export function useDeleteTable() {
  const invalidate = useInvalidateTables()
  return useMutation({
    mutationFn: deleteTable,
    onSuccess: () => {
      invalidate()
      toast.success("Table deleted")
    },
    onError: (error: ApiError) => toast.error(error.message),
  })
}

export function useRegenerateQrToken() {
  const invalidate = useInvalidateTables()
  return useMutation({
    mutationFn: regenerateQrToken,
    onSuccess: (data) => {
      invalidate()
      toast.success(
        `QR code for table ${data.tableNumber} regenerated — old printed codes no longer work`
      )
    },
    onError: (error: ApiError) => toast.error(error.message),
  })
}
