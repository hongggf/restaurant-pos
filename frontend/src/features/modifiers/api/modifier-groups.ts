import { apiFetch } from "@/lib/api/client"
import type {
  AttachModifierGroupRequest,
  AttachedModifierGroup,
  ModifierGroup,
  ModifierGroupCreateRequest,
  ModifierGroupPage,
  ModifierGroupUpdateRequest,
} from "../types"

export function listModifierGroups({
  page,
  size,
}: {
  page: number
  size: number
}) {
  const query = new URLSearchParams({
    page: String(page),
    size: String(size),
    sort: "nameEn,asc",
  })
  return apiFetch<ModifierGroupPage>(`/modifier-groups?${query}`)
}

export function createModifierGroup(body: ModifierGroupCreateRequest) {
  return apiFetch<ModifierGroup>("/modifier-groups", {
    method: "POST",
    body: JSON.stringify(body),
  })
}

export function updateModifierGroup(
  id: string,
  body: ModifierGroupUpdateRequest
) {
  return apiFetch<ModifierGroup>(`/modifier-groups/${id}`, {
    method: "PUT",
    body: JSON.stringify(body),
  })
}

export function deleteModifierGroup(id: string) {
  return apiFetch<void>(`/modifier-groups/${id}`, { method: "DELETE" })
}

export function listAttachedModifierGroups(menuItemId: string) {
  return apiFetch<AttachedModifierGroup[]>(
    `/menu-items/${menuItemId}/modifier-groups`
  )
}

export function attachModifierGroup(
  menuItemId: string,
  body: AttachModifierGroupRequest
) {
  return apiFetch<AttachedModifierGroup>(
    `/menu-items/${menuItemId}/modifier-groups`,
    { method: "POST", body: JSON.stringify(body) }
  )
}

export function detachModifierGroup(
  menuItemId: string,
  modifierGroupId: string
) {
  return apiFetch<void>(
    `/menu-items/${menuItemId}/modifier-groups/${modifierGroupId}`,
    { method: "DELETE" }
  )
}

export function uploadModifierOptionImage(
  groupId: string,
  optionId: string,
  file: File
) {
  const formData = new FormData()
  formData.append("file", file)
  return apiFetch<any>(`/modifier-groups/${groupId}/options/${optionId}/image`, {
    method: "POST",
    body: formData,
  })
}
