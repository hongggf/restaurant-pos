import { useQuery } from "@tanstack/react-query"

import { getGuestOrders } from "../api/orders"

// Other devices at the table can send rounds too — keep this live.
const ORDERS_REFETCH_MS = 5000

export function useGuestOrders() {
  return useQuery({
    queryKey: ["guest", "orders"],
    queryFn: getGuestOrders,
    refetchInterval: ORDERS_REFETCH_MS,
  })
}
