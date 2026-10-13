import { useState } from "react"
import { Minus, Plus } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Label } from "@/components/ui/label"
import { Separator } from "@/components/ui/separator"
import { Spinner } from "@/components/ui/spinner"
import { choiceRule } from "@/features/modifiers/lib/choice-rule"
import type {
  AttachedModifierGroup,
  ModifierOption,
} from "@/features/modifiers/types"
import type { RoundLine } from "@/features/sessions/types"
import { formatPrice } from "@/lib/format"
import { cn } from "@/lib/utils"
import { useItemModifierGroups } from "../hooks/use-manual-order"

export interface SelectionUpdate {
  modifierOptionId: string
  quantity: number
}

interface EditLineSelectionsDialogProps {
  line: RoundLine
  onOpenChange: (open: boolean) => void
  onSave: (selections: SelectionUpdate[]) => void
  saving: boolean
}

/** Edits a sent line's modifier selections only — quantity and remark are
 *  fixed at send-time and aren't part of this endpoint. */
export function EditLineSelectionsDialog({
  line,
  onOpenChange,
  onSave,
  saving,
}: EditLineSelectionsDialogProps) {
  const { data, isPending, isError } = useItemModifierGroups(
    line.menuItemId ?? undefined
  )

  return (
    <Dialog open onOpenChange={onOpenChange}>
      <DialogContent className="flex max-h-[85svh] flex-col">
        <DialogHeader>
          <DialogTitle>{line.nameEn}</DialogTitle>
          <DialogDescription>Edit modifiers for this item</DialogDescription>
        </DialogHeader>

        {isPending ? (
          <div className="flex justify-center py-6">
            <Spinner />
          </div>
        ) : isError ? (
          <p className="text-destructive text-sm">
            Couldn&apos;t load this item&apos;s modifiers. Close and try
            again.
          </p>
        ) : (
          <EditLineSelectionsForm
            line={line}
            groups={data ?? []}
            onSave={onSave}
            onCancel={() => onOpenChange(false)}
            saving={saving}
          />
        )}
      </DialogContent>
    </Dialog>
  )
}

interface EditLineSelectionsFormProps {
  line: RoundLine
  groups: AttachedModifierGroup[]
  onSave: (selections: SelectionUpdate[]) => void
  onCancel: () => void
  saving: boolean
}

function EditLineSelectionsForm({
  line,
  groups: attachedGroups,
  onSave,
  onCancel,
  saving,
}: EditLineSelectionsFormProps) {
  const groups = attachedGroups
    .filter((attached) => attached.group?.active !== false)
    .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))

  // Seeded once from the live groups data and the line's current selections —
  // an option no longer attached to this item just can't be preselected.
  const [selected, setSelected] = useState<Record<string, number>>(() => {
    const init: Record<string, number> = {}
    for (const s of line.selections ?? []) {
      if (s.modifierOptionId) init[s.modifierOptionId] = s.quantity ?? 1
    }
    return init
  })

  function groupOptions(groupIndex: number): ModifierOption[] {
    return (groups[groupIndex].group?.options ?? []).filter(
      (option) => option.available !== false
    )
  }

  function selectedCount(groupIndex: number) {
    return groupOptions(groupIndex).filter((o) => o.id! in selected).length
  }

  function toggle(groupIndex: number, option: ModifierOption) {
    setSelected((prev) => {
      const next = { ...prev }
      if (option.id! in next) {
        delete next[option.id!]
        return next
      }
      // Single-choice groups behave like radios: picking replaces.
      for (const o of groupOptions(groupIndex)) delete next[o.id!]
      next[option.id!] = 1
      return next
    })
  }

  function changeOptionQuantity(option: ModifierOption, delta: number) {
    setSelected((prev) => {
      const next = { ...prev }
      const current = next[option.id!] ?? 0
      const updated = current + delta
      if (updated <= 0) {
        delete next[option.id!]
      } else {
        next[option.id!] = updated
      }
      return next
    })
  }

  const violations = groups.filter((attached, index) => {
    const count = selectedCount(index)
    const min = attached.group?.minChoice ?? 0
    const max = attached.group?.maxChoice
    return count < min || (max != null && count > max)
  })

  return (
    <>
      <div className="-mx-1 flex-1 space-y-4 overflow-y-auto px-1">
        {groups.map((attached, index) => {
          const group = attached.group
          if (!group) return null
          const single = group.maxChoice === 1
          const atMax =
            !single &&
            group.maxChoice != null &&
            selectedCount(index) >= group.maxChoice
          return (
            <div key={group.id} className="space-y-2">
              <div className="flex items-baseline justify-between gap-2">
                <p className="text-sm font-medium">{group.nameEn}</p>
                <p className="text-muted-foreground text-xs">
                  {choiceRule(group.minChoice, group.maxChoice)}
                </p>
              </div>
              <div className="grid gap-1.5">
                {groupOptions(index).map((option) => {
                  const qty = selected[option.id!] ?? 0
                  const price = option.unitPrice ?? 0
                  if (single) {
                    return (
                      <Label
                        key={option.id}
                        className={cn(
                          "hover:bg-muted/50 flex items-center gap-2 rounded-md border p-2 font-normal",
                          qty > 0 && "border-primary bg-primary/5"
                        )}
                      >
                        <Checkbox
                          checked={qty > 0}
                          onCheckedChange={() => toggle(index, option)}
                        />
                        <span className="flex-1 text-sm">
                          {option.nameEn}
                        </span>
                        {price > 0 && (
                          <span className="text-muted-foreground text-xs tabular-nums">
                            +{formatPrice(price)}
                          </span>
                        )}
                      </Label>
                    )
                  }
                  return (
                    <div
                      key={option.id}
                      className={cn(
                        "flex items-center gap-2 rounded-md border p-2",
                        qty > 0 && "border-primary bg-primary/5"
                      )}
                    >
                      <span className="flex-1 text-sm">{option.nameEn}</span>
                      <div className="flex items-center gap-1">
                        <Button
                          size="icon-xs"
                          variant="outline"
                          disabled={qty === 0}
                          onClick={() => changeOptionQuantity(option, -1)}
                        >
                          <Minus />
                          <span className="sr-only">Fewer {option.nameEn}</span>
                        </Button>
                        <span className="w-6 text-center text-sm font-medium tabular-nums">
                          {qty}
                        </span>
                        <Button
                          size="icon-xs"
                          variant="outline"
                          disabled={qty === 0 && atMax}
                          onClick={() => changeOptionQuantity(option, 1)}
                        >
                          <Plus />
                          <span className="sr-only">More {option.nameEn}</span>
                        </Button>
                      </div>
                      {price > 0 && (
                        <span className="text-muted-foreground w-14 text-right text-xs tabular-nums">
                          +{formatPrice(price)}
                        </span>
                      )}
                    </div>
                  )
                })}
              </div>
            </div>
          )
        })}
      </div>

      <Separator />
      <DialogFooter>
        <Button variant="outline" onClick={onCancel} disabled={saving}>
          Cancel
        </Button>
        <Button
          disabled={saving || violations.length > 0}
          onClick={() =>
            onSave(
              Object.entries(selected).map(([modifierOptionId, quantity]) => ({
                modifierOptionId,
                quantity,
              }))
            )
          }
        >
          {saving ? <Spinner /> : null}
          {saving ? "Saving…" : "Save changes"}
        </Button>
      </DialogFooter>
    </>
  )
}
