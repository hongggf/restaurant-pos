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
import { useDeleteUser } from "../hooks/use-users"
import type { User } from "../types"

interface DeleteUserDialogProps {
  user: User | null
  onOpenChange: (open: boolean) => void
}

export function DeleteUserDialog({
  user,
  onOpenChange,
}: DeleteUserDialogProps) {
  const remove = useDeleteUser()

  return (
    <AlertDialog open={!!user} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Delete "{user?.username}"?</AlertDialogTitle>
          <AlertDialogDescription>
            This permanently removes the account. If the person just left the
            staff, prefer disabling the account instead — that keeps their
            history intact. This can&apos;t be undone.
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
              if (!user?.id) return
              remove.mutate(user.id, {
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
