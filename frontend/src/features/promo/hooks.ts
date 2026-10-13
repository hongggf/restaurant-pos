import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"
import type { ApiError } from "@/lib/api/client"
import {
  createPromo,
  deletePromo,
  getActivePromo,
  listPromos,
  updatePromo,
  type GuestPromoDto,
} from "./api/promotions"
import { uploadImage } from "@/features/upload/api/upload"

export function usePromos() {
  return useQuery({
    queryKey: ["promos"],
    queryFn: listPromos,
  })
}

export function useActivePromo() {
  return useQuery({
    queryKey: ["promos", "active"],
    queryFn: getActivePromo,
  })
}

export function useCreatePromo() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async ({
      dto,
      image,
    }: {
      dto: GuestPromoDto
      image?: File
    }) => {
      const finalDto = { ...dto }
      if (image) {
        const { url } = await uploadImage(image)
        finalDto.imageUrl = url
      }
      return createPromo(finalDto)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["promos"] })
      toast.success("Promo created")
    },
    onError: (error: ApiError) => toast.error(error.message),
  })
}

export function useUpdatePromo() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async ({
      id,
      dto,
      image,
    }: {
      id: number
      dto: GuestPromoDto
      image?: File
    }) => {
      const finalDto = { ...dto }
      if (image) {
        const { url } = await uploadImage(image)
        finalDto.imageUrl = url
      }
      return updatePromo(id, finalDto)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["promos"] })
      toast.success("Promo updated")
    },
    onError: (error: ApiError) => toast.error(error.message),
  })
}

export function useDeletePromo() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: deletePromo,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["promos"] })
      toast.success("Promo deleted")
    },
    onError: (error: ApiError) => toast.error(error.message),
  })
}
