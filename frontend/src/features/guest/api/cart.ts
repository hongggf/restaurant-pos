import { guestApiFetch } from "@/lib/api/guest-client"
import type {
  CartLineAddRequest,
  CartLineUpdateRequest,
  CartResponse,
  GuestOrdersResponse,
} from "../types"

export function getGuestCart() {
  return guestApiFetch<CartResponse>("/guest/cart")
}

export function addGuestCartLine(body: CartLineAddRequest) {
  return guestApiFetch<CartResponse>("/guest/cart/lines", {
    method: "POST",
    body: JSON.stringify(body),
  })
}

export function updateGuestCartLine(lineId: string, body: CartLineUpdateRequest) {
  return guestApiFetch<CartResponse>(`/guest/cart/lines/${lineId}`, {
    method: "PUT",
    body: JSON.stringify(body),
  })
}

export function removeGuestCartLine(lineId: string) {
  return guestApiFetch<CartResponse>(`/guest/cart/lines/${lineId}`, {
    method: "DELETE",
  })
}

export function clearGuestCart() {
  return guestApiFetch<CartResponse>("/guest/cart", { method: "DELETE" })
}

export function sendGuestCart() {
  return guestApiFetch<GuestOrdersResponse>("/guest/cart/send", {
    method: "POST",
  })
}
