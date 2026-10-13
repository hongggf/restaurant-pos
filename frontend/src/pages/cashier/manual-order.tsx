import { useState } from "react"
import { ArrowLeft } from "lucide-react"
import { Link, useLocation, useNavigate, useParams } from "react-router"

import { Button } from "@/components/ui/button"
import { ItemConfigDialog } from "@/features/orders/components/item-config-dialog"
import { MenuBrowser } from "@/features/orders/components/menu-browser"
import { RoundPanel } from "@/features/orders/components/round-panel"
import { useSubmitRound } from "@/features/orders/hooks/use-manual-order"
import { lineSignature, toRoundRequest } from "@/features/orders/lib/draft"
import type { DraftLine } from "@/features/orders/types"
import type { MenuItem } from "@/features/menu/types"

export function ManualOrderPage() {
  const { sessionId = "" } = useParams()
  const navigate = useNavigate()
  const location = useLocation() as { state?: { tableNumber?: string } }
  const tableNumber = location.state?.tableNumber

  const submit = useSubmitRound(sessionId)
  const [lines, setLines] = useState<DraftLine[]>([])
  const [picked, setPicked] = useState<MenuItem | null>(null)

  function addLine(line: Omit<DraftLine, "key">) {
    const signature = lineSignature(line)
    setLines((prev) => {
      const existing = prev.find((l) => lineSignature(l) === signature)
      if (existing) {
        return prev.map((l) =>
          l === existing ? { ...l, quantity: l.quantity + line.quantity } : l
        )
      }
      return [...prev, { ...line, key: crypto.randomUUID() }]
    })
  }

  function changeQuantity(key: string, delta: number) {
    setLines((prev) =>
      prev.map((l) =>
        l.key === key ? { ...l, quantity: Math.max(1, l.quantity + delta) } : l
      )
    )
  }

  function handleSubmit() {
    if (submit.isPending) return
    submit.mutate(toRoundRequest(lines), {
      onSuccess: () =>
        navigate(`/cashier/sessions/${sessionId}`, {
          state: { tableNumber },
        }),
    })
  }

  return (
    <>
      <div className="flex items-center gap-2">
        <Button size="icon" variant="ghost" asChild>
          <Link to={`/cashier/sessions/${sessionId}`} state={{ tableNumber }}>
            <ArrowLeft />
            <span className="sr-only">Back to session</span>
          </Link>
        </Button>
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">New order</h1>
          <p className="text-muted-foreground text-sm">
            {tableNumber ? `Table ${tableNumber} · ` : ""}
            Pick items on the left; the round builds up on the right.
          </p>
        </div>
      </div>

      <div className="flex flex-col gap-4 lg:flex-row">
        <MenuBrowser onPick={setPicked} />
        <div className="w-full shrink-0 lg:w-95">
          <RoundPanel
            lines={lines}
            submitting={submit.isPending}
            onChangeQuantity={changeQuantity}
            onRemove={(key) =>
              setLines((prev) => prev.filter((l) => l.key !== key))
            }
            onSubmit={handleSubmit}
          />
        </div>
      </div>

      {picked && (
        <ItemConfigDialog
          key={picked.id}
          item={picked}
          onOpenChange={(open) => !open && setPicked(null)}
          onAdd={addLine}
        />
      )}
    </>
  )
}
