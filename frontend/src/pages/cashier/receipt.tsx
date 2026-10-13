import { Check, LayoutGrid, Printer } from "lucide-react"
import { Link, useParams } from "react-router"

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Separator } from "@/components/ui/separator"
import { Skeleton } from "@/components/ui/skeleton"
import { Spinner } from "@/components/ui/spinner"
import { BillSummary } from "@/features/payments/components/bill-summary"
import {
  useOpenReceiptPdf,
  useSessionReceipt,
} from "@/features/receipts/hooks/use-receipt"
import { formatPrice } from "@/lib/format"

const dateTimeFormat = new Intl.DateTimeFormat(undefined, {
  dateStyle: "medium",
  timeStyle: "short",
})

export function ReceiptPage() {
  const { sessionId = "" } = useParams()
  const { data: receipt, isPending, isError, error, refetch } =
    useSessionReceipt(sessionId)
  const openPdf = useOpenReceiptPdf()

  const payment = receipt?.payment
  const hasChange =
    (payment?.changeUsd ?? 0) > 0 || (payment?.changeKhr ?? 0) > 0

  return (
    <div className="mx-auto w-full max-w-md space-y-4">
      {isPending ? (
        <Skeleton className="h-96 rounded-xl" />
      ) : isError ? (
        <Alert variant="destructive">
          <AlertTitle>Couldn&apos;t load the receipt</AlertTitle>
          <AlertDescription>
            <p>{error.message}</p>
            <Button
              variant="outline"
              size="sm"
              className="mt-2"
              onClick={() => refetch()}
            >
              Try again
            </Button>
          </AlertDescription>
        </Alert>
      ) : (
        <>
          <Card>
            <CardContent className="space-y-4 py-5">
              <div className="text-center">
                <div className="bg-emerald-500/15 text-emerald-700 dark:text-emerald-400 mx-auto mb-2 flex size-10 items-center justify-center rounded-full">
                  <Check className="size-5" />
                </div>
                <p className="text-lg font-semibold">
                  {receipt.restaurantName ?? "Receipt"}
                </p>
                <p className="text-muted-foreground text-sm">
                  Receipt {receipt.receiptNumber}
                  {receipt.bill?.tableNumber
                    ? ` · Table ${receipt.bill.tableNumber}`
                    : ""}
                </p>
                {receipt.closedAt && (
                  <p className="text-muted-foreground text-xs">
                    {dateTimeFormat.format(new Date(receipt.closedAt))}
                  </p>
                )}
              </div>

              {receipt.bill && <BillSummary bill={receipt.bill} />}

              <Separator />
              <div className="space-y-1 text-sm">
                <div className="text-muted-foreground flex justify-between">
                  <span>Paid by</span>
                  <span>
                    {payment?.method === "QR" ? "QR" : "Cash"}
                    {payment?.paidBy ? ` · ${payment.paidBy}` : ""}
                  </span>
                </div>
                {payment?.method === "CASH" && (
                  <div className="text-muted-foreground flex justify-between">
                    <span>Received</span>
                    <span className="tabular-nums">
                      {formatPrice(
                        payment.amountTendered,
                        payment.tenderedCurrency
                      )}
                    </span>
                  </div>
                )}
                {payment?.referenceNote && (
                  <div className="text-muted-foreground flex justify-between gap-4">
                    <span>Reference</span>
                    <span className="truncate">{payment.referenceNote}</span>
                  </div>
                )}
              </div>
            </CardContent>
          </Card>

          {hasChange && (
            <Card className="border-emerald-500/50 bg-emerald-500/5">
              <CardContent className="py-4 text-center">
                <p className="text-muted-foreground text-sm">Change due</p>
                <p className="text-3xl font-bold tabular-nums">
                  {formatPrice(payment?.changeUsd ?? 0)}
                </p>
                {(payment?.changeKhr ?? 0) > 0 && (
                  <p className="text-muted-foreground tabular-nums">
                    or {formatPrice(payment?.changeKhr ?? 0, "KHR")}
                  </p>
                )}
              </CardContent>
            </Card>
          )}

          <div className="flex gap-3">
            <Button
              variant="outline"
              className="flex-1"
              disabled={openPdf.isPending || !receipt.receiptId}
              onClick={() =>
                receipt.receiptId && openPdf.mutate(receipt.receiptId)
              }
            >
              {openPdf.isPending ? <Spinner /> : <Printer />}
              Print / PDF
            </Button>
            <Button className="flex-1" asChild>
              <Link to="/cashier">
                <LayoutGrid />
                Back to tables
              </Link>
            </Button>
          </div>
        </>
      )}
    </div>
  )
}
