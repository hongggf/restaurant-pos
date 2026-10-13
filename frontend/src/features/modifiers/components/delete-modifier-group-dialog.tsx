import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { useDeleteModifierGroup } from "../hooks/use-modifier-groups"
import type { ModifierGroup } from "../types"

interface DeleteModifierGroupDialogProps {
  group: ModifierGroup | null
  onOpenChange: (open: boolean) => void
}

export function DeleteModifierGroupDialog({
  group,
  onOpenChange,
}: DeleteModifierGroupDialogProps) {
  const remove = useDeleteModifierGroup()

  return (
    <AlertDialog open={!!group} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Delete "{group?.nameEn}"?</AlertDialogTitle>
          <AlertDialogDescription>
            The group and its options will be removed from every menu item it
            is attached to. This can&apos;t be undone.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel disabled={remove.isPending}>
            Cancel
          </AlertDialogCancel>
          <AlertDialogAction
            className="bg-destructive text-white hover:bg-destructive/90"
            disabled={remove.isPending}
            onClick={(event) => {
              event.preventDefault()
              if (!group?.id) return
              remove.mutate(group.id, {
                onSuccess: () => onOpenChange(false),
              })
            }}
          >
            {remove.isPending ? "Deleting…" : "Delete"}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
