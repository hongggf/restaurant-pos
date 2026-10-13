import { Card, CardContent, CardFooter, CardHeader } from "@/components/ui/card"
import { Separator } from "@/components/ui/separator"
import { formatPrice } from "@/lib/format"
import { useLanguage } from "@/lib/language-context"
import { cn } from "@/lib/utils"
import type { OrderRound } from "../types"

const statusBadges: Record<string, string> = {
  SENT: "bg-amber-500/15 text-amber-700 dark:text-amber-400",
  READY: "bg-sky-500/15 text-sky-700 dark:text-sky-400",
  COMPLETED: "bg-emerald-500/15 text-emerald-700 dark:text-emerald-400",
  CANCELLED: "bg-destructive/10 text-destructive",
}

const timeFormat = new Intl.DateTimeFormat(undefined, {
  hour: "2-digit",
  minute: "2-digit",
})

export function GuestRoundCard({ round }: { round: OrderRound }) {
  const { language, t } = useLanguage()
  const isKhmer = language === "km"
  const status = round.fulfillmentStatus === "CANCELLED" ? "CANCELLED" : round.paymentStatus === "PAID" ? "COMPLETED" : round.fulfillmentStatus === "READY" ? "READY" : "SENT"
  const cancelled = status === "CANCELLED"
  const statusLabel = isKhmer
    ? status === "SENT"
      ? t("statusSent")
      : status === "READY"
        ? t("statusReady")
        : status === "COMPLETED"
          ? t("statusCompleted")
          : status === "CANCELLED"
            ? t("statusCancelled")
            : status
    : status

  return (
    <Card className={cn("gap-3 py-4", cancelled && "opacity-70")}>
      <CardHeader className="flex flex-row items-center justify-between gap-2 px-4">
        <div className="flex items-center gap-2">
          <span className="font-semibold">
            {isKhmer ? `${t("round")} #${round.roundNumber}` : `Round #${round.roundNumber}`}
          </span>
          {round.sentAt && (
            <span className="text-muted-foreground text-xs font-normal">
              · {timeFormat.format(new Date(round.sentAt))}
            </span>
          )}
        </div>
        <span className={cn("rounded-md px-2 py-0.5 text-xs font-medium", statusBadges[status])}>
          {statusLabel}
        </span>
      </CardHeader>
      <CardContent className="px-4">
        <ul className="space-y-3">
          {round.lines?.map((line) => (
            <li key={line.id} className="flex items-start gap-2">
              <div className={cn("min-w-0 flex-1", line.voided && "text-muted-foreground line-through")}>
                <p className="text-sm font-medium">
                  {line.quantity}× {isKhmer ? (line.nameKm || line.nameEn) : line.nameEn}
                </p>
                {line.selections && line.selections.length > 0 && (
                  <ul className="text-muted-foreground mt-1 ml-1 space-y-1 border-l-2 pl-3 text-xs">
                    {line.selections.map((s) => (
                      <li key={s.modifierOptionId}>
                        {(s.quantity ?? 1) > 1 ? `${s.quantity}× ` : ""}
                        {isKhmer ? (s.nameKm || s.nameEn) : s.nameEn}
                      </li>
                    ))}
                  </ul>
                )}
                {line.remark && (
                  <p className="text-muted-foreground mt-1 text-xs italic">“{line.remark}”</p>
                )}
                {line.voided && line.voidReason && (
                  <p className="text-destructive mt-1 text-xs no-underline">Voided: {line.voidReason}</p>
                )}
              </div>
              <span
                className={cn(
                  "text-sm tabular-nums",
                  line.voided && "text-muted-foreground line-through"
                )}
              >
                {formatPrice(line.lineTotal)}
              </span>
            </li>
          ))}
        </ul>
      </CardContent>
      {!cancelled && (
        <>
          <Separator />
          <CardFooter className="flex flex-col items-end gap-1 px-4 text-xs tabular-nums">
            <div className="text-muted-foreground flex w-full max-w-[200px] justify-between">
              <span>Subtotal</span>
              <span>{formatPrice(round.subtotal)}</span>
            </div>
            <div className="text-muted-foreground flex w-full max-w-[200px] justify-between">
              <span>VAT</span>
              <span>{formatPrice(round.vatAmount)}</span>
            </div>
            <div className="text-foreground mt-1 flex w-full max-w-[200px] justify-between text-sm font-medium">
              <span>Total</span>
              <span>{formatPrice(round.grandTotal)}</span>
            </div>
          </CardFooter>
        </>
      )}
    </Card>
  )
}
