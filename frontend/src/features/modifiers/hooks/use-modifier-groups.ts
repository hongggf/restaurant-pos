import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query"
import { toast } from "sonner"

import type { ApiError } from "@/lib/api/client"
import {
  attachModifierGroup,
  createModifierGroup,
  deleteModifierGroup,
  detachModifierGroup,
  listAttachedModifierGroups,
  listModifierGroups,
  updateModifierGroup,
} from "../api/modifier-groups"
import type {
  AttachModifierGroupRequest,
  ModifierGroupCreateRequest,
  ModifierGroupUpdateRequest,
} from "../types"

export const PAGE_SIZE = 12

export function useModifierGroups(page: number) {
  return useQuery({
    queryKey: ["modifier-groups", "list", page],
    queryFn: () => listModifierGroups({ page, size: PAGE_SIZE }),
    placeholderData: keepPreviousData,
  })
}

/** All groups, for the attach-to-item picker. */
export function useModifierGroupOptions() {
  return useQuery({
    queryKey: ["modifier-groups", "options"],
    queryFn: () => listModifierGroups({ page: 0, size: 100 }),
    select: (data) => data.content ?? [],
  })
}

function useInvalidateModifierGroups() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: ["modifier-groups"] })
}

export function useCreateModifierGroup() {
  const invalidate = useInvalidateModifierGroups()
  return useMutation({
    mutationFn: (body: ModifierGroupCreateRequest) =>
      createModifierGroup(body),
    onSuccess: (data) => {
      invalidate()
      toast.success(`Modifier group "${data.nameEn}" created`)
    },
    onError: (error: ApiError) => toast.error(error.message),
  })
}

export function useUpdateModifierGroup() {
  const invalidate = useInvalidateModifierGroups()
  return useMutation({
    mutationFn: ({
      id,
      body,
    }: {
      id: string
      body: ModifierGroupUpdateRequest
    }) => updateModifierGroup(id, body),
    onSuccess: (data) => {
      invalidate()
      toast.success(`Modifier group "${data.nameEn}" updated`)
    },
    onError: (error: ApiError) => toast.error(error.message),
  })
}

export function useDeleteModifierGroup() {
  const invalidate = useInvalidateModifierGroups()
  return useMutation({
    mutationFn: deleteModifierGroup,
    onSuccess: () => {
      invalidate()
      toast.success("Modifier group deleted")
    },
    onError: (error: ApiError) => toast.error(error.message),
  })
}

export function useAttachedModifierGroups(menuItemId: string | undefined) {
  return useQuery({
    queryKey: ["menu-item-modifier-groups", menuItemId],
    queryFn: () => listAttachedModifierGroups(menuItemId!),
    enabled: !!menuItemId,
  })
}

function useInvalidateAttached() {
  const queryClient = useQueryClient()
  return () =>
    queryClient.invalidateQueries({ queryKey: ["menu-item-modifier-groups"] })
}

export function useAttachModifierGroup() {
  const invalidate = useInvalidateAttached()
  return useMutation({
    mutationFn: ({
      menuItemId,
      body,
    }: {
      menuItemId: string
      body: AttachModifierGroupRequest
    }) => attachModifierGroup(menuItemId, body),
    onSuccess: invalidate,
    onError: (error: ApiError) => toast.error(error.message),
  })
}

export function useDetachModifierGroup() {
  const invalidate = useInvalidateAttached()
  return useMutation({
    mutationFn: ({
      menuItemId,
      modifierGroupId,
    }: {
      menuItemId: string
      modifierGroupId: string
    }) => detachModifierGroup(menuItemId, modifierGroupId),
    onSuccess: invalidate,
    onError: (error: ApiError) => toast.error(error.message),
  })
}
