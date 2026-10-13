import { apiFetch } from "@/lib/api/client"
import type {
  DiningTable,
  DiningTablePage,
  TableCreateRequest,
  TableUpdateRequest,
} from "../types"

export function listTables(params: { page: number; size: number }) {
  const query = new URLSearchParams({
    page: String(params.page),
    size: String(params.size),
    sort: "tableNumber,asc",
  })
  return apiFetch<DiningTablePage>(`/tables?${query}`)
}

export function createTable(body: TableCreateRequest) {
  return apiFetch<DiningTable>("/tables", {
    method: "POST",
    body: JSON.stringify(body),
  })
}

export function updateTable(id: string, body: TableUpdateRequest) {
  return apiFetch<DiningTable>(`/tables/${id}`, {
    method: "PUT",
    body: JSON.stringify(body),
  })
}

export function deleteTable(id: string) {
  return apiFetch<void>(`/tables/${id}`, { method: "DELETE" })
}

export function regenerateQrToken(id: string) {
  return apiFetch<DiningTable>(`/tables/${id}/qr-token`, { method: "POST" })
}
