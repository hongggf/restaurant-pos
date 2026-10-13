import type { components } from "@/lib/api/schema"

export type ModifierGroup = components["schemas"]["ModifierGroupResponse"]
export type ModifierOption = components["schemas"]["ModifierOptionResponse"]
export type ModifierGroupPage =
  components["schemas"]["PageResponseModifierGroupResponse"]
export type ModifierGroupCreateRequest =
  components["schemas"]["ModifierGroupCreateRequest"]
export type ModifierGroupUpdateRequest =
  components["schemas"]["ModifierGroupUpdateRequest"]
export type ModifierOptionRequest =
  components["schemas"]["ModifierOptionRequest"]
export type AttachedModifierGroup =
  components["schemas"]["AttachedModifierGroupResponse"]
export type AttachModifierGroupRequest =
  components["schemas"]["AttachModifierGroupRequest"]
