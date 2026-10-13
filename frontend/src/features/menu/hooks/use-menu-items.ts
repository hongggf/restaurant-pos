import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query"
import { toast } from "sonner"

import { listCategories } from "@/features/categories/api/categories"
import type { ApiError } from "@/lib/api/client"
import {
  createMenuItem,
  deleteMenuItem,
  listCurrencies,
  listMenuItems,
  updateMenuItem,
  type MenuItemListParams,
} from "../api/menu-items"
import type { MenuItemCreateRequest, MenuItemUpdateRequest } from "../types"

export const PAGE_SIZE = 12

export function useMenuItems(params: Omit<MenuItemListParams, "size">) {
  return useQuery({
    queryKey: ["menu-items", "list", params],
    queryFn: () => listMenuItems({ ...params, size: PAGE_SIZE }),
    placeholderData: keepPreviousData,
  })
}

/** All categories, for the filter dropdown and the form's category select. */
export function useCategoryOptions() {
  return useQuery({
    queryKey: ["categories", "options"],
    queryFn: () => listCategories({ page: 0, size: 100 }),
    select: (data) => data.content ?? [],
  })
}

export function useCurrencies() {
  return useQuery({
    queryKey: ["currencies"],
    queryFn: listCurrencies,
    staleTime: Infinity,
  })
}

import { uploadImage } from "@/features/upload/api/upload"

function useInvalidateMenuItems() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: ["menu-items"] })
}

export function useCreateMenuItem() {
  const invalidate = useInvalidateMenuItems()
  return useMutation({
    mutationFn: async ({
      body,
      image,
    }: {
      body: MenuItemCreateRequest & { imageUrl?: string | null }
      image?: File
    }) => {
      const finalBody = { ...body }
      if (image) {
        const { url } = await uploadImage(image)
        finalBody.imageUrl = url
      }
      return createMenuItem(finalBody)
    },
    onSuccess: (data) => {
      invalidate()
      toast.success(`Menu item "${data.nameEn}" created`)
    },
    onError: (error: ApiError) => toast.error(error.message),
  })
}

export function useUpdateMenuItem() {
  const invalidate = useInvalidateMenuItems()
  return useMutation({
    mutationFn: async ({
      id,
      body,
      image,
    }: {
      id: string
      body: MenuItemUpdateRequest & { imageUrl?: string | null }
      image?: File
    }) => {
      const finalBody = { ...body }
      if (image) {
        const { url } = await uploadImage(image)
        finalBody.imageUrl = url
      }
      return updateMenuItem(id, finalBody)
    },
    onSuccess: (data) => {
      invalidate()
      toast.success(`Menu item "${data.nameEn}" updated`)
    },
    onError: (error: ApiError) => toast.error(error.message),
  })
}

export function useDeleteMenuItem() {
  const invalidate = useInvalidateMenuItems()
  return useMutation({
    mutationFn: deleteMenuItem,
    onSuccess: () => {
      invalidate()
      toast.success("Menu item deleted")
    },
    onError: (error: ApiError) => toast.error(error.message),
  })
}

export function useUpdateMenuItemAvailability() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, available }: { id: string; available: boolean }) => {
      // NOTE: Import updateMenuItemAvailability manually or it might fail if we don't fix imports
      const { updateMenuItemAvailability } = await import("../api/menu-items")
      return updateMenuItemAvailability(id, available)
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ["menu-items"] })
      toast.success(`Menu item "${data.nameEn}" marked as ${data.available ? "available" : "sold out"}`)
    },
    onError: (error: ApiError) => toast.error(error.message),
  })
}
