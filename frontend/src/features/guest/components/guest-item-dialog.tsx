import { useEffect, useState } from "react"
import { Minus, Plus } from "lucide-react"
import { toast } from "sonner"

import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Label } from "@/components/ui/label"
import { Separator } from "@/components/ui/separator"
import { Spinner } from "@/components/ui/spinner"
import { Textarea } from "@/components/ui/textarea"
import { choiceRule } from "@/features/modifiers/lib/choice-rule"
import { resolveItemImage } from "@/features/menu/lib/food-image"
import { formatPrice } from "@/lib/format"
import { cn } from "@/lib/utils"
import { useGuestMenuItemDetail } from "../hooks/use-guest-menu"
import { useAddCartLine, useUpdateCartLine } from "../hooks/use-guest-cart"
import { useGuestSession } from "../hooks/use-guest-session"
import { useLanguage } from "@/lib/language-context"
import {
  BUILD_MINIMUM,
  buildMinimumProgress,
} from "@/features/modifiers/lib/build-minimum"
import type { CartLine, MenuItem, ModifierOption } from "../types"

interface Selection {
  option: ModifierOption
  quantity: number
}

interface GuestItemDialogProps {
  item: MenuItem
  onOpenChange: (open: boolean) => void
  /** Present when reopening an existing cart line: the dialog prefills from it
   *  and saves back to that line instead of adding a second one. */
  editLine?: CartLine
}

export function GuestItemDialog({ item, onOpenChange, editLine }: GuestItemDialogProps) {
  const { language, t } = useLanguage()
  const isKhmer = language === "km"
  const title = isKhmer ? (item.nameKm || item.nameEn) : item.nameEn
  const subtitleName = isKhmer ? item.nameEn : item.nameKm
  const description = isKhmer
    ? (item.descriptionKm || item.descriptionEn)
    : (item.descriptionEn || item.descriptionKm)

  const { data, isPending, isError } = useGuestMenuItemDetail(item.id)
  const addLine = useAddCartLine()
  const updateLine = useUpdateCartLine()
  const session = useGuestSession()
  const spent = session.status === "spent"

  const isDrink = /drink|beverage|tea|water|cola|coffee|juice|soda/i.test(
    `${data?.item?.categoryNameEn ?? item.categoryNameEn ?? ""} ${item.nameEn}`
  )
  const allowNotes = (data?.item?.categoryAllowNotes ?? item.categoryAllowNotes) !== false

  const [quantity, setQuantity] = useState(editLine?.quantity ?? 1)
  const [remark, setRemark] = useState(editLine?.remark ?? "")
  const [selected, setSelected] = useState<Record<string, Selection>>({})

  /* A cart line stores option ids; the full options only arrive with the item
     detail, so the prefill waits for that fetch. Runs once — after it, the
     guest's edits own the state. */
  const [prefilled, setPrefilled] = useState(!editLine)
  useEffect(() => {
    if (prefilled || !data) return
    const byId = new Map<string, ModifierOption>()
    for (const attached of data.modifierGroups ?? []) {
      for (const option of attached.group?.options ?? []) {
        if (option.id) byId.set(option.id, option)
      }
    }
    const next: Record<string, Selection> = {}
    for (const selection of editLine?.selections ?? []) {
      const option = selection.modifierOptionId
        ? byId.get(selection.modifierOptionId)
        : undefined
      // An option pulled from the menu since it was added just drops out.
      if (option?.id) next[option.id] = { option, quantity: selection.quantity ?? 1 }
    }
    setSelected(next)
    setPrefilled(true)
  }, [data, editLine, prefilled])

  const groups = (data?.modifierGroups ?? [])
    .filter((attached) => attached.group?.active !== false)
    .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))

  function groupOptions(groupIndex: number): ModifierOption[] {
    return (groups[groupIndex].group?.options ?? []).filter(
      (option) => option.available !== false
    )
  }

  /** Portions chosen in one group. maxChoice caps this, not the number of
   *  options ticked, so 6 beef + 4 chicken fills a 10-portion Meat group.
   *  Each group is counted on its own, so a full Meat never blocks Veggie. */
  function portionCount(groupIndex: number) {
    return groupOptions(groupIndex).reduce(
      (sum, o) => sum + (selected[o.id!]?.quantity ?? 0),
      0
    )
  }

  function toggle(groupIndex: number, option: ModifierOption) {
    setSelected((prev) => {
      const next = { ...prev }
      if (option.id! in next) {
        delete next[option.id!]
        return next
      }
      for (const o of groupOptions(groupIndex)) delete next[o.id!]
      next[option.id!] = { option, quantity: 1 }
      return next
    })
  }

  function changeOptionQuantity(option: ModifierOption, delta: number) {
    setSelected((prev) => {
      const next = { ...prev }
      const current = next[option.id!]?.quantity ?? 0
      const updated = current + delta
      if (updated <= 0) {
        delete next[option.id!]
      } else {
        next[option.id!] = { option, quantity: updated }
      }
      return next
    })
  }

  const violations = groups.filter((attached, index) => {
    // Portions, to match what the server validates.
    const count = portionCount(index)
    const min = attached.group?.minChoice ?? 0
    const max = attached.group?.maxChoice
    return count < min || (max != null && count > max)
  })

  // A build has to carry at least BUILD_MINIMUM of meat/veg/noodles before it
  // can be ordered; flavour alone is free.
  const minimum = buildMinimumProgress(groups, selected)
  const belowMinimum = minimum.applies && minimum.shortfall > 0
  const isDiy = minimum.applies || /diy|malatang/i.test(item.nameEn ?? "") || /diy|malatang/i.test(item.categoryNameEn ?? "")

  const selections = Object.values(selected)
  const unitPrice =
    (item.price ?? 0) +
    selections.reduce((sum, s) => sum + (s.option.unitPrice ?? 0) * s.quantity, 0)

  const payloadSelections = () =>
    selections.map((s) => ({ modifierOptionId: s.option.id!, quantity: s.quantity }))

  function handleAdd() {
    addLine.mutate(
      {
        menuItemId: item.id!,
        quantity,
        remark: remark.trim() || undefined,
        selections: payloadSelections(),
      },
      { onSuccess: () => onOpenChange(false) }
    )
  }

  function handleSave() {
    updateLine.mutate(
      {
        lineId: editLine!.id!,
        body: {
          quantity,
          remark: remark.trim() || undefined,
          selections: payloadSelections(),
        },
      },
      {
        // The hook stays quiet because the +/- buttons share it; a full edit
        // closes the dialog, so it needs its own confirmation.
        onSuccess: () => {
          toast.success("Cart updated")
          onOpenChange(false)
        },
      }
    )
  }

  const saving = editLine ? updateLine.isPending : addLine.isPending

  const heroImage = resolveItemImage(item.nameEn, item.imageUrl, "hero")

  return (
    <Dialog open onOpenChange={onOpenChange}>
      <DialogContent
        className={cn(
          "flex flex-col overflow-hidden p-0",
          isDiy
            ? "max-h-[92svh] min-h-[55svh] max-w-full top-auto bottom-0 translate-y-0 rounded-b-none data-[state=open]:zoom-in-100 data-[state=closed]:zoom-out-100 data-[state=open]:slide-in-from-bottom-full data-[state=closed]:slide-out-to-bottom-full sm:data-[state=open]:zoom-in-95 sm:data-[state=closed]:zoom-out-95 sm:data-[state=open]:slide-in-from-bottom-0 sm:data-[state=closed]:slide-out-to-bottom-0 sm:top-1/2 sm:-translate-y-1/2 sm:rounded-xl sm:max-w-2xl sm:h-[90svh]"
            : "max-h-[85svh] sm:max-h-[90svh] sm:max-w-lg"
        )}
      >
        {heroImage && !isDiy && (
          <div className="relative aspect-[4/3] w-full shrink-0 overflow-hidden bg-muted">
            <img src={heroImage} alt={item.nameEn} className="size-full object-cover" />
          </div>
        )}
        {!isDiy && (
          <div className="p-6 pt-4 pb-0">
            <DialogHeader>
              <DialogTitle>{title}</DialogTitle>
              <DialogDescription>
                {formatPrice(item.price, item.currencyCode)}
                {subtitleName ? ` · ${subtitleName}` : ""}
              </DialogDescription>
              {description && (
                <p className="text-muted-foreground mt-1 text-xs">
                  {description}
                </p>
              )}
            </DialogHeader>
          </div>
        )}

        <div className="flex-1 space-y-4 overflow-y-auto px-6 py-2">
          {isDiy && (
            <>
              {heroImage && (
                <div className="relative aspect-[4/3] -mx-6 -mt-2 mb-4 shrink-0 overflow-hidden bg-muted">
                  <img src={heroImage} alt={item.nameEn} className="size-full object-cover" />
                </div>
              )}
              <DialogHeader className="mb-4">
                <DialogTitle>{title}</DialogTitle>
                <DialogDescription>
                  {formatPrice(item.price, item.currencyCode)}
                  {subtitleName ? ` · ${subtitleName}` : ""}
                </DialogDescription>
                {description && (
                  <p className="text-muted-foreground mt-1 text-xs">
                    {description}
                  </p>
                )}
              </DialogHeader>
            </>
          )}

          {isPending ? (
            <div className="flex flex-col items-center justify-center min-h-[200px] text-muted-foreground gap-3">
              <Spinner className="size-6" />
              <span className="text-sm animate-pulse">Loading options...</span>
            </div>
          ) : isError ? (
            <p className="text-destructive text-sm">
              Couldn&apos;t load this item&apos;s options. Close and try again.
            </p>
          ) : (
            groups.map((attached, index) => {
              const group = attached.group
              if (!group) return null
              const single = group.maxChoice === 1
              const atMax =
                !single &&
                group.maxChoice != null &&
                portionCount(index) >= group.maxChoice
              return (
                <div key={group.id} className="space-y-2">
                  <div className="flex items-baseline justify-between gap-2">
                    <p className="text-sm font-medium">
                      {isKhmer ? (group.nameKm || group.nameEn) : group.nameEn}
                    </p>
                    <p
                      className={cn(
                        "text-muted-foreground text-xs",
                        atMax && "text-foreground font-medium"
                      )}
                    >
                      {group.maxChoice != null && !single
                        ? `${portionCount(index)}/${group.maxChoice}${atMax ? " · full" : ""}`
                        : choiceRule(group.minChoice, group.maxChoice)}
                    </p>
                  </div>
                  <div className="grid gap-1.5">
                    {groupOptions(index).map((option) => {
                      const qty = selected[option.id!]?.quantity ?? 0
                      const price = option.unitPrice ?? 0
                      const optionImg = resolveItemImage(option.nameEn, option.imageUrl, "thumb")
                      const optionDisplayName = isKhmer
                        ? (option.nameKm || option.nameEn)
                        : option.nameEn
                      if (single) {
                        return (
                          <Label
                            key={option.id}
                            className={cn(
                              "hover:bg-muted/50 flex items-center gap-2.5 rounded-md border p-2 font-normal cursor-pointer",
                              qty > 0 && "border-primary bg-primary/5"
                            )}
                          >
                            <Checkbox
                              checked={qty > 0}
                              onCheckedChange={() => toggle(index, option)}
                            />
                            {optionImg && (
                              <img
                                src={optionImg}
                                alt={option.nameEn}
                                className="size-8 shrink-0 rounded object-cover"
                              />
                            )}
                            <span className="flex-1 text-sm">{optionDisplayName}</span>
                            {price > 0 && (
                              <span className="text-muted-foreground text-xs tabular-nums">
                                +{formatPrice(price)}
                              </span>
                            )}
                          </Label>
                        )
                      }
                      return (
                        <div
                          key={option.id}
                          className={cn(
                            "flex items-center gap-2.5 rounded-md border p-2",
                            qty > 0 && "border-primary bg-primary/5"
                          )}
                        >
                          {optionImg && (
                            <img
                              src={optionImg}
                              alt={option.nameEn}
                              className="size-8 shrink-0 rounded object-cover"
                            />
                          )}
                          <span className="flex-1 text-sm">{optionDisplayName}</span>
                          <div className="flex items-center gap-1">
                            <Button
                              size="icon-xs"
                              variant="outline"
                              disabled={qty === 0}
                              onClick={() => changeOptionQuantity(option, -1)}
                            >
                              <Minus />
                              <span className="sr-only">Fewer {option.nameEn}</span>
                            </Button>
                            <span className="w-6 text-center text-sm font-medium tabular-nums">
                              {qty}
                            </span>
                            <Button
                              size="icon-xs"
                              variant="outline"
                              disabled={atMax}
                              onClick={() => changeOptionQuantity(option, 1)}
                            >
                              <Plus />
                              <span className="sr-only">More {option.nameEn}</span>
                            </Button>
                          </div>
                          {price > 0 && (
                            <span className="text-muted-foreground w-14 text-right text-xs tabular-nums">
                              +{formatPrice(price)}
                            </span>
                          )}
                        </div>
                      )
                    })}
                  </div>
                </div>
              )
            })
          )}

          {allowNotes && (
            <div className="space-y-2">
              <Label htmlFor="guest-order-remark" className="text-sm font-medium">
                {t("specialInstructions")}
              </Label>
              <Textarea
                id="guest-order-remark"
                value={remark}
                maxLength={200}
                rows={2}
                placeholder={isDrink ? t("drinkNotePlaceholder") : t("notePlaceholder")}
                onChange={(event) => setRemark(event.target.value)}
              />
            </div>
          )}
        </div>

        <Separator />
        <DialogFooter className="m-0 flex-row items-center p-4 sm:p-5 sm:justify-between">
          <div className="flex items-center gap-1">
            <Button
              size="icon-sm"
              variant="outline"
              disabled={quantity <= 1}
              onClick={() => setQuantity((q) => q - 1)}
            >
              <Minus />
              <span className="sr-only">Decrease quantity</span>
            </Button>
            <span className="w-8 text-center text-sm font-medium tabular-nums">
              {quantity}
            </span>
            <Button
              size="icon-sm"
              variant="outline"
              onClick={() => setQuantity((q) => q + 1)}
            >
              <Plus />
              <span className="sr-only">Increase quantity</span>
            </Button>
          </div>
          <Button
            disabled={
              isPending ||
              isError ||
              spent ||
              violations.length > 0 ||
              belowMinimum ||
              saving
            }
            onClick={editLine ? handleSave : handleAdd}
          >
            {saving ? (
              <Spinner />
            ) : belowMinimum ? (
              `${editLine ? t("update") : t("add")} · ${formatPrice(unitPrice * quantity, item.currencyCode)} (Min ${formatPrice(BUILD_MINIMUM, item.currencyCode)})`
            ) : (
              `${editLine ? t("update") : t("add")} · ${formatPrice(unitPrice * quantity, item.currencyCode)}`
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

