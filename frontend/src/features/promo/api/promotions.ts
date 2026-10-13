import { apiFetch } from "@/lib/api/client"

export interface GuestPromo {
  id: number
  title: string
  description: string
  imageUrl: string
  active: boolean
}

export type GuestPromoDto = Omit<GuestPromo, "id">

export function listPromos() {
  return apiFetch<GuestPromo[]>("/promos")
}

export function getActivePromo() {
  return apiFetch<GuestPromo | null>("/promos/active")
}

export function createPromo(dto: GuestPromoDto) {
  return apiFetch<GuestPromo>("/promos", {
    method: "POST",
    body: JSON.stringify(dto),
  })
}

export function updatePromo(id: number, dto: GuestPromoDto) {
  return apiFetch<GuestPromo>(`/promos/${id}`, {
    method: "PUT",
    body: JSON.stringify(dto),
  })
}

export function deletePromo(id: number) {
  return apiFetch<void>(`/promos/${id}`, { method: "DELETE" })
}
