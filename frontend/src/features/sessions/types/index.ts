import type { components } from "@/lib/api/schema"

export type TableOverview = components["schemas"]["TableOverviewResponse"]
export type TableState = NonNullable<TableOverview["state"]>
export type StaffSession = components["schemas"]["StaffSessionResponse"]
export type CashierRound = components["schemas"]["CashierRoundResponse"]
export type RoundLine = components["schemas"]["OrderRoundLineResponse"]
export type RoundStatus = "SENT" | "READY" | "COMPLETED" | "CANCELLED"
