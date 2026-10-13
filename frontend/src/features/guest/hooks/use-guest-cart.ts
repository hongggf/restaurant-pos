import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"

import type { ApiError } from "@/lib/api/client"
import {
  addGuestCartLine,
  clearGuestCart,
  getGuestCart,
  removeGuestCartLine,
  sendGuestCart,
  updateGuestCartLine,
} from "../api/cart"
import { guestSessionStore } from "../lib/session-store"
import type { CartLineAddRequest, CartLineUpdateRequest, CartResponse } from "../types"

const CART_KEY = ["guest", "cart"]

export function useGuestCart() {
  return useQuery({
    queryKey: CART_KEY,
    queryFn: getGuestCart,
  })
}

function useUpdateCartCache() {
  const queryClient = useQueryClient()
  return (cart: CartResponse) => queryClient.setQueryData(CART_KEY, cart)
}

export function useAddCartLine() {
  const updateCache = useUpdateCartCache()
  return useMutation({
    mutationFn: (body: CartLineAddRequest) => addGuestCartLine(body),
    onSuccess: (cart) => {
      updateCache(cart)
      toast.success("Added to cart")
    },
    onError: (error: ApiError) => toast.error(error.message),
  })
}

export function useUpdateCartLine() {
  const updateCache = useUpdateCartCache()
  return useMutation({
    mutationFn: ({ lineId, body }: { lineId: string; body: CartLineUpdateRequest }) =>
      updateGuestCartLine(lineId, body),
    onSuccess: updateCache,
    onError: (error: ApiError) => toast.error(error.message),
  })
}

export function useRemoveCartLine() {
  const updateCache = useUpdateCartCache()
  return useMutation({
    mutationFn: (lineId: string) => removeGuestCartLine(lineId),
    onSuccess: updateCache,
    onError: (error: ApiError) => toast.error(error.message),
  })
}

export function useClearCart() {
  const updateCache = useUpdateCartCache()
  return useMutation({
    mutationFn: clearGuestCart,
    onSuccess: updateCache,
    onError: (error: ApiError) => toast.error(error.message),
  })
}

export function useSendCart() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: sendGuestCart,
    onSuccess: (orders) => {
      queryClient.setQueryData(["guest", "orders"], orders)
      queryClient.invalidateQueries({ queryKey: CART_KEY })
      guestSessionStore.markSpent()
      toast.success("Order sent")
    },
    onError: (error: ApiError) => toast.error(error.message),
  })
}
