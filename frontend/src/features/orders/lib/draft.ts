import type { CashierRoundRequest, DraftLine, DraftSelection } from "../types"

export function lineUnitPrice(line: DraftLine): number {
  const options = line.selections.reduce(
    (sum, s) => sum + (s.option.unitPrice ?? 0) * s.quantity,
    0
  )
  return (line.item.price ?? 0) + options
}

export function lineTotal(line: DraftLine): number {
  return lineUnitPrice(line) * line.quantity
}

export function draftTotal(lines: DraftLine[]): number {
  return lines.reduce((sum, line) => sum + lineTotal(line), 0)
}

/** Identical item + selections + remark ⇒ same signature, so lines merge. */
export function lineSignature(
  line: Pick<DraftLine, "item" | "remark" | "selections">
): string {
  const selections = line.selections
    .map((s) => `${s.option.id}x${s.quantity}`)
    .sort()
    .join(",")
  return `${line.item.id}|${selections}|${line.remark}`
}

export function toRoundRequest(lines: DraftLine[]): CashierRoundRequest {
  return {
    lines: lines.map((line) => ({
      menuItemId: line.item.id!,
      quantity: line.quantity,
      remark: line.remark || undefined,
      selections: line.selections.map((s: DraftSelection) => ({
        modifierOptionId: s.option.id!,
        quantity: s.quantity,
      })),
    })),
  }
}
