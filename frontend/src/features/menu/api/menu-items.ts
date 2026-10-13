import { apiFetch } from "@/lib/api/client"
import type {
  Currency,
  MenuItem,
  MenuItemCreateRequest,
  MenuItemPage,
  MenuItemUpdateRequest,
} from "../types"

export interface MenuItemListParams {
  page: number
  size: number
  categoryId?: string
  available?: boolean
}

export function listMenuItems({
  page,
  size,
  categoryId,
  available,
}: MenuItemListParams) {
  const query = new URLSearchParams()
  query.append("page", String(page))
  query.append("size", String(size))
  query.append("sort", "category.sortOrder,asc")
  query.append("sort", "nameEn,asc")
  if (categoryId) query.set("categoryId", categoryId)
  if (available !== undefined) query.set("available", String(available))
  return apiFetch<MenuItemPage>(`/menu-items?${query}`)
}

export function createMenuItem(body: MenuItemCreateRequest) {
  return apiFetch<MenuItem>("/menu-items", {
    method: "POST",
    body: JSON.stringify(body),
  })
}

export function updateMenuItem(id: string, body: MenuItemUpdateRequest) {
  return apiFetch<MenuItem>(`/menu-items/${id}`, {
    method: "PUT",
    body: JSON.stringify(body),
  })
}

export function deleteMenuItem(id: string) {
  return apiFetch<void>(`/menu-items/${id}`, { method: "DELETE" })
}

export function uploadMenuItemImage(id: string, file: File) {
  const form = new FormData()
  form.append("file", file)
  return apiFetch<MenuItem>(`/menu-items/${id}/image`, {
    method: "POST",
    body: form,
  })
}

export function listCurrencies() {
  return apiFetch<Currency[]>("/currencies")
}

export function updateMenuItemAvailability(id: string, available: boolean) {
  return apiFetch<MenuItem>(`/menu-items/${id}/availability?available=${available}`, {
    method: "PATCH",
  })
}
