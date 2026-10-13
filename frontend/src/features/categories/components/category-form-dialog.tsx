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
import { Textarea } from "@/components/ui/textarea"
import { useCreateCategory, useUpdateCategory } from "../hooks/use-categories"
import { categorySchema, type CategoryValues } from "../schemas/category"
import type { Category } from "../types"

interface CategoryFormDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** When set, the dialog edits this category; otherwise it creates one. */
  category?: Category
}

function toValues(category?: Category): CategoryValues {
  return {
    nameEn: category?.nameEn ?? "",
    nameKm: category?.nameKm ?? "",
    description: category?.description ?? "",
    sortOrder: category?.sortOrder ?? 0,
    active: category?.active ?? true,
    allowNotes: category?.allowNotes ?? true,
  }
}

export function CategoryFormDialog({
  open,
  onOpenChange,
  category,
}: CategoryFormDialogProps) {
  const form = useForm<CategoryValues>({
    resolver: zodResolver(categorySchema),
    defaultValues: toValues(category),
  })
  const create = useCreateCategory()
  const update = useUpdateCategory()
  const isPending = create.isPending || update.isPending

  useEffect(() => {
    if (open) form.reset(toValues(category))
  }, [open, category, form])

  function onSubmit(values: CategoryValues) {
    const done = { onSuccess: () => onOpenChange(false) }
    if (category?.id) {
      update.mutate({ id: category.id, ...values }, done)
    } else {
      create.mutate(values, done)
    }
  }

  const { errors } = form.formState

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>
            {category ? "Edit category" : "New category"}
          </DialogTitle>
          <DialogDescription>
            {category
              ? `Update "${category.nameEn}".`
              : "Add a menu section, e.g. Drinks or Soups."}
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={form.handleSubmit(onSubmit)} noValidate>
          <FieldGroup>
            <Field data-invalid={!!errors.nameEn}>
              <FieldLabel htmlFor="nameEn">Name (English)</FieldLabel>
              <Input
                id="nameEn"
                placeholder="Drinks"
                aria-invalid={!!errors.nameEn}
                {...form.register("nameEn")}
              />
              <FieldError errors={[errors.nameEn]} />
            </Field>
            <Field data-invalid={!!errors.nameKm}>
              <FieldLabel htmlFor="nameKm">Name (Khmer)</FieldLabel>
              <Input
                id="nameKm"
                placeholder="ភេសជ្ជៈ"
                aria-invalid={!!errors.nameKm}
                {...form.register("nameKm")}
              />
              <FieldError errors={[errors.nameKm]} />
            </Field>
            <Field data-invalid={!!errors.description}>
              <FieldLabel htmlFor="description">Description</FieldLabel>
              <Textarea
                id="description"
                placeholder="Optional"
                rows={2}
                {...form.register("description")}
              />
              <FieldError errors={[errors.description]} />
            </Field>
            <Field data-invalid={!!errors.sortOrder}>
              <FieldLabel htmlFor="sortOrder">Sort order</FieldLabel>
              <Input
                id="sortOrder"
                type="number"
                min={0}
                aria-invalid={!!errors.sortOrder}
                {...form.register("sortOrder", { valueAsNumber: true })}
              />
              <FieldError errors={[errors.sortOrder]} />
            </Field>
            <div className="flex gap-4">
              <Field className="flex-1">
                <FieldLabel htmlFor="allowNotes">Special instructions</FieldLabel>
                <div className="flex items-center gap-2 pt-1">
                  <Switch
                    id="allowNotes"
                    checked={form.watch("allowNotes")}
                    onCheckedChange={(checked) =>
                      form.setValue("allowNotes", checked)
                    }
                  />
                  <span className="text-muted-foreground text-xs">
                    {form.watch("allowNotes") ? "Allowed" : "Disabled"}
                  </span>
                </div>
              </Field>
              <Field className="flex-1">
                <FieldLabel htmlFor="active">Active</FieldLabel>
                <div className="flex items-center gap-2 pt-1">
                  <Switch
                    id="active"
                    checked={form.watch("active")}
                    onCheckedChange={(checked) =>
                      form.setValue("active", checked)
                    }
                  />
                  <span className="text-muted-foreground text-xs">
                    {form.watch("active") ? "Visible" : "Hidden"}
                  </span>
                </div>
              </Field>
            </div>
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
                  : category
                    ? "Save changes"
                    : "Create category"}
              </Button>
            </DialogFooter>
          </FieldGroup>
        </form>
      </DialogContent>
    </Dialog>
  )
}
