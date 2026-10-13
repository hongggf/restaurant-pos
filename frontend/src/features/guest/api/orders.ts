import { guestApiFetch } from "@/lib/api/guest-client"
import type { GuestOrdersResponse } from "../types"

export function getGuestOrders() {
  return guestApiFetch<GuestOrdersResponse>("/guest/orders")
}
