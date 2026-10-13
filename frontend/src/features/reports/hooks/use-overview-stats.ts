import { useQuery } from "@tanstack/react-query"
import { getOverviewStats } from "../api/reports"

export function useOverviewStats() {
  return useQuery({
    queryKey: ["reports", "overview"],
    queryFn: getOverviewStats,
    refetchInterval: 30000, // Refresh every 30 seconds
  })
}
