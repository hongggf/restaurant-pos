import { useState } from "react"
import { Minus, Pencil, Plus, Trash2 } from "lucide-react"

import { Button } from "@/components/ui/button"
import { formatPrice } from "@/lib/format"
import { useLanguage } from "@/lib/language-context"
import { useRemoveCartLine, useUpdateCartLine } from "../hooks/use-guest-cart"
import { GuestItemDialog } from "./guest-item-dialog"
import type { CartLine, MenuItem } from "../types"

interface GuestCartLineProps {
  line: CartLine
  disabled?: boolean
  currencyCode?: string
}

export function GuestCartLine({ line, disabled, currencyCode }: GuestCartLineProps) {
  const { language, t } = useLanguage()
  const isKhmer = language === "km"
  const updateLine = useUpdateCartLine()
  const removeLine = useRemoveCartLine()
  const busy = updateLine.isPending || removeLine.isPending || disabled
  const quantity = line.quantity ?? 1
  const hasSelections = (line.selections?.length ?? 0) > 0
  const [editing, setEditing] = useState(false)

  /* Only a built item has anything to reopen — a drink is just a drink, so it
     gets no edit button. */
  const editable = hasSelections && line.menuItemId != null

  /* The cart line carries everything the dialog needs about the item itself;
     it refetches the modifier groups by id either way. */
  const item: MenuItem = {
    id: line.menuItemId,
    nameEn: line.nameEn,
    nameKm: line.nameKm,
    imageUrl: line.imageUrl,
    price: line.basePrice,
    currencyCode,
  }

  function changeQuantity(delta: number) {
    const next = quantity + delta
    if (next < 1) return
    updateLine.mutate({
      lineId: line.id!,
      body: {
        quantity: next,
        remark: line.remark,
        selections: (line.selections ?? []).map((s) => ({
          modifierOptionId: s.modifierOptionId!,
          quantity: s.quantity ?? 1,
        })),
      },
    })
  }

  const lineName = isKhmer ? (line.nameKm || line.nameEn) : line.nameEn

  return (
    <li className="space-y-1">
      <div className="flex items-baseline gap-2">
        <p className="min-w-0 flex-1 text-sm font-medium">{lineName}</p>
        <span className="text-sm tabular-nums">{formatPrice(line.lineTotal)}</span>
      </div>

      {line.selections?.map((selection) => {
        const selectionQty = selection.quantity ?? 1
        const optionTotal = (selection.unitPrice ?? 0) * selectionQty
        const optionName = isKhmer ? (selection.nameKm || selection.nameEn) : selection.nameEn
        return (
          <div
            key={selection.modifierOptionId}
            className="text-muted-foreground flex items-baseline gap-2 text-xs"
          >
            <span className="min-w-0 flex-1">
              {selectionQty > 1 ? (
                <>
                  {selectionQty}× {optionName}
                  {selection.unitPrice ? ` (${formatPrice(selection.unitPrice, currencyCode)} ea)` : ""}
                </>
              ) : (
                optionName
              )}
            </span>
            <span className="tabular-nums">
              {optionTotal === 0 ? t("free") : formatPrice(optionTotal, currencyCode)}
            </span>
          </div>
        )
      })}

      {/* Option prices are per bowl, so above a quantity of one the rows no
          longer add up to the line total on their own. This closes the gap. */}
      {quantity > 1 && (
        <p className="text-muted-foreground text-xs tabular-nums">
          {formatPrice(line.unitPrice)} {t("each")} × {quantity}
        </p>
      )}

      {line.remark && <p className="text-muted-foreground text-xs italic">“{line.remark}”</p>}

      {/* Quantity on the left, line actions pushed to the right edge. */}
      <div className="flex items-center gap-1 pt-1">
        <Button
          size="icon-xs"
          variant="outline"
          disabled={quantity <= 1 || busy}
          onClick={() => changeQuantity(-1)}
        >
          <Minus />
          <span className="sr-only">Decrease quantity</span>
        </Button>
        <span className="w-6 text-center text-sm tabular-nums">{line.quantity}</span>
        <Button size="icon-xs" variant="outline" disabled={busy} onClick={() => changeQuantity(1)}>
          <Plus />
          <span className="sr-only">Increase quantity</span>
        </Button>

        <div className="ml-auto flex items-center gap-1">
          {editable && (
            <Button
              size="icon-xs"
              variant="ghost"
              className="text-muted-foreground"
              disabled={busy}
              onClick={() => setEditing(true)}
            >
              <Pencil />
              <span className="sr-only">Edit line</span>
            </Button>
          )}
          <Button
            size="icon-xs"
            variant="ghost"
            className="text-muted-foreground"
            disabled={busy}
            onClick={() => removeLine.mutate(line.id!)}
          >
            <Trash2 />
            <span className="sr-only">Remove line</span>
          </Button>
        </div>
      </div>

      {/* Unmounting on close is what resets the dialog, so editing one line
          never shows another line's picks. */}
      {editing && (
        <GuestItemDialog
          key={line.id}
          item={item}
          editLine={line}
          onOpenChange={(open) => !open && setEditing(false)}
        />
      )}
    </li>
  )
}
