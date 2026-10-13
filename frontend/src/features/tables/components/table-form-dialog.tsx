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
  FieldError,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Switch } from "@/components/ui/switch"
import { useCreateTable, useUpdateTable } from "../hooks/use-tables"
import { tableSchema, type TableValues } from "../schemas/table"
import type { DiningTable } from "../types"

interface TableFormDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** When set, the dialog edits this table; otherwise it creates one. */
  table?: DiningTable
}

function toValues(table?: DiningTable): TableValues {
  return {
    tableNumber: table?.tableNumber ?? "",
    active: table?.active ?? true,
  }
}

export function TableFormDialog({
  open,
  onOpenChange,
  table,
}: TableFormDialogProps) {
  const form = useForm<TableValues>({
    resolver: zodResolver(tableSchema),
    defaultValues: toValues(table),
  })
  const create = useCreateTable()
  const update = useUpdateTable()
  const isPending = create.isPending || update.isPending

  useEffect(() => {
    if (open) form.reset(toValues(table))
  }, [open, table, form])

  function onSubmit(values: TableValues) {
    const done = { onSuccess: () => onOpenChange(false) }
    if (table?.id) {
      update.mutate({ id: table.id, ...values }, done)
    } else {
      create.mutate({ tableNumber: values.tableNumber }, done)
    }
  }

  const { errors } = form.formState

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-sm">
        <DialogHeader>
          <DialogTitle>{table ? "Edit table" : "New table"}</DialogTitle>
          <DialogDescription>
            {table
              ? `Update table ${table.tableNumber}.`
              : "Add a physical table. A permanent QR code is generated for it."}
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={form.handleSubmit(onSubmit)} noValidate>
          <FieldGroup>
            <Field data-invalid={!!errors.tableNumber}>
              <FieldLabel htmlFor="tableNumber">Table number</FieldLabel>
              <Input
                id="tableNumber"
                placeholder="12"
                aria-invalid={!!errors.tableNumber}
                {...form.register("tableNumber")}
              />
              <FieldError errors={[errors.tableNumber]} />
            </Field>
            {table && (
              <Field orientation="horizontal">
                <FieldLabel htmlFor="active">Active</FieldLabel>
                <Switch
                  id="active"
                  checked={form.watch("active")}
                  onCheckedChange={(checked) =>
                    form.setValue("active", checked)
                  }
                />
              </Field>
            )}
            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={() => onOpenChange(false)}
                disabled={isPending}
              >
                Cancel
              </Button>
              <Button type="submit" disabled={isPending}>
                {isPending
                  ? "Saving…"
                  : table
                    ? "Save changes"
                    : "Create table"}
              </Button>
            </DialogFooter>
          </FieldGroup>
        </form>
      </DialogContent>
    </Dialog>
  )
}
