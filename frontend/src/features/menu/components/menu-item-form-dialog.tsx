import { useEffect, useState } from "react"
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Switch } from "@/components/ui/switch"
import { Textarea } from "@/components/ui/textarea"
import { assetUrl } from "@/lib/api/client"
import {
  useCategoryOptions,
  useCreateMenuItem,
  useCurrencies,
  useUpdateMenuItem,
} from "../hooks/use-menu-items"
import { menuItemSchema, type MenuItemValues } from "../schemas/menu-item"
import type { MenuItem } from "../types"

interface MenuItemFormDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** When set, the dialog edits this item; otherwise it creates one. */
  item?: MenuItem
}

function toValues(item?: MenuItem): MenuItemValues {
  return {
    nameEn: item?.nameEn ?? "",
    nameKm: item?.nameKm ?? "",
    descriptionEn: item?.descriptionEn ?? "",
    descriptionKm: item?.descriptionKm ?? "",
    price: item?.price ?? 0,
    currencyCode: item?.currencyCode ?? "USD",
    categoryId: item?.categoryId ?? "",
    station: item?.station ?? "KITCHEN",
    available: item?.available ?? true,
  }
}

export function MenuItemFormDialog({
  open,
  onOpenChange,
  item,
}: MenuItemFormDialogProps) {
  const form = useForm<MenuItemValues>({
    resolver: zodResolver(menuItemSchema),
    defaultValues: toValues(item),
  })
  const [imageFile, setImageFile] = useState<File | undefined>(undefined)
  const categories = useCategoryOptions()
  const currencies = useCurrencies()
  const create = useCreateMenuItem()
  const update = useUpdateMenuItem()
  const isPending = create.isPending || update.isPending

  useEffect(() => {
    if (open) {
      form.reset(toValues(item))
      setImageFile(undefined)
    }
  }, [open, item, form])

  function onSubmit(values: MenuItemValues) {
    const done = { onSuccess: () => onOpenChange(false) }
    if (item?.id) {
      update.mutate({ id: item.id, body: { ...values, imageUrl: values.imageUrl ?? undefined }, image: imageFile }, done)
    } else {
      create.mutate({ body: { ...values, imageUrl: values.imageUrl ?? undefined }, image: imageFile }, done)
    }
  }

  const { errors } = form.formState
  const currentImage = assetUrl(item?.imageUrl)

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90svh] overflow-y-auto sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{item ? "Edit menu item" : "New menu item"}</DialogTitle>
          <DialogDescription>
            {item
              ? `Update "${item.nameEn}".`
              : "Add a dish or drink customers can order."}
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={form.handleSubmit(onSubmit)} noValidate>
          <FieldGroup>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field data-invalid={!!errors.nameEn}>
                <FieldLabel htmlFor="nameEn">Name (English)</FieldLabel>
                <Input
                  id="nameEn"
                  placeholder="Iced Latte"
                  aria-invalid={!!errors.nameEn}
                  {...form.register("nameEn")}
                />
                <FieldError errors={[errors.nameEn]} />
              </Field>
              <Field data-invalid={!!errors.nameKm}>
                <FieldLabel htmlFor="nameKm">Name (Khmer)</FieldLabel>
                <Input
                  id="nameKm"
                  placeholder="ឡាតេទឹកកក"
                  aria-invalid={!!errors.nameKm}
                  {...form.register("nameKm")}
                />
                <FieldError errors={[errors.nameKm]} />
              </Field>
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field data-invalid={!!errors.categoryId}>
                <FieldLabel>Category</FieldLabel>
                <Select
                  value={form.watch("categoryId")}
                  onValueChange={(value) =>
                    form.setValue("categoryId", value, { shouldValidate: true })
                  }
                >
                  <SelectTrigger
                    className="w-full"
                    aria-invalid={!!errors.categoryId}
                  >
                    <SelectValue placeholder="Choose a category" />
                  </SelectTrigger>
                  <SelectContent>
                    {(categories.data ?? []).map((category) => (
                      <SelectItem key={category.id} value={category.id!}>
                        {category.nameEn}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <FieldError errors={[errors.categoryId]} />
              </Field>

              <Field data-invalid={!!errors.station}>
                <FieldLabel>Station</FieldLabel>
                <Select
                  value={form.watch("station")}
                  onValueChange={(value) =>
                    form.setValue("station", value as "KITCHEN" | "COUNTER", { shouldValidate: true })
                  }
                >
                  <SelectTrigger
                    className="w-full"
                    aria-invalid={!!errors.station}
                  >
                    <SelectValue placeholder="Select a station" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="KITCHEN">Kitchen</SelectItem>
                    <SelectItem value="COUNTER">Counter</SelectItem>
                  </SelectContent>
                </Select>
                <FieldError errors={[errors.station]} />
              </Field>
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field data-invalid={!!errors.price}>
                <FieldLabel htmlFor="price">Price</FieldLabel>
                <Input
                  id="price"
                  type="number"
                  min={0}
                  step="0.01"
                  aria-invalid={!!errors.price}
                  {...form.register("price", { valueAsNumber: true })}
                />
                <FieldError errors={[errors.price]} />
              </Field>
              <Field data-invalid={!!errors.currencyCode}>
                <FieldLabel>Currency</FieldLabel>
                <Select
                  value={form.watch("currencyCode")}
                  onValueChange={(value) =>
                    form.setValue("currencyCode", value, {
                      shouldValidate: true,
                    })
                  }
                >
                  <SelectTrigger className="w-full">
                    <SelectValue placeholder="Currency" />
                  </SelectTrigger>
                  <SelectContent>
                    {(currencies.data ?? []).map((currency) => (
                      <SelectItem key={currency.code} value={currency.code!}>
                        {currency.code} — {currency.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <FieldError errors={[errors.currencyCode]} />
              </Field>
            </div>
            <Field>
              <FieldLabel htmlFor="descriptionEn">
                Description (English)
              </FieldLabel>
              <Textarea
                id="descriptionEn"
                placeholder="Optional"
                rows={2}
                {...form.register("descriptionEn")}
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="descriptionKm">
                Description (Khmer)
              </FieldLabel>
              <Textarea
                id="descriptionKm"
                placeholder="Optional"
                rows={2}
                {...form.register("descriptionKm")}
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="image">Image</FieldLabel>
              <div className="flex items-center gap-4">
                {currentImage && !imageFile && (
                  <img
                    src={currentImage}
                    alt={item?.nameEn}
                    className="bg-muted h-24 w-32 rounded-md object-cover"
                  />
                )}
                {imageFile && (
                  <div className="bg-muted h-24 w-32 rounded-md flex flex-col items-center justify-center text-xs text-muted-foreground">
                    <span className="font-semibold text-foreground text-center line-clamp-2 px-2">{imageFile.name}</span>
                    <span>Ready to upload</span>
                  </div>
                )}
                <div className="flex-1">
                  <Input
                    id="image"
                    type="file"
                    accept="image/*"
                    className={currentImage || imageFile ? "hidden" : ""}
                    onChange={(event) =>
                      setImageFile(event.target.files?.[0] ?? undefined)
                    }
                  />
                  {(currentImage || imageFile) && (
                    <Button type="button" variant="outline" size="sm" asChild>
                      <label htmlFor="image" className="cursor-pointer">Change Image</label>
                    </Button>
                  )}
                </div>
              </div>
            </Field>
            <Field orientation="horizontal">
              <Switch
                id="available"
                checked={form.watch("available")}
                onCheckedChange={(checked) =>
                  form.setValue("available", checked)
                }
              />
              <FieldLabel htmlFor="available">Available for ordering</FieldLabel>
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
                  : item
                    ? "Save changes"
                    : "Create item"}
              </Button>
            </DialogFooter>
          </FieldGroup>
        </form>
      </DialogContent>
    </Dialog>
  )
}
