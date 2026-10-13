import { apiFetch } from "@/lib/api/client"
import type { StaffSession, TableOverview } from "../types"

export function getTablesOverview() {
  return apiFetch<TableOverview[]>("/tables/overview")
}

export function openSession(tableId: string) {
  return apiFetch<StaffSession>(`/tables/${tableId}/session`, {
    method: "POST",
  })
}
