import { useEffect, useState } from "react"
import { ChevronLeft, ChevronRight, ImageOff, UtensilsCrossed, X } from "lucide-react"

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Empty,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from "@/components/ui/empty"
import { Skeleton } from "@/components/ui/skeleton"
import { resolveItemImage } from "@/features/menu/lib/food-image"
import { formatPrice } from "@/lib/format"
import { useLanguage } from "@/lib/language-context"
import { cn } from "@/lib/utils"
import { useGuestCategories, useGuestMenuItems } from "../hooks/use-guest-menu"
import type { MenuItem } from "../types"
import { useActivePromo } from "@/features/promo/hooks"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { assetUrl } from "@/lib/api/client"

interface GuestMenuBrowserProps {
  onPick: (item: MenuItem) => void
}

function PromoPopup() {
  const { data: promo } = useActivePromo()
  const [showPromo, setShowPromo] = useState(false)

  useEffect(() => {
    if (promo?.active && promo.id) {
      try {
        const sessionKey = `promo_dismissed_${promo.id}`
        if (!sessionStorage.getItem(sessionKey)) {
          setShowPromo(true)
        }
      } catch {
        setShowPromo(true)
      }
    }
  }, [promo?.id, promo?.active])

  if (!promo || !promo.active || !showPromo) return null

  const handleDismiss = () => {
    if (promo?.id) {
      try {
        sessionStorage.setItem(`promo_dismissed_${promo.id}`, "true")
      } catch {
        // ignore storage error
      }
    }
    setShowPromo(false)
  }

  return (
    <Dialog
      open={showPromo}
      onOpenChange={(open) => {
        if (!open) {
          handleDismiss()
        } else {
          setShowPromo(true)
        }
      }}
    >
      <DialogContent showCloseButton={false} className="max-w-sm border-0 p-0 overflow-hidden bg-transparent shadow-none">
        <div className="relative overflow-hidden rounded-3xl bg-background shadow-2xl ring-1 ring-black/5">
          <DialogHeader className="sr-only">
            <DialogTitle>{promo.title}</DialogTitle>
            <DialogDescription>{promo.description}</DialogDescription>
          </DialogHeader>

          {promo.imageUrl && (
            <div className="relative aspect-[2/3] w-full">
              <img
                src={assetUrl(resolveItemImage(undefined, promo.imageUrl, "hero") || promo.imageUrl) || ""}
                className="absolute inset-0 h-full w-full object-cover"
                alt={promo.title || "Promotion"}
              />
              <div className="pointer-events-none absolute inset-x-0 bottom-0 h-20 bg-gradient-to-t from-background/40 to-transparent" />
            </div>
          )}

          <button
            type="button"
            onClick={handleDismiss}
            className="absolute right-3 top-3 z-10 rounded-full bg-black/40 p-2 text-white shadow-md backdrop-blur-md transition-colors hover:bg-black/60 active:scale-95"
          >
            <X className="size-5" />
            <span className="sr-only">Close</span>
          </button>
        </div>
      </DialogContent>
    </Dialog>
  )
}

export function GuestMenuBrowser({ onPick }: GuestMenuBrowserProps) {
  const [categoryId, setCategoryId] = useState<string | undefined>(undefined)
  const [page, setPage] = useState(0)

  const categories = useGuestCategories()
  const { data, isPending, isError, error, refetch } = useGuestMenuItems({
    page,
    categoryId,
  })
  const { language, t } = useLanguage()
  const isKhmer = language === "km"

  const items = data?.content ?? []
  const totalPages = data?.totalPages ?? 0
  const totalElements = data?.totalElements ?? 0

  useEffect(() => {
    if (data && page > 0 && page >= totalPages) {
      setPage(Math.max(0, totalPages - 1))
    }
  }, [data, page, totalPages])

  function pickCategory(id: string | undefined) {
    setCategoryId(id)
    setPage(0)
  }

  return (
    <div className="flex min-w-0 flex-1 flex-col gap-3">
      <PromoPopup />
      <div className="flex gap-2 overflow-x-auto pb-1">
        <Badge
          asChild
          variant={categoryId === undefined ? "default" : "outline"}
          className="h-8 px-4 text-sm"
        >
          <button type="button" onClick={() => pickCategory(undefined)}>
            {t("all")}
          </button>
        </Badge>
        {categories.data?.map((category) => (
          <Badge
            key={category.id}
            asChild
            variant={categoryId === category.id ? "default" : "outline"}
            className="h-8 px-4 text-sm"
          >
            <button type="button" onClick={() => pickCategory(category.id)}>
              {isKhmer ? (category.nameKm || category.nameEn) : category.nameEn}
            </button>
          </Badge>
        ))}
      </div>

      <div className="text-muted-foreground text-right text-xs">
        {t("pricesBeforeVat")}
      </div>

      {isPending || (totalElements > 0 && items.length === 0) ? (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
          {Array.from({ length: 6 }, (_, i) => (
            <Skeleton key={i} className="aspect-[4/3] rounded-xl" />
          ))}
        </div>
      ) : isError ? (
        <Alert variant="destructive">
          <AlertTitle>Couldn&apos;t load the menu</AlertTitle>
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
      ) : totalElements === 0 ? (
        <Empty className="border border-dashed">
          <EmptyHeader>
            <EmptyMedia variant="icon">
              <UtensilsCrossed />
            </EmptyMedia>
            <EmptyTitle>{t("nothingAvailable")}</EmptyTitle>
            <EmptyDescription>
              {t("noAvailableItems")}
            </EmptyDescription>
          </EmptyHeader>
        </Empty>
      ) : (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
          {items.map((item) => {
            const image = resolveItemImage(item.nameEn, item.imageUrl, "card")
            const primaryName = isKhmer ? (item.nameKm || item.nameEn) : item.nameEn
            const secondaryName = isKhmer ? item.nameEn : item.nameKm
            return (
              <button
                key={item.id}
                type="button"
                onClick={() => onPick(item)}
                disabled={!item.available}
                className={cn(
                  "bg-card hover:border-primary/50 flex flex-col overflow-hidden rounded-xl border text-left relative",
                  "transition-colors",
                  !item.available && "opacity-50 cursor-not-allowed"
                )}
              >
                <div className="bg-muted relative aspect-[4/3] w-full">
                  {image ? (
                    <img
                      src={image}
                      alt={item.nameEn}
                      className="size-full object-cover"
                      loading="lazy"
                    />
                  ) : (
                    <div className="text-muted-foreground/50 flex size-full items-center justify-center">
                      <ImageOff className="size-6" />
                    </div>
                  )}
                  {!item.available && (
                    <div className="absolute inset-0 flex items-center justify-center bg-background/20 backdrop-blur-[1px]">
                      <Badge variant="destructive" className="font-semibold pointer-events-none">{t("soldOut")}</Badge>
                    </div>
                  )}
                </div>
                <div className="flex flex-1 flex-col gap-0.5 p-2">
                  <span className="line-clamp-2 text-sm font-medium">
                    {primaryName}
                  </span>
                  {secondaryName && (
                    <span className="text-muted-foreground line-clamp-1 text-xs">
                      {secondaryName}
                    </span>
                  )}
                  <span className="text-muted-foreground mt-auto text-sm tabular-nums">
                    {formatPrice(item.price, item.currencyCode)}
                  </span>
                </div>
              </button>
            )
          })}
        </div>
      )}

      {totalPages > 1 && (
        <div className="flex items-center justify-end gap-2">
          <Button
            variant="outline"
            size="sm"
            disabled={page === 0}
            onClick={() => setPage((p) => p - 1)}
          >
            <ChevronLeft />
            {t("previous")}
          </Button>
          <span className="text-muted-foreground text-sm tabular-nums">
            {page + 1} / {totalPages}
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={page + 1 >= totalPages}
            onClick={() => setPage((p) => p + 1)}
          >
            {t("next")}
            <ChevronRight />
          </Button>
        </div>
      )}
    </div>
  )
}
