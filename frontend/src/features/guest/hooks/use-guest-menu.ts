import { keepPreviousData, useQuery } from "@tanstack/react-query"

import {
  getGuestMenuItemDetail,
  listGuestCategories,
  listGuestMenuItems,
  type GuestMenuItemListParams,
} from "../api/menu"

export const PAGE_SIZE = 12

export function useGuestCategories() {
  return useQuery({
    queryKey: ["guest", "categories"],
    queryFn: listGuestCategories,
  })
}

export function useGuestMenuItems(params: Omit<GuestMenuItemListParams, "size">) {
  return useQuery({
    queryKey: ["guest", "menu-items", "list", params],
    queryFn: () => listGuestMenuItems({ ...params, size: PAGE_SIZE }),
    placeholderData: keepPreviousData,
  })
}

export function useGuestMenuItemDetail(id: string | undefined) {
  return useQuery({
    queryKey: ["guest", "menu-items", id, "detail"],
    queryFn: () => getGuestMenuItemDetail(id!),
    enabled: !!id,
  })
}
