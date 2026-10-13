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
import { useResetPassword } from "../hooks/use-users"
import {
  resetPasswordSchema,
  type ResetPasswordValues,
} from "../schemas/user"
import type { User } from "../types"

interface ResetPasswordDialogProps {
  user: User | null
  onOpenChange: (open: boolean) => void
}

export function ResetPasswordDialog({
  user,
  onOpenChange,
}: ResetPasswordDialogProps) {
  const form = useForm<ResetPasswordValues>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: { newPassword: "" },
  })
  const reset = useResetPassword()

  useEffect(() => {
    if (user) form.reset({ newPassword: "" })
  }, [user, form])

  function onSubmit(values: ResetPasswordValues) {
    if (!user?.id) return
    reset.mutate(
      { id: user.id, ...values },
      { onSuccess: () => onOpenChange(false) }
    )
  }

  const { errors } = form.formState

  return (
    <Dialog open={!!user} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-sm">
        <DialogHeader>
          <DialogTitle>Reset password</DialogTitle>
          <DialogDescription>
            Set a new password for &quot;{user?.username}&quot;.
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={form.handleSubmit(onSubmit)} noValidate>
          <FieldGroup>
            <Field data-invalid={!!errors.newPassword}>
              <FieldLabel htmlFor="newPassword">New password</FieldLabel>
              <Input
                id="newPassword"
                type="password"
                autoComplete="new-password"
                aria-invalid={!!errors.newPassword}
                {...form.register("newPassword")}
              />
              <FieldDescription>
                They&apos;ll use this password at their next login.
              </FieldDescription>
              <FieldError errors={[errors.newPassword]} />
            </Field>
            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={() => onOpenChange(false)}
                disabled={reset.isPending}
              >
                Cancel
              </Button>
              <Button type="submit" disabled={reset.isPending}>
                {reset.isPending ? "Saving…" : "Reset password"}
              </Button>
            </DialogFooter>
          </FieldGroup>
        </form>
      </DialogContent>
    </Dialog>
  )
}
