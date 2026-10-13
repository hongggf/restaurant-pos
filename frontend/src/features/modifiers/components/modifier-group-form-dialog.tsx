import { useEffect, useState } from "react"
import { zodResolver } from "@hookform/resolvers/zod"
import { Plus, Trash2 } from "lucide-react"
import { useFieldArray, useForm, type FieldErrors } from "react-hook-form"
import { toast } from "sonner"

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
import { assetUrl } from "@/lib/api/client"
import { uploadModifierOptionImage } from "../api/modifier-groups"
import {
  useCreateModifierGroup,
  useUpdateModifierGroup,
} from "../hooks/use-modifier-groups"
import {
  modifierGroupSchema,
  type ModifierGroupValues,
} from "../schemas/modifier-group"
import type { ModifierGroup } from "../types"

interface ModifierGroupFormDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** When set, the dialog edits this group; otherwise it creates one. */
  group?: ModifierGroup
}

const emptyOption = {
  nameEn: "",
  nameKm: "",
  unitPrice: 0,
  available: true,
}

function toValues(group?: ModifierGroup): ModifierGroupValues {
  return {
    nameEn: group?.nameEn ?? "",
    nameKm: group?.nameKm ?? "",
    minChoice: group?.minChoice ?? 0,
    maxChoice: group?.maxChoice ?? undefined,
    active: group?.active ?? true,
    options: group?.options?.length
      ? group.options.map((option) => ({
          id: option.id,
          nameEn: option.nameEn ?? "",
          nameKm: option.nameKm ?? "",
          unitPrice: option.unitPrice ?? 0,
          available: option.available ?? true,
          imageUrl: option.imageUrl,
          packSize: option.packSize,
        }))
      : [emptyOption],
  }
}

function toRequest(values: ModifierGroupValues) {
  return {
    ...values,
    options: values.options.map((option, index) => ({
      ...option,
      // The form accepts the null the API hands back; the request type doesn't.
      imageUrl: option.imageUrl ?? undefined,
      packSize: option.packSize ?? undefined,
      sortOrder: index,
    })),
  }
}

export function ModifierGroupFormDialog({
  open,
  onOpenChange,
  group,
}: ModifierGroupFormDialogProps) {
  const form = useForm<ModifierGroupValues>({
    resolver: zodResolver(modifierGroupSchema),
    defaultValues: toValues(group),
  })
  const options = useFieldArray({ control: form.control, name: "options" })
  const create = useCreateModifierGroup()
  const update = useUpdateModifierGroup()
  const isPending = create.isPending || update.isPending

  const [optionImages, setOptionImages] = useState<Record<number, File>>({})

  useEffect(() => {
    if (open) {
      form.reset(toValues(group))
      setOptionImages({})
    }
  }, [open, group, form])

  function onSubmit(values: ModifierGroupValues) {
    const handleUploads = async (savedGroup: ModifierGroup) => {
      try {
        const uploadPromises = (savedGroup.options || []).map((opt) => {
          const file = optionImages[opt.sortOrder!]
          if (file && opt.id) {
            return uploadModifierOptionImage(savedGroup.id!, opt.id, file)
          }
          return Promise.resolve()
        })
        await Promise.all(uploadPromises)
      } catch (error) {
        console.error("Failed to upload option images", error)
        toast.error("Group saved, but failed to upload some option images.")
      } finally {
        onOpenChange(false)
      }
    }

    if (group?.id) {
      update.mutate({ id: group.id, body: toRequest(values) }, { onSuccess: handleUploads })
    } else {
      create.mutate(toRequest(values), { onSuccess: handleUploads })
    }
  }

  /* Not every field in the schema has an input rendering its error — an option's
     imageUrl and packSize have none. Without this, such a failure just makes the
     submit button look dead. */
  function onInvalid(fieldErrors: FieldErrors<ModifierGroupValues>) {
    toast.error("Couldn't save — check the highlighted fields.")
    console.warn("Modifier group validation failed", fieldErrors)
  }

  const { errors } = form.formState

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90svh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>
            {group ? "Edit modifier group" : "New modifier group"}
          </DialogTitle>
          <DialogDescription>
            {group
              ? `Update "${group.nameEn}".`
              : "A reusable set of choices (sugar level, size, toppings) you can attach to menu items."}
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={form.handleSubmit(onSubmit, onInvalid)} noValidate>
          <FieldGroup>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field data-invalid={!!errors.nameEn}>
                <FieldLabel htmlFor="nameEn">Name (English)</FieldLabel>
                <Input
                  id="nameEn"
                  placeholder="Sugar Level"
                  aria-invalid={!!errors.nameEn}
                  {...form.register("nameEn")}
                />
                <FieldError errors={[errors.nameEn]} />
              </Field>
              <Field data-invalid={!!errors.nameKm}>
                <FieldLabel htmlFor="nameKm">Name (Khmer)</FieldLabel>
                <Input
                  id="nameKm"
                  placeholder="កម្រិតស្ករ"
                  aria-invalid={!!errors.nameKm}
                  {...form.register("nameKm")}
                />
                <FieldError errors={[errors.nameKm]} />
              </Field>
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field data-invalid={!!errors.minChoice}>
                <FieldLabel htmlFor="minChoice">Min choices</FieldLabel>
                <Input
                  id="minChoice"
                  type="number"
                  min={0}
                  step={1}
                  aria-invalid={!!errors.minChoice}
                  {...form.register("minChoice", { valueAsNumber: true })}
                />
                <FieldError errors={[errors.minChoice]} />
              </Field>
              <Field data-invalid={!!errors.maxChoice}>
                <FieldLabel htmlFor="maxChoice">Max choices</FieldLabel>
                <Input
                  id="maxChoice"
                  type="number"
                  min={1}
                  step={1}
                  placeholder="Unlimited"
                  aria-invalid={!!errors.maxChoice}
                  {...form.register("maxChoice", {
                    setValueAs: (value) =>
                      value === "" || value == null
                        ? undefined
                        : Number(value),
                  })}
                />
                <FieldError errors={[errors.maxChoice]} />
              </Field>
            </div>

            <Field>
              <div className="flex items-center justify-between">
                <FieldLabel>Options</FieldLabel>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => options.append(emptyOption)}
                >
                  <Plus />
                  Add option
                </Button>
              </div>
              <div className="text-muted-foreground grid grid-cols-[4rem_1fr_1fr_5.5rem_2.5rem_2rem] gap-2 text-xs">
                <span>Image</span>
                <span>English</span>
                <span>Khmer</span>
                <span>Price</span>
                <span>Avail.</span>
                <span />
              </div>
              <div className="space-y-2">
                {options.fields.map((field, index) => {
                  const rowErrors = errors.options?.[index]
                  return (
                    <div key={field.id}>
                      <div className="grid grid-cols-[4rem_1fr_1fr_5.5rem_2.5rem_2rem] items-center gap-2">
                        <div className="relative h-9 w-full overflow-hidden rounded-md border bg-muted group/img">
                          {optionImages[index] ? (
                            <img src={URL.createObjectURL(optionImages[index])} alt="preview" className="h-full w-full object-cover" />
                          ) : field.imageUrl ? (
                            <img src={assetUrl(field.imageUrl) || undefined} alt="preview" className="h-full w-full object-cover" />
                          ) : (
                            <span className="flex h-full w-full items-center justify-center text-[10px] text-muted-foreground">Img</span>
                          )}
                          <input 
                            type="file" 
                            accept="image/*" 
                            title="Upload image"
                            className="absolute inset-0 cursor-pointer opacity-0"
                            onChange={(e) => {
                              const file = e.target.files?.[0];
                              if (file) {
                                setOptionImages(prev => ({ ...prev, [index]: file }));
                              }
                            }}
                          />
                        </div>
                        <Input
                          placeholder="50%"
                          aria-label={`Option ${index + 1} English name`}
                          aria-invalid={!!rowErrors?.nameEn}
                          {...form.register(`options.${index}.nameEn`)}
                        />
                        <Input
                          placeholder="៥០%"
                          aria-label={`Option ${index + 1} Khmer name`}
                          aria-invalid={!!rowErrors?.nameKm}
                          {...form.register(`options.${index}.nameKm`)}
                        />
                        <Input
                          type="number"
                          min={0}
                          step="0.01"
                          aria-label={`Option ${index + 1} price`}
                          aria-invalid={!!rowErrors?.unitPrice}
                          {...form.register(`options.${index}.unitPrice`, {
                            valueAsNumber: true,
                          })}
                        />
                        <Switch
                          checked={form.watch(`options.${index}.available`)}
                          onCheckedChange={(checked) =>
                            form.setValue(
                              `options.${index}.available`,
                              checked
                            )
                          }
                          aria-label={`Option ${index + 1} available`}
                        />
                        <Button
                          type="button"
                          variant="ghost"
                          size="icon"
                          disabled={options.fields.length === 1}
                          onClick={() => {
                            options.remove(index)
                            setOptionImages(prev => {
                              const next = { ...prev }
                              delete next[index]
                              return next
                            })
                          }}
                        >
                          <Trash2 />
                          <span className="sr-only">
                            Remove option {index + 1}
                          </span>
                        </Button>
                      </div>
                      <FieldError
                        errors={[
                          rowErrors?.nameEn,
                          rowErrors?.nameKm,
                          rowErrors?.unitPrice,
                        ]}
                      />
                    </div>
                  )
                })}
              </div>
              <FieldError errors={[errors.options?.root ?? errors.options]} />
            </Field>

            <Field orientation="horizontal">
              <Switch
                id="active"
                checked={form.watch("active")}
                onCheckedChange={(checked) => form.setValue("active", checked)}
              />
              <FieldLabel htmlFor="active">
                Active (offered when attached to an item)
              </FieldLabel>
            </Field>
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
                  : group
                    ? "Save changes"
                    : "Create group"}
              </Button>
            </DialogFooter>
          </FieldGroup>
        </form>
      </DialogContent>
    </Dialog>
  )
}
