import type { components } from "@/lib/api/schema"
import type { Category } from "@/features/categories/types"
import type { AttachedModifierGroup, ModifierOption } from "@/features/modifiers/types"
import type { MenuItem, MenuItemPage } from "@/features/menu/types"

export type GuestSessionResponse = components["schemas"]["GuestSessionResponse"]
export type GuestMenuItemDetail = components["schemas"]["GuestMenuItemDetailResponse"]
export type CartResponse = components["schemas"]["CartResponse"]
export type CartLine = components["schemas"]["CartLineResponse"]
export type CartSelection = components["schemas"]["CartSelectionResponse"]
export type CartLineAddRequest = components["schemas"]["CartLineAddRequest"]
export type CartLineUpdateRequest = components["schemas"]["CartLineUpdateRequest"]
export type CartSelectionRequest = components["schemas"]["CartSelectionRequest"]
export type GuestOrdersResponse = components["schemas"]["GuestOrdersResponse"]
export type OrderRound = components["schemas"]["OrderRoundResponse"]
export type OrderRoundLine = components["schemas"]["OrderRoundLineResponse"]

export type { MenuItem, MenuItemPage, Category, AttachedModifierGroup, ModifierOption }
