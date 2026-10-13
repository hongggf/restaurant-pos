import { useState } from "react"
import { Plus, Users } from "lucide-react"

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import {
  Empty,
  EmptyContent,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from "@/components/ui/empty"
import { Skeleton } from "@/components/ui/skeleton"
import { CreateUserDialog } from "@/features/users/components/create-user-dialog"
import { DeleteUserDialog } from "@/features/users/components/delete-user-dialog"
import { EditUserDialog } from "@/features/users/components/edit-user-dialog"
import { ResetPasswordDialog } from "@/features/users/components/reset-password-dialog"
import { UsersTable } from "@/features/users/components/users-table"
import { useUsers } from "@/features/users/hooks/use-users"
import type { User } from "@/features/users/types"
import { getUsername } from "@/lib/auth/token"

export function UsersPage() {
  const { data, isPending, isError, error, refetch } = useUsers()
  const currentUsername = getUsername()

  const [createOpen, setCreateOpen] = useState(false)
  const [editing, setEditing] = useState<User | null>(null)
  const [resetting, setResetting] = useState<User | null>(null)
  const [deleting, setDeleting] = useState<User | null>(null)

  const users = data ?? []

  return (
    <>
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Users</h1>
          <p className="text-muted-foreground text-sm">
            Staff accounts for the back office and cashier POS.
          </p>
        </div>
        {users.length > 0 && (
          <Button onClick={() => setCreateOpen(true)}>
            <Plus />
            New user
          </Button>
        )}
      </div>

      {isPending ? (
        <div className="space-y-2">
          <Skeleton className="h-10 w-full" />
          {Array.from({ length: 4 }, (_, i) => (
            <Skeleton key={i} className="h-12 w-full" />
          ))}
        </div>
      ) : isError ? (
        <Alert variant="destructive">
          <AlertTitle>Couldn&apos;t load users</AlertTitle>
          <AlertDescription>
            <p>{error.message}</p>
            <Button
              variant="outline"
              size="sm"
              className="mt-2"
              onClick={() => refetch()}
            >
              Try again
            </Button>
          </AlertDescription>
        </Alert>
      ) : users.length === 0 ? (
        <Empty className="border border-dashed">
          <EmptyHeader>
            <EmptyMedia variant="icon">
              <Users />
            </EmptyMedia>
            <EmptyTitle>No users yet</EmptyTitle>
            <EmptyDescription>
              Add accounts for your cashiers and fellow admins.
            </EmptyDescription>
          </EmptyHeader>
          <EmptyContent>
            <Button onClick={() => setCreateOpen(true)}>
              <Plus />
              New user
            </Button>
          </EmptyContent>
        </Empty>
      ) : (
        <UsersTable
          users={users}
          currentUsername={currentUsername}
          onEdit={setEditing}
          onResetPassword={setResetting}
          onDelete={setDeleting}
        />
      )}

      <CreateUserDialog open={createOpen} onOpenChange={setCreateOpen} />
      <EditUserDialog
        user={editing}
        isSelf={editing?.username === currentUsername}
        onOpenChange={(open) => !open && setEditing(null)}
      />
      <ResetPasswordDialog
        user={resetting}
        onOpenChange={(open) => !open && setResetting(null)}
      />
      <DeleteUserDialog
        user={deleting}
        onOpenChange={(open) => !open && setDeleting(null)}
      />
    </>
  )
}
