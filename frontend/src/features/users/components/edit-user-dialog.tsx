import { useEffect } from "react"
import { zodResolver } from "@hookform/resolvers/zod"
import { useForm } from "react-hook-form"

import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import {
  Field,
  FieldDescription,
  FieldError,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Switch } from "@/components/ui/switch"
import { useUpdateUser } from "../hooks/use-users"
import { editUserSchema, type EditUserValues } from "../schemas/user"
import type { User } from "../types"
import { RoleCheckboxes } from "./role-checkboxes"

interface EditUserDialogProps {
  user: User | null
  /** True when the user being edited is the logged-in admin. */
  isSelf: boolean
  onOpenChange: (open: boolean) => void
}

function toValues(user: User | null): EditUserValues {
  return {
    email: user?.email ?? "",
    enabled: user?.enabled ?? true,
    roles: user?.roles ?? [],
  }
}

export function EditUserDialog({
  user,
  isSelf,
  onOpenChange,
}: EditUserDialogProps) {
  const form = useForm<EditUserValues>({
    resolver: zodResolver(editUserSchema),
    defaultValues: toValues(user),
  })
  const update = useUpdateUser()

  useEffect(() => {
    if (user) form.reset(toValues(user))
  }, [user, form])

  function onSubmit(values: EditUserValues) {
    if (!user?.id) return
    update.mutate(
      { id: user.id, ...values },
      { onSuccess: () => onOpenChange(false) }
    )
  }

  const { errors } = form.formState

  return (
    <Dialog open={!!user} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Edit user</DialogTitle>
          <DialogDescription>
            Update &quot;{user?.username}&quot;.
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={form.handleSubmit(onSubmit)} noValidate>
          <FieldGroup>
            <Field>
              <FieldLabel htmlFor="edit-username">Username</FieldLabel>
              <Input id="edit-username" value={user?.username ?? ""} disabled />
              <FieldDescription>
                Usernames can&apos;t be changed.
              </FieldDescription>
            </Field>
            <Field data-invalid={!!errors.email}>
              <FieldLabel htmlFor="edit-email">Email</FieldLabel>
              <Input
                id="edit-email"
                type="email"
                aria-invalid={!!errors.email}
                {...form.register("email")}
              />
              <FieldError errors={[errors.email]} />
            </Field>
            <Field data-invalid={!!errors.roles}>
              <FieldLabel>Roles</FieldLabel>
              <RoleCheckboxes
                value={form.watch("roles")}
                onChange={(roles) =>
                  form.setValue("roles", roles, { shouldValidate: true })
                }
                locked={isSelf ? ["ADMIN"] : []}
              />
              {isSelf && (
                <FieldDescription>
                  You can&apos;t remove your own admin role.
                </FieldDescription>
              )}
              <FieldError errors={[errors.roles]} />
            </Field>
            <Field orientation="horizontal">
              <FieldLabel htmlFor="edit-enabled">Enabled</FieldLabel>
              <Switch
                id="edit-enabled"
                checked={form.watch("enabled")}
                disabled={isSelf}
                onCheckedChange={(checked) =>
                  form.setValue("enabled", checked)
                }
              />
            </Field>
            {isSelf && (
              <FieldDescription>
                You can&apos;t disable your own account.
              </FieldDescription>
            )}
            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={() => onOpenChange(false)}
                disabled={update.isPending}
              >
                Cancel
              </Button>
              <Button type="submit" disabled={update.isPending}>
                {update.isPending ? "Saving…" : "Save changes"}
              </Button>
            </DialogFooter>
          </FieldGroup>
        </form>
      </DialogContent>
    </Dialog>
  )
}
