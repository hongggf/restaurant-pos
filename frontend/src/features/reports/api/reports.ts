import { apiFetch } from "@/lib/api/client"
import type { OverviewStatsResponse } from "../types"

export function getOverviewStats() {
  return apiFetch<OverviewStatsResponse>("/reports/overview")
}
