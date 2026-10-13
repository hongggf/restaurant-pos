import { Fragment } from "react"

import { Separator } from "@/components/ui/separator"
import { formatPrice } from "@/lib/format"
import { cn } from "@/lib/utils"
import type { Bill } from "../types"
import { ModifierBreakdown } from "@/features/sessions/components/modifier-breakdown"

/** Itemized bill: every non-cancelled round's lines, then session totals. */
export function BillSummary({ bill }: { bill: Bill }) {
  const rounds = (bill.rounds ?? []).filter((r) => r.fulfillmentStatus !== "CANCELLED")

  return (
    <div className="space-y-3">
      {rounds.map((round) => (
        <Fragment key={round.id}>
          <div className="space-y-1.5">
            <p className="text-muted-foreground text-xs font-medium">
              Round #{round.roundNumber}
            </p>
            {round.lines?.map((line) => (
              <div key={line.id} className="space-y-1">
                <div
                  className={cn(
                    "flex items-baseline justify-between gap-2 text-sm",
                    line.voided && "text-muted-foreground line-through"
                  )}
                >
                  <span className="min-w-0">
                    {line.quantity}× {line.nameEn}
                  </span>
                  <span className="tabular-nums">
                    {formatPrice(line.lineTotal)}
                  </span>
                </div>
                <ModifierBreakdown
                  menuItemId={line.menuItemId}
                  selections={line.selections}
                />
              </div>
            ))}
          </div>
        </Fragment>
      ))}

      <Separator />
      <div className="space-y-1 text-sm">
        <div className="text-muted-foreground flex justify-between">
          <span>Subtotal</span>
          <span className="tabular-nums">{formatPrice(bill.subtotal)}</span>
        </div>
        <div className="text-muted-foreground flex justify-between">
          <span>VAT</span>
          <span className="tabular-nums">{formatPrice(bill.vatAmount)}</span>
        </div>
        <div className="flex justify-between text-base font-semibold">
          <span>Total</span>
          <span className="tabular-nums">
            {formatPrice(bill.grandTotal, bill.currencyCode)}
          </span>
        </div>
        {bill.grandTotalKhr != null && (
          <div className="text-muted-foreground flex justify-between">
            <span>Total in Riel</span>
            <span className="tabular-nums">
              {formatPrice(bill.grandTotalKhr, "KHR")}
            </span>
          </div>
        )}
      </div>
    </div>
  )
}
