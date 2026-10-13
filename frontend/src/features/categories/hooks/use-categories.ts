import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query"
import { toast } from "sonner"

import type { ApiError } from "@/lib/api/client"
import {
  createCategory,
  deleteCategory,
  listCategories,
  updateCategory,
} from "../api/categories"
import type { CategoryUpdateRequest } from "../types"

export const PAGE_SIZE = 10

export function useCategories(page: number) {
  return useQuery({
    queryKey: ["categories", "list", page],
    queryFn: () => listCategories({ page, size: PAGE_SIZE }),
    placeholderData: keepPreviousData,
  })
}

function useInvalidateCategories() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: ["categories"] })
}

export function useCreateCategory() {
  const invalidate = useInvalidateCategories()
  return useMutation({
    mutationFn: createCategory,
    onSuccess: (data) => {
      invalidate()
      toast.success(`Category "${data.nameEn}" created`)
    },
    onError: (error: ApiError) => toast.error(error.message),
  })
}

export function useUpdateCategory() {
  const invalidate = useInvalidateCategories()
  return useMutation({
    mutationFn: ({ id, ...body }: CategoryUpdateRequest & { id: string }) =>
      updateCategory(id, body),
    onSuccess: (data) => {
      invalidate()
      toast.success(`Category "${data.nameEn}" updated`)
    },
    onError: (error: ApiError) => toast.error(error.message),
  })
}

export function useDeleteCategory() {
  const invalidate = useInvalidateCategories()
  return useMutation({
    mutationFn: deleteCategory,
    onSuccess: () => {
      invalidate()
      toast.success("Category deleted")
    },
    onError: (error: ApiError) => toast.error(error.message),
  })
}
