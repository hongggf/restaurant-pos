import { useState } from "react"

import { Badge } from "@/components/ui/badge"
import { Checkbox } from "@/components/ui/checkbox"
import {
  Command,
  CommandEmpty,
  CommandInput,
  CommandItem,
  CommandList,
} from "@/components/ui/command"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Spinner } from "@/components/ui/spinner"
import type { MenuItem } from "@/features/menu/types"
import { choiceRule } from "../lib/choice-rule"
import {
  useAttachModifierGroup,
  useAttachedModifierGroups,
  useDetachModifierGroup,
  useModifierGroupOptions,
} from "../hooks/use-modifier-groups"
import type { ModifierGroup } from "../types"

interface AttachModifiersDialogProps {
  /** The menu item whose modifier groups are being managed; null = closed. */
  item: MenuItem | null
  onOpenChange: (open: boolean) => void
}

export function AttachModifiersDialog({
  item,
  onOpenChange,
}: AttachModifiersDialogProps) {
  const groups = useModifierGroupOptions()
  const attached = useAttachedModifierGroups(item?.id)
  const attach = useAttachModifierGroup()
  const detach = useDetachModifierGroup()
  const [togglingId, setTogglingId] = useState<string | null>(null)

  const attachedIds = new Set(
    (attached.data ?? []).map((entry) => entry.group?.id)
  )
  const loading = groups.isPending || attached.isPending

  function toggle(group: ModifierGroup) {
    if (!item?.id || !group.id || togglingId) return
    setTogglingId(group.id)
    const settled = { onSettled: () => setTogglingId(null) }
    if (attachedIds.has(group.id)) {
      detach.mutate(
        { menuItemId: item.id, modifierGroupId: group.id },
        settled
      )
    } else {
      attach.mutate(
        {
          menuItemId: item.id,
          body: {
            modifierGroupId: group.id,
            sortOrder: attached.data?.length ?? 0,
          },
        },
        settled
      )
    }
  }

  return (
    <Dialog open={!!item} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Modifiers for "{item?.nameEn}"</DialogTitle>
          <DialogDescription>
            Check the groups customers should choose from when ordering this
            item.
          </DialogDescription>
        </DialogHeader>
        {loading ? (
          <div className="flex justify-center py-8">
            <Spinner />
          </div>
        ) : (groups.data ?? []).length === 0 ? (
          <p className="text-muted-foreground py-4 text-center text-sm">
            No modifier groups yet. Create one on the Modifier Groups page
            first.
          </p>
        ) : (
          <Command className="rounded-md border">
            <CommandInput placeholder="Search modifier groups…" />
            <CommandList>
              <CommandEmpty>No groups match.</CommandEmpty>
              {(groups.data ?? []).map((group) => (
                <CommandItem
                  key={group.id}
                  value={`${group.nameEn} ${group.nameKm}`}
                  disabled={!!togglingId}
                  onSelect={() => toggle(group)}
                >
                  {togglingId === group.id ? (
                    <Spinner className="size-4" />
                  ) : (
                    <Checkbox
                      checked={attachedIds.has(group.id)}
                      tabIndex={-1}
                      aria-hidden
                      className="pointer-events-none"
                    />
                  )}
                  <span className="min-w-0 flex-1 truncate">
                    {group.nameEn}
                    {!group.active && (
                      <span className="text-muted-foreground"> (inactive)</span>
                    )}
                  </span>
                  <Badge variant="outline" className="ml-auto">
                    {choiceRule(group.minChoice, group.maxChoice)}
                  </Badge>
                </CommandItem>
              ))}
            </CommandList>
          </Command>
        )}
      </DialogContent>
    </Dialog>
  )
}
