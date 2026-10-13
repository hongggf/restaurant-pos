import { guestApiFetch } from "@/lib/api/guest-client"
import type { Category, GuestMenuItemDetail, MenuItemPage } from "../types"

export function listGuestCategories() {
  return guestApiFetch<Category[]>("/guest/menu/categories")
}

export interface GuestMenuItemListParams {
  page: number
  size: number
  categoryId?: string
}

export function listGuestMenuItems({
  page,
  size,
  categoryId,
}: GuestMenuItemListParams) {
  const query = new URLSearchParams()
  query.append("page", String(page))
  query.append("size", String(size))
  query.append("sort", "category.sortOrder,asc")
  query.append("sort", "nameEn,asc")
  if (categoryId) query.set("categoryId", categoryId)
  return guestApiFetch<MenuItemPage>(`/guest/menu/items?${query}`)
}

export function getGuestMenuItemDetail(id: string) {
  return guestApiFetch<GuestMenuItemDetail>(`/guest/menu/items/${id}`)
}
