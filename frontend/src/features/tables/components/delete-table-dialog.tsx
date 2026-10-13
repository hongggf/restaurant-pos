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
import { useDeleteTable } from "../hooks/use-tables"
import type { DiningTable } from "../types"

interface DeleteTableDialogProps {
  table: DiningTable | null
  onOpenChange: (open: boolean) => void
}

export function DeleteTableDialog({
  table,
  onOpenChange,
}: DeleteTableDialogProps) {
  const remove = useDeleteTable()

  return (
    <AlertDialog open={!!table} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>
            Delete table {table?.tableNumber}?
          </AlertDialogTitle>
          <AlertDialogDescription>
            The printed QR code for this table will stop working and customers
            won&apos;t be able to order from it. This can&apos;t be undone.
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
              if (!table?.id) return
              remove.mutate(table.id, {
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
