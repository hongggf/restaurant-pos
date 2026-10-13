import { Spinner } from "@/components/ui/spinner"
import { Receipt } from "lucide-react"
import { Link } from "react-router"
import { formatPrice } from "@/lib/format"
import { cn } from "@/lib/utils"
import type { TableOverview } from "../types"

export type DerivedTableState = "IDLE" | "OCCUPIED" | "WAITING" | "READY_TO_SERVE" | "READY_TO_PAY"

const stateStyles: Record<DerivedTableState, { card: string; badge: string }> = {
  IDLE: {
    card: "border-slate-200 bg-slate-50 hover:border-slate-400 dark:border-slate-800 dark:bg-slate-900/50 dark:hover:border-slate-700",
    badge: "bg-slate-200 text-slate-600 dark:bg-slate-800 dark:text-slate-400",
  },
  OCCUPIED: {
    card: "border-indigo-300 bg-indigo-50 hover:border-indigo-500 dark:border-indigo-500/40 dark:bg-indigo-500/10 dark:hover:border-indigo-500",
    badge: "bg-indigo-600 text-white",
  },
  WAITING: {
    card: "border-amber-400 bg-amber-100 hover:border-amber-500 dark:border-amber-500/60 dark:bg-amber-500/20 dark:hover:border-amber-400",
    badge: "bg-amber-500 text-white",
  },
  READY_TO_SERVE: {
    card: "border-blue-300 bg-blue-50 hover:border-blue-500 dark:border-blue-500/40 dark:bg-blue-500/10 dark:hover:border-blue-500",
    badge: "bg-blue-600 text-white",
  },
  READY_TO_PAY: {
    card: "border-emerald-400 bg-emerald-100 hover:border-emerald-500 dark:border-emerald-500/60 dark:bg-emerald-500/20 dark:hover:border-emerald-400",
    badge: "bg-emerald-500 text-white",
  },
}

function getDerivedState(table: TableOverview): DerivedTableState {
  if (table.state === "IDLE" || !table.sessionId) return "IDLE"
  // Most-urgent-wins (backend rolls rounds up NEW > COOKING > READY):
  // amber while the kitchen is working, blue when food is at the pass.
  if (table.fulfillmentStatus === "NEW" || table.fulfillmentStatus === "COOKING") return "WAITING"
  if (table.fulfillmentStatus === "READY") return "READY_TO_SERVE"
  if (table.runningTotal && table.runningTotal > 0) return "READY_TO_PAY"
  return "OCCUPIED"
}

interface TableBoardCardProps {
  table: TableOverview
  busy?: boolean
  onClick: (table: TableOverview) => void
  selected?: boolean
}

export function TableBoardCard({ table, busy, onClick, selected }: TableBoardCardProps) {
  const state = getDerivedState(table)
  const styles = stateStyles[state]

  return (
    <div
      role="button"
      tabIndex={0}
      aria-disabled={busy}
      onClick={(e) => {
        // Prevent clicking the card if we clicked a child link/button
        if ((e.target as HTMLElement).closest('a, button')) return
        if (!busy) onClick(table)
      }}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault()
          if (!busy) onClick(table)
        }
      }}
      className={cn(
        "relative flex aspect-square cursor-pointer flex-col items-center justify-center gap-1 rounded-xl border-2 p-2 text-center transition-colors aria-disabled:pointer-events-none aria-disabled:opacity-60",
        styles.card,
        selected && "ring-2 ring-primary ring-offset-2 ring-offset-background"
      )}
    >
      {state === "IDLE" && table.lastClosedSessionHasReceipt && (
        <Link
          to={`/cashier/sessions/${table.lastClosedSessionId}/receipt`}
          className="absolute right-2 top-2 rounded-full bg-muted p-1.5 text-muted-foreground hover:bg-muted/80 hover:text-foreground"
          title="View last receipt"
          onClick={(e) => e.stopPropagation()}
        >
          <Receipt className="size-3.5" />
        </Link>
      )}
      <span className="text-2xl font-semibold tracking-tight">
        {table.tableNumber}
      </span>
      <span
        className={cn(
          "rounded-md px-1.5 py-0.5 text-[10px] font-medium",
          styles.badge
        )}
      >
        {state === "READY_TO_PAY" ? "READY" : state === "READY_TO_SERVE" ? "AT PASS" : state}
      </span>
      <span className="text-sm font-semibold tabular-nums">
        {state === "IDLE" ? "—" : formatPrice(table.runningTotal ?? 0)}
      </span>
      {busy ? (
        <Spinner className="size-4" />
      ) : state === "IDLE" ? (
        <span className="text-muted-foreground text-[10px]">Tap to open</span>
      ) : (
        <span className="text-muted-foreground text-[10px]">
          {table.fulfillmentStatus ? `Status: ${table.fulfillmentStatus}` : '—'}
        </span>
      )}
    </div>
  )
}
