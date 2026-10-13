import { useState } from "react"
import { Link } from "react-router"
import { ShoppingCart, ArrowRight } from "lucide-react"

import { Button } from "@/components/ui/button"
import { GuestItemDialog } from "@/features/guest/components/guest-item-dialog"
import { GuestMenuBrowser } from "@/features/guest/components/guest-menu-browser"
import { useGuestCart } from "@/features/guest/hooks/use-guest-cart"
import { useLanguage } from "@/lib/language-context"
import { formatPrice } from "@/lib/format"
import type { MenuItem } from "@/features/guest/types"

export function GuestMenuPage() {
  const [picked, setPicked] = useState<MenuItem | null>(null)
  const { t } = useLanguage()
  const { data: cart } = useGuestCart()

  const cartItemCount = cart?.lines?.reduce((acc, line) => acc + (line.quantity || 1), 0) || 0
  const cartTotal = cart?.grandTotal ?? 0
  const currencyCode = cart?.currencyCode ?? "USD"

  return (
    <>
      <div className="flex items-center justify-between">
        <h1 className="text-lg font-semibold">{t("menu")}</h1>
        <Button variant="outline" size="sm" asChild>
          <Link to="/guest/cart" className="flex items-center gap-2">
            <ShoppingCart className="size-4" />
            {t("cart")}
          </Link>
        </Button>
      </div>

      <GuestMenuBrowser onPick={setPicked} />

      {picked && (
        <GuestItemDialog
          key={picked.id}
          item={picked}
          onOpenChange={(open) => !open && setPicked(null)}
        />
      )}

      {cartItemCount > 0 && (
        <div className="fixed inset-x-0 bottom-4 z-40 mx-4 flex justify-center pb-safe">
          <Button
            asChild
            size="lg"
            className="w-full max-w-sm h-14 rounded-full shadow-lg text-base font-semibold"
          >
            <Link to="/guest/cart" className="flex items-center justify-between px-6">
              <span className="flex items-center gap-2">
                <ShoppingCart className="size-5" />
                <span>
                  {cartItemCount} {cartItemCount === 1 ? t("item") : t("items", { defaultValue: "items" })} · {formatPrice(cartTotal, currencyCode)}
                </span>
              </span>
              <span className="flex items-center gap-2">
                Review & Send Order <ArrowRight className="size-5" />
              </span>
            </Link>
          </Button>
        </div>
      )}
    </>
  )
}
