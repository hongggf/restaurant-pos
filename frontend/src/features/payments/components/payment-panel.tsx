import { useState } from "react"
import { Banknote, QrCode } from "lucide-react"

import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { formatPrice } from "@/lib/format"
import { cn } from "@/lib/utils"
import { usePay } from "../hooks/use-payment"
import type { Bill, Payment, PaymentMethod } from "../types"

interface PaymentPanelProps {
  sessionId: string
  bill: Bill
  onPaid: (payment: Payment) => void
}

export function PaymentPanel({ sessionId, bill, onPaid }: PaymentPanelProps) {
  const pay = usePay(sessionId)

  const [method, setMethod] = useState<PaymentMethod>("CASH")
  const [currency, setCurrency] = useState<"USD" | "KHR">("USD")
  const [tendered, setTendered] = useState("")
  const [referenceNote, setReferenceNote] = useState("")
  const [confirming, setConfirming] = useState(false)

  const totalUsd = bill.grandTotal ?? 0
  const totalDue = currency === "USD" ? totalUsd : (bill.grandTotalKhr ?? 0)
  const tenderedNumber = Number.parseFloat(tendered)
  const cashValid =
    Number.isFinite(tenderedNumber) && tenderedNumber >= totalDue
  const changePreview = cashValid ? tenderedNumber - totalDue : null
  const ready = method === "QR" || cashValid

  function confirmPayment() {
    if (pay.isPending) return
    pay.mutate(
      method === "CASH"
        ? { method, amountTendered: tenderedNumber, currency }
        : { method, referenceNote: referenceNote.trim() || undefined },
      {
        onSuccess: (payment) => {
          setConfirming(false)
          onPaid(payment)
        },
        onError: () => setConfirming(false),
      }
    )
  }

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 gap-2">
        {(
          [
            { value: "CASH", label: "Cash", icon: Banknote },
            { value: "QR", label: "QR", icon: QrCode },
          ] as const
        ).map((option) => (
          <button
            key={option.value}
            type="button"
            onClick={() => setMethod(option.value)}
            className={cn(
              "flex flex-col items-center gap-1 rounded-xl border-2 p-3 text-sm font-medium transition-colors",
              method === option.value
                ? "border-primary bg-primary/5"
                : "border-border hover:border-primary/40"
            )}
          >
            <option.icon className="size-5" />
            {option.label}
          </button>
        ))}
      </div>

      {method === "CASH" ? (
        <div className="space-y-3">
          <div className="grid grid-cols-[1fr_auto] gap-2">
            <div className="space-y-1.5">
              <Label htmlFor="tendered">Amount received</Label>
              <Input
                id="tendered"
                type="number"
                inputMode="decimal"
                min={0}
                placeholder={String(totalDue)}
                value={tendered}
                onChange={(event) => setTendered(event.target.value)}
              />
            </div>
            <div className="space-y-1.5">
              <Label>Currency</Label>
              <Select
                value={currency}
                onValueChange={(value) => {
                  setCurrency(value as "USD" | "KHR")
                  setTendered("")
                }}
              >
                <SelectTrigger className="w-24">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="USD">USD</SelectItem>
                  <SelectItem value="KHR">KHR</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={() => setTendered(String(totalDue))}
          >
            Exact — {formatPrice(totalDue, currency)}
          </Button>
          {changePreview != null && changePreview > 0 && (
            <p className="text-sm">
              Change due:{" "}
              <span className="font-semibold tabular-nums">
                {formatPrice(changePreview, currency)}
              </span>
            </p>
          )}
          {tendered !== "" && !cashValid && (
            <p className="text-destructive text-sm">
              Received amount is less than the total (
              {formatPrice(totalDue, currency)}).
            </p>
          )}
        </div>
      ) : (
        <div className="space-y-1.5">
          <Label htmlFor="reference">Reference note (optional)</Label>
          <Input
            id="reference"
            maxLength={200}
            placeholder="e.g. Bakong transaction ref…"
            value={referenceNote}
            onChange={(event) => setReferenceNote(event.target.value)}
          />
        </div>
      )}

      <Button
        className="w-full"
        size="lg"
        disabled={!ready || pay.isPending}
        onClick={() => setConfirming(true)}
      >
        Take payment · {formatPrice(totalUsd, bill.currencyCode)}
      </Button>

      <AlertDialog
        open={confirming}
        onOpenChange={(open) => !pay.isPending && setConfirming(open)}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>
              Confirm {method === "CASH" ? "cash" : "QR"} payment?
            </AlertDialogTitle>
            <AlertDialogDescription>
              {method === "CASH"
                ? `Bill of ${formatPrice(totalUsd, bill.currencyCode)}, received ${formatPrice(
                    tenderedNumber,
                    currency
                  )}. `
                : `Bill of ${formatPrice(totalUsd, bill.currencyCode)} paid by QR. `}
              This closes the session and frees table {bill.tableNumber}.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={pay.isPending}>
              Go back
            </AlertDialogCancel>
            <AlertDialogAction
              disabled={pay.isPending}
              onClick={(event) => {
                event.preventDefault()
                confirmPayment()
              }}
            >
              {pay.isPending ? "Recording…" : "Confirm payment"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
