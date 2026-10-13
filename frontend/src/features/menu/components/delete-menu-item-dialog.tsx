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
import { useDeleteMenuItem } from "../hooks/use-menu-items"
import type { MenuItem } from "../types"

interface DeleteMenuItemDialogProps {
  item: MenuItem | null
  onOpenChange: (open: boolean) => void
}

export function DeleteMenuItemDialog({
  item,
  onOpenChange,
}: DeleteMenuItemDialogProps) {
  const remove = useDeleteMenuItem()

  return (
    <AlertDialog open={!!item} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Delete "{item?.nameEn}"?</AlertDialogTitle>
          <AlertDialogDescription>
            The item will disappear from the menu for customers and the
            cashier. This can&apos;t be undone.
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
              if (!item?.id) return
              remove.mutate(item.id, {
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
