import { useState } from "react"
import { CheckCheck, Ban, Flame } from "lucide-react"
import { useUpdateMenuItemAvailability } from "@/features/menu/hooks/use-menu-items"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardFooter, CardHeader } from "@/components/ui/card"
import { Spinner } from "@/components/ui/spinner"
import type { CashierRound, RoundLine } from "@/features/sessions/types"
import { cn } from "@/lib/utils"
import {
  URGENT_AFTER_MIN,
  WARN_AFTER_MIN,
  minutesWaiting,
} from "../lib/ticket-age"

function TicketLine({ line, onToggleDone }: { line: RoundLine; onToggleDone?: () => void }) {
  const updateAvailability = useUpdateMenuItemAvailability()
  const [done, setDone] = useState(false)
  
  const isDone = done || line.voided
  
  const handleToggle = () => {
    if (line.voided) return
    setDone(!done)
    if (onToggleDone) onToggleDone()
  }

  return (
    <li 
      className={cn(
        "group relative flex flex-col p-2 rounded-lg cursor-pointer transition-colors hover:bg-muted/50 border border-transparent",
        isDone && "opacity-50 grayscale",
        done && "bg-muted border-muted"
      )}
      onClick={handleToggle}
    >
      <div className="flex items-start justify-between gap-2">
        <div className="flex items-baseline gap-2">
          <span className="text-xl font-bold tabular-nums leading-none">{line.quantity}×</span>
          <span
            className={cn(
              "text-lg leading-none font-semibold",
              isDone && "line-through text-muted-foreground"
            )}
          >
            {line.nameEn}
          </span>
        </div>
        {!line.voided && (
          <Button
            variant="ghost"
            size="icon"
            className="h-8 w-8 text-destructive hover:text-destructive hover:bg-destructive/10 z-10 shrink-0"
            onClick={(e) => {
              e.stopPropagation()
              updateAvailability.mutate({ id: line.menuItemId!, available: false })
            }}
            disabled={updateAvailability.isPending}
            title="86 / Sold out"
          >
            <Ban className="size-4" />
          </Button>
        )}
      </div>
      {line.nameKm && (
        <p className={cn("text-muted-foreground pl-7 text-sm", isDone && "line-through")}>{line.nameKm}</p>
      )}
      {line.selections && line.selections.length > 0 && (
        <ul className={cn("text-muted-foreground pl-7 text-sm", isDone && "line-through")}>
          {line.selections.map((selection) => (
            <li key={selection.modifierOptionId ?? selection.nameEn}>
              •{" "}
              {(selection.quantity ?? 1) > 1
                ? `${selection.nameEn} ×${selection.quantity}`
                : selection.nameEn}
            </li>
          ))}
        </ul>
      )}
      {line.remark && (
        <p className="mt-1 ml-7 rounded bg-amber-500/15 px-2 py-1 text-sm font-medium text-amber-800 dark:text-amber-300">
          {line.remark}
        </p>
      )}
      {line.voided && (
        <p className="text-destructive pl-7 text-sm font-medium mt-1">
          VOIDED{line.voidReason ? ` — ${line.voidReason}` : ""} — do not cook
        </p>
      )}
    </li>
  )
}

interface KitchenTicketProps {
  round: CashierRound
  now: number
  column: "NEW" | "COOKING" | "READY"
  marking: boolean
  onStartCooking?: (round: CashierRound) => void
  onMarkReady?: (round: CashierRound) => void
  onBump?: (round: CashierRound) => void
}

export function KitchenTicket({
  round,
  now,
  column,
  marking,
  onStartCooking,
  onMarkReady,
  onBump,
}: KitchenTicketProps) {
  const waited = minutesWaiting(round.sentAt, now)
  const urgent = waited >= URGENT_AFTER_MIN
  const warn = !urgent && waited >= WARN_AFTER_MIN

  return (
    <Card
      className={cn(
        "gap-2 py-3 flex flex-col shrink-0 animate-in fade-in slide-in-from-bottom-4 duration-500 w-full md:w-80 shadow-md",
        warn && "border-amber-500",
        urgent && "border-destructive border-2"
      )}
    >
      <CardHeader className="flex flex-row items-center justify-between gap-2 px-4 py-1 pb-2">
        <div>
          <p className="text-3xl leading-none font-bold">
            Table {round.tableNumber ?? "—"}
          </p>
          <p className="text-muted-foreground text-sm mt-1">
            Round #{round.roundNumber}
          </p>
        </div>
        <div
          className={cn(
            "rounded-md px-3 py-1.5 text-2xl font-bold tabular-nums text-center min-w-[70px]",
            !warn && !urgent && "bg-muted text-muted-foreground",
            warn && "bg-amber-500 text-amber-50",
            urgent && "bg-destructive text-destructive-foreground animate-pulse"
          )}
        >
          {waited}m
        </div>
      </CardHeader>
      
      <CardContent className="px-2 flex-1 overflow-y-auto">
        <ul className="space-y-1">
          {round.lines?.map((line) => (
            <TicketLine key={line.id} line={line} />
          ))}
        </ul>
      </CardContent>
      
      <CardFooter className="px-4 pt-2">
        {column === "NEW" && onStartCooking && (
          <Button
            size="lg"
            className="h-14 w-full text-lg font-semibold bg-indigo-600 hover:bg-indigo-700 text-white"
            onClick={() => onStartCooking(round)}
          >
            <Flame className="mr-2 size-5" />
            Start Cooking
          </Button>
        )}
        
        {column === "COOKING" && onMarkReady && (
          <Button
            size="lg"
            className="h-14 w-full text-lg font-semibold bg-emerald-600 hover:bg-emerald-700 text-white"
            disabled={marking}
            onClick={() => onMarkReady(round)}
          >
            {marking ? <Spinner className="mr-2 size-5" /> : <CheckCheck className="mr-2 size-5" />}
            Mark Ready
          </Button>
        )}
        
        {column === "READY" && onBump ? (
          <Button
            size="lg"
            className="h-14 w-full text-lg font-semibold bg-gray-600 hover:bg-gray-700 text-white"
            onClick={() => onBump(round)}
          >
            <CheckCheck className="mr-2 size-5" />
            Bump
          </Button>
        ) : column === "READY" ? (
          <div className="w-full text-center py-2 rounded bg-emerald-500/10 text-emerald-700 font-semibold border border-emerald-500/20">
            Ready to Serve
          </div>
        ) : null}
      </CardFooter>
    </Card>
  )
}
