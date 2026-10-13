import type { AttachedModifierGroup, ModifierOption } from "../types"

/**
 * A built bowl (DIY Malatang) has to carry at least this much of actual food
 * before a guest can add it to the cart. The base is free and a flavour is a
 * nominal cent, so without this a $0.01 bowl of plain broth would be orderable.
 */
export const BUILD_MINIMUM = 3

/**
 * Only these groups count toward the minimum, matched on group name. Flavour is
 * excluded because its options are nominal and required anyway; drinks are
 * excluded because they are extras, not the meal.
 *
 * Today these groups are attached only to DIY Malatang, so the rule applies
 * there and nowhere else — matching on name rather than item id means a future
 * build-your-own item that reuses them inherits the same floor.
 */
const COUNTED_GROUPS = new Set([
  "meat",
  "meat ball",
  "veggie",
  "noodles & rice",
])

export interface BuildMinimumProgress {
  /** True when the item has any of the counted groups — i.e. it is a build. */
  applies: boolean
  /** Value selected so far from the counted groups. */
  total: number
  /** Still needed to reach the minimum; 0 once satisfied. */
  shortfall: number
  /** Counted group names, in display order, for the guidance message. */
  groupNames: string[]
}

/** Prices are dollars with cent precision; compare as integer cents so that
 *  0.70×3 + 0.90 (a real $3.00 basket) isn't rejected as 2.9999999999999996. */
function cents(amount: number) {
  return Math.round(amount * 100)
}

export function buildMinimumProgress(
  groups: AttachedModifierGroup[],
  selected: Record<string, { option: ModifierOption; quantity: number }>
): BuildMinimumProgress {
  const counted = groups.filter((attached) =>
    COUNTED_GROUPS.has((attached.group?.nameEn ?? "").trim().toLowerCase())
  )
  if (counted.length === 0) {
    return { applies: false, total: 0, shortfall: 0, groupNames: [] }
  }

  let totalCents = 0
  for (const attached of counted) {
    for (const option of attached.group?.options ?? []) {
      const pick = option.id ? selected[option.id] : undefined
      if (pick) totalCents += cents(option.unitPrice ?? 0) * pick.quantity
    }
  }

  const shortfallCents = Math.max(0, cents(BUILD_MINIMUM) - totalCents)
  return {
    applies: true,
    total: totalCents / 100,
    shortfall: shortfallCents / 100,
    groupNames: counted.map((attached) => attached.group?.nameEn ?? ""),
  }
}

/** "Meat, Meat Ball, Veggie or Noodles & Rice" — Intl handles the commas. */
const listFormat = new Intl.ListFormat("en", { style: "long", type: "disjunction" })

export function formatGroupList(names: string[]) {
  return listFormat.format(names.filter(Boolean))
}
