import { useState } from "react"
import { Plus, ReceiptText, UtensilsCrossed, WifiOff } from "lucide-react"
import { useLocation, useNavigate, useParams } from "react-router"

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import {
  Empty,
  EmptyContent,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from "@/components/ui/empty"
import { Skeleton } from "@/components/ui/skeleton"
import { EditLineSelectionsDialog } from "@/features/orders/components/edit-line-selections-dialog"
import { ReasonDialog } from "@/features/sessions/components/reason-dialog"
import { TransferDialog } from "@/features/sessions/components/transfer-dialog"
import { RoundCard } from "@/features/sessions/components/round-card"
import {
  useCancelRound,
  useSessionRounds,
  useUpdateLineSelections,
  useVoidLine,
} from "@/features/sessions/hooks/use-session-rounds"
import type { CashierRound, RoundLine } from "@/features/sessions/types"
import { formatPrice } from "@/lib/format"
import { useIsOffline } from "@/hooks/use-offline"

export function SessionPage() {
  const { sessionId = "" } = useParams()
  const navigate = useNavigate()
  const location = useLocation() as { state?: { tableNumber?: string } }

  const { data, isPending, isLoadingError, isRefetchError, error, refetch } =
    useSessionRounds(sessionId)
  const cancelRound = useCancelRound(sessionId)
  const updateSelections = useUpdateLineSelections(sessionId)
  const voidLine = useVoidLine(sessionId)

  const [voiding, setVoiding] = useState<{
    round: CashierRound
    line: RoundLine
  } | null>(null)

  const [cancelling, setCancelling] = useState<CashierRound | null>(null)
  const isOffline = useIsOffline()
  const [transferring, setTransferring] = useState(false)
  const [editing, setEditing] = useState<{
    round: CashierRound
    line: RoundLine
  } | null>(null)

  const rounds = [...(data ?? [])].sort(
    (a, b) => (a.roundNumber ?? 0) - (b.roundNumber ?? 0)
  )
  const tableNumber =
    rounds[0]?.tableNumber ?? location.state?.tableNumber ?? null
  const total = rounds
    .filter((r) => r.fulfillmentStatus !== "CANCELLED")
    .reduce((sum, r) => sum + (r.grandTotal ?? 0), 0)

  return (
    <div className="flex h-full flex-col">
      {isOffline && (
        <Alert variant="destructive" className="mb-4 bg-destructive/10 shrink-0">
          <WifiOff className="size-4" />
          <AlertTitle>You are offline</AlertTitle>
          <AlertDescription>
            Showing cached data. New orders or updates may fail to save.
          </AlertDescription>
        </Alert>
      )}

      <div className="sticky top-0 z-10 bg-card pb-4 border-b shrink-0 flex items-start justify-between gap-4">
        <div>
          <h1 className="text-xl font-semibold tracking-tight">
            {tableNumber ? `Table ${tableNumber}` : "Session"}
          </h1>
          <p className="text-muted-foreground text-sm">
            {rounds.length === 1 ? "1 round" : `${rounds.length} rounds`}
            <Button variant="link" className="px-1 h-auto text-sm" onClick={() => setTransferring(true)}>
              (Move)
            </Button>
          </p>
          <p className="text-xl font-semibold tabular-nums mt-1">
            {formatPrice(total)}
          </p>
        </div>
        
        {rounds.length > 0 && (
          <div className="flex flex-col gap-2">
            <Button
              size="sm"
              onClick={() =>
                navigate(`/cashier/sessions/${sessionId}/order`, {
                  state: { tableNumber },
                })
              }
            >
              <Plus className="mr-2 size-4" />
              Add order
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() =>
                navigate(`/cashier/sessions/${sessionId}/bill`, {
                  state: { tableNumber },
                })
              }
            >
              <ReceiptText className="mr-2 size-4" />
              View bill
            </Button>
          </div>
        )}
      </div>

      {isRefetchError && (
        <Alert variant="destructive" className="mt-4 shrink-0">
          <AlertTitle>Connection lost</AlertTitle>
          <AlertDescription>Reconnecting to the server...</AlertDescription>
        </Alert>
      )}

      <div className="flex-1 overflow-y-auto pt-4 pb-10">
        {isPending ? (
          <div className="space-y-3">
            {Array.from({ length: 3 }, (_, i) => (
              <Skeleton key={i} className="h-32 w-full rounded-xl" />
            ))}
          </div>
        ) : isLoadingError ? (
          <Alert variant="destructive">
            <AlertTitle>Couldn&apos;t load this session</AlertTitle>
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
        ) : rounds.length === 0 ? (
          <Empty className="border border-dashed h-full">
            <EmptyHeader>
              <EmptyMedia variant="icon">
                <UtensilsCrossed />
              </EmptyMedia>
              <EmptyTitle>No orders yet</EmptyTitle>
              <EmptyDescription>
                Take an order here or guests can scan the QR code.
              </EmptyDescription>
            </EmptyHeader>
            <EmptyContent>
              <Button
                onClick={() =>
                  navigate(`/cashier/sessions/${sessionId}/order`, {
                    state: { tableNumber },
                  })
                }
              >
                <Plus className="mr-2 size-4" />
                Add order
              </Button>
            </EmptyContent>
          </Empty>
        ) : (
          <div className="space-y-3">
            {rounds.map((round) => (
              <RoundCard
                key={round.id}
                round={round}
                onCancel={setCancelling}
                onVoidLine={(r, line) => setVoiding({ round: r, line })}
                onEditLine={(r, line) => setEditing({ round: r, line })}
              />
            ))}
          </div>
        )}
      </div>

      <TransferDialog
        sessionId={sessionId}
        open={transferring}
        onOpenChange={setTransferring}
      />
      <ReasonDialog
        key={`void-${voiding?.line.id ?? "none"}`}
        open={!!voiding}
        onOpenChange={(open) => !open && setVoiding(null)}
        title="Void item?"
        description={`Are you sure you want to void ${voiding?.line.nameEn}?`}
        confirmLabel="Void item"
        pendingLabel="Voiding…"
        pending={voidLine.isPending}
        onConfirm={(reason) => {
          if (!voiding?.round.id || !voiding?.line.id) return
          voidLine.mutate(
            { roundId: voiding.round.id, lineId: voiding.line.id, reason },
            { onSuccess: () => setVoiding(null) }
          )
        }}
      />
      <ReasonDialog
        key={`cancel-${cancelling?.id ?? "none"}`}
        open={!!cancelling}
        onOpenChange={(open) => !open && setCancelling(null)}
        title={`Cancel round #${cancelling?.roundNumber}?`}
        description="Every item in this round will be removed from the bill. This can't be undone."
        confirmLabel="Cancel round"
        pendingLabel="Cancelling…"
        pending={cancelRound.isPending}
        onConfirm={(reason) => {
          if (!cancelling?.id) return
          cancelRound.mutate(
            { roundId: cancelling.id, reason },
            { onSuccess: () => setCancelling(null) }
          )
        }}
      />
      {editing && (
        <EditLineSelectionsDialog
          key={editing.line.id}
          line={editing.line}
          saving={updateSelections.isPending}
          onOpenChange={(open) => !open && setEditing(null)}
          onSave={(selections) => {
            if (!editing.round.id || !editing.line.id) return
            updateSelections.mutate(
              { roundId: editing.round.id, lineId: editing.line.id, selections },
              { onSuccess: () => setEditing(null) }
            )
          }}
        />
      )}
    </div>
  )
}
