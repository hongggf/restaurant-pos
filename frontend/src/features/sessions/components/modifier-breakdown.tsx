import { useMemo } from "react"

import { useItemModifierGroups } from "@/features/orders/hooks/use-manual-order"
import type { components } from "@/lib/api/schema"
import { formatPrice } from "@/lib/format"
import type { AttachedModifierGroup } from "@/features/modifiers/types"

type RoundSelection = components["schemas"]["OrderRoundSelectionResponse"]

export interface ModifierBreakdownProps {
  menuItemId?: string
  selections?: RoundSelection[]
}

function buildGroupLookup(
  attachedGroups: AttachedModifierGroup[]
) {
  const lookup = new Map<string, { nameEn?: string; order: number }>()
  attachedGroups.forEach((ag, gIndex) => {
    const g = ag.group
    if (!g || !g.options) return
    for (const opt of g.options) {
      if (opt.id) {
        lookup.set(opt.id, { nameEn: g.nameEn, order: gIndex })
      }
    }
  })
  return lookup
}

function groupSelections(
  selections: RoundSelection[],
  lookup: Map<string, { nameEn?: string; order: number }>
) {
  const groups: {
    key: string
    nameEn?: string
    order: number
    items: RoundSelection[]
  }[] = []
  selections.forEach((selection, index) => {
    const info = selection.modifierOptionId
      ? lookup.get(selection.modifierOptionId)
      : undefined
    const key = info?.nameEn ?? "__ungrouped__"
    let bucket = groups.find((g) => g.key === key)
    if (!bucket) {
      bucket = {
        key,
        nameEn: info?.nameEn,
        order: info?.order ?? Number.MAX_SAFE_INTEGER + index,
        items: [],
      }
      groups.push(bucket)
    }
    bucket.items.push(selection)
  })
  return groups.sort((a, b) => a.order - b.order)
}

export function ModifierBreakdown({
  menuItemId,
  selections,
}: ModifierBreakdownProps) {
  const { data: attachedGroups } = useItemModifierGroups(menuItemId)
  const lookup = useMemo(
    () => buildGroupLookup(attachedGroups ?? []),
    [attachedGroups]
  )
  const groups = selections?.length
    ? groupSelections(selections, lookup)
    : []

  if (groups.length === 0) return null

  return (
    <div className="space-y-1">
      {groups.map((group) => (
        <div key={group.key}>
          {group.nameEn && (
            <p className="text-muted-foreground text-[11px] font-medium">
              {group.nameEn}
            </p>
          )}
          <ul className="text-muted-foreground text-xs">
            {group.items.map((s) => {
              const price = (s.unitPrice ?? 0) * (s.quantity ?? 1)
              return (
                <li
                  key={s.modifierOptionId ?? s.nameEn}
                  className="flex items-center justify-between gap-2"
                >
                  <span>
                    •{" "}
                    {(s.quantity ?? 1) > 1
                      ? `${s.nameEn} ×${s.quantity}`
                      : s.nameEn}
                  </span>
                  {price > 0 && (
                    <span className="tabular-nums">{formatPrice(price)}</span>
                  )}
                </li>
              )
            })}
          </ul>
        </div>
      ))}
    </div>
  )
}
