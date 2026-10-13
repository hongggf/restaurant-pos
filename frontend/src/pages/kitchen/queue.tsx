import { useEffect, useState, useRef } from "react"
import { CookingPot } from "lucide-react"

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { KitchenTicket } from "@/features/kitchen/components/kitchen-ticket"
import { minutesWaiting } from "@/features/kitchen/lib/ticket-age"
import {
  useKitchenQueue,
  useStartCookingKitchenRound,
  useMarkKitchenRoundReady,
  useBumpKitchenRound
} from "@/features/kitchen/hooks/use-kitchen-queue"
import type { CashierRound } from "@/features/sessions/types"

// Ticket age is derived from a clock, not from the data, so refetching alone
// would leave "12m" frozen when the queue itself hasn't changed.
const CLOCK_TICK_MS = 30_000

// The ready strip is reference information, not the working queue, so it polls
// lazily. Marking a ticket ready invalidates both lists, so it still updates
// immediately on the action that matters.
const READY_REFETCH_MS = 20_000

let audioCtx: AudioContext | null = null

function playBeep() {
  try {
    if (!audioCtx) {
      audioCtx = new (window.AudioContext || (window as any).webkitAudioContext)()
    }
    if (audioCtx.state === "suspended") {
      audioCtx.resume()
    }
    const osc = audioCtx.createOscillator()
    const gain = audioCtx.createGain()
    osc.connect(gain)
    gain.connect(audioCtx.destination)
    osc.type = "sine"
    osc.frequency.setValueAtTime(880, audioCtx.currentTime)
    gain.gain.setValueAtTime(0.1, audioCtx.currentTime)
    osc.start()
    osc.stop(audioCtx.currentTime + 0.2)
  } catch (e) {
    // ignore
  }
}

function useNow() {
  const [now, setNow] = useState(() => Date.now())
  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), CLOCK_TICK_MS)
    return () => clearInterval(id)
  }, [])
  return now
}

export function KitchenQueuePage() {
  const now = useNow()
  const { data, isPending, isLoadingError, isRefetchError, error, refetch } = useKitchenQueue("SENT,COOKING")
  const { data: ready } = useKitchenQueue("READY", READY_REFETCH_MS)
  const startCooking = useStartCookingKitchenRound()
  const markReady = useMarkKitchenRoundReady()
  const bump = useBumpKitchenRound()

  const rounds = data ?? []
  const readyRounds = ready ?? []
  const oldest = rounds.reduce(
    (max, round) => Math.max(max, minutesWaiting(round.sentAt, now)),
    0
  )

  const seenIds = useRef<Set<string>>(new Set())
  useEffect(() => {
    if (!data) return
    let hasNew = false
    const currentIds = new Set<string>()
    for (const r of data) {
      if (r.id) {
        currentIds.add(r.id)
        if (!seenIds.current.has(r.id)) {
          hasNew = true
        }
      }
    }
    if (hasNew && seenIds.current.size > 0) {
      playBeep()
    }
    seenIds.current = currentIds
  }, [data])

  function handleStartCooking(round: CashierRound) {
    if (round.id) startCooking.mutate(round.id)
  }

  function handleMarkReady(round: CashierRound) {
    if (round.id) markReady.mutate(round.id)
  }

  function handleBump(round: CashierRound) {
    if (round.id) bump.mutate(round.id)
  }

  const newRounds = rounds.filter(r => r.fulfillmentStatus === 'NEW')
  const cookingRounds = rounds.filter(r => r.fulfillmentStatus === 'COOKING')

  return (
    <div className="flex h-full flex-col overflow-hidden">
      <div className="flex flex-wrap items-baseline justify-between gap-2 shrink-0 mb-4 px-2">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Kitchen Production Board</h1>
          <p className="text-muted-foreground text-sm">
            Oldest first. Mark items when done, then mark round ready.
          </p>
        </div>
        {rounds.length > 0 && (
          <p className="text-muted-foreground text-sm tabular-nums font-medium">
            {rounds.length === 1 ? "1 ticket" : `${rounds.length} tickets`} ·
            oldest {oldest}m
            {readyRounds.length > 0 && ` · ${readyRounds.length} ready`}
          </p>
        )}
      </div>

      {isRefetchError && (
        <Alert variant="destructive" className="mb-4 shrink-0">
          <AlertTitle>Connection lost</AlertTitle>
          <AlertDescription>Reconnecting to the server...</AlertDescription>
        </Alert>
      )}

      {isPending ? (
        <div className="flex flex-1 gap-6 overflow-hidden p-2">
          {Array.from({ length: 3 }, (_, i) => (
            <div key={i} className="flex-1 min-w-[300px] space-y-4">
              <Skeleton className="h-8 w-1/3" />
              <Skeleton className="h-64 rounded-xl w-full" />
            </div>
          ))}
        </div>
      ) : isLoadingError ? (
        <Alert variant="destructive" className="shrink-0">
          <AlertTitle>Couldn&apos;t load the cook queue</AlertTitle>
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
        <div className="flex flex-1 gap-6 overflow-x-auto pb-4 pt-2 px-2 snap-x">
          {/* New / Prep Column */}
          <div className="flex flex-col min-w-[320px] max-w-[400px] flex-shrink-0 snap-start bg-muted/30 rounded-xl p-4">
            <div className="flex items-center justify-between mb-4 shrink-0">
              <h2 className="text-lg font-bold">New / Prep</h2>
              <span className="bg-muted px-2 py-1 rounded-full text-xs font-semibold tabular-nums">{newRounds.length}</span>
            </div>
            
            <div className="flex-1 overflow-y-auto space-y-4 pb-12 pr-2 scrollbar-thin">
              {newRounds.length === 0 ? (
                <div className="flex flex-col items-center justify-center h-40 text-muted-foreground border-2 border-dashed rounded-xl">
                  <CookingPot className="size-8 mb-2 opacity-50" />
                  <p className="text-sm">No new tickets</p>
                </div>
              ) : (
                newRounds.map((round) => (
                  <KitchenTicket
                    key={round.id}
                    round={round}
                    now={now}
                    column="NEW"
                    marking={false}
                    onStartCooking={handleStartCooking}
                  />
                ))
              )}
            </div>
          </div>

          {/* Cooking Column */}
          <div className="flex flex-col min-w-[320px] max-w-[400px] flex-shrink-0 snap-start bg-muted/30 rounded-xl p-4">
            <div className="flex items-center justify-between mb-4 shrink-0">
              <h2 className="text-lg font-bold">Cooking</h2>
              <span className="bg-indigo-500/10 text-indigo-700 dark:text-indigo-400 px-2 py-1 rounded-full text-xs font-semibold tabular-nums">{cookingRounds.length}</span>
            </div>
            
            <div className="flex-1 overflow-y-auto space-y-4 pb-12 pr-2 scrollbar-thin">
              {cookingRounds.length === 0 ? (
                <div className="flex flex-col items-center justify-center h-40 text-muted-foreground border-2 border-dashed rounded-xl">
                  <p className="text-sm">Nothing is cooking</p>
                </div>
              ) : (
                cookingRounds.map((round) => (
                  <KitchenTicket
                    key={round.id}
                    round={round}
                    now={now}
                    column="COOKING"
                    marking={markReady.isPending && markReady.variables === round.id}
                    onMarkReady={handleMarkReady}
                  />
                ))
              )}
            </div>
          </div>

          {/* Ready / Expedite Column */}
          <div className="flex flex-col min-w-[320px] max-w-[400px] flex-shrink-0 snap-start bg-emerald-500/5 border border-emerald-500/10 rounded-xl p-4">
            <div className="flex items-center justify-between mb-4 shrink-0">
              <h2 className="text-lg font-bold text-emerald-800 dark:text-emerald-400">Ready / Expedite</h2>
              <span className="bg-emerald-500/20 text-emerald-800 dark:text-emerald-400 px-2 py-1 rounded-full text-xs font-semibold tabular-nums">{readyRounds.length}</span>
            </div>
            
            <div className="flex-1 overflow-y-auto space-y-4 pb-12 pr-2 scrollbar-thin">
              {readyRounds.length === 0 ? (
                <div className="flex flex-col items-center justify-center h-40 text-emerald-700/50 border-2 border-emerald-500/20 border-dashed rounded-xl">
                  <p className="text-sm">No tickets ready</p>
                </div>
              ) : (
                readyRounds.map((round) => (
                  <KitchenTicket
                    key={round.id}
                    round={round}
                    now={now}
                    column="READY"
                    marking={false}
                    onBump={handleBump}
                  />
                ))
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
