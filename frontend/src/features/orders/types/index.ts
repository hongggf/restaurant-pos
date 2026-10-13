import type { components } from "@/lib/api/schema"
import type { MenuItem } from "@/features/menu/types"
import type { ModifierOption } from "@/features/modifiers/types"

export type CashierRoundRequest = components["schemas"]["CashierRoundRequest"]
export type CartLineAddRequest = components["schemas"]["CartLineAddRequest"]

/** A selected modifier option on a draft line. */
export interface DraftSelection {
  option: ModifierOption
  quantity: number
  group: { id?: string; nameEn?: string; order: number }
}

/** One line of the round being composed locally, before it's sent. */
export interface DraftLine {
  key: string
  item: MenuItem
  quantity: number
  remark: string
  selections: DraftSelection[]
}
